"""
project_4 운영 대시보드.

- 판매 / 커뮤니티 / 상담 챗봇 : Spring Boot(project_4/back) 소유 Oracle 을 "읽기 전용"으로 직접 조회해 pandas 로 집계
- 운영 지표 추이           : Spring Boot 가 매일 23:50 에 POST 로 밀어준 값(opslog.ServiceLog, Django 자체 DB)
- "지금 동기화" 버튼        : Django → Spring Boot POST /api/statistics/sync (flutter 수업의 동기화 흐름)
"""
import json
import logging
from datetime import timedelta

import requests
from django.conf import settings
from django.contrib import messages
from django.contrib.auth.decorators import login_required
from django.db import connections
from django.db.utils import DatabaseError
from django.http import JsonResponse
from django.shortcuts import redirect, render
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_POST

from opslog.models import ServiceLog

from . import analysis
from .models import ChatbotLog, Comment, Follow, OrderItem, Post, PostLike

log = logging.getLogger(__name__)

# ChatbotService 의 FAQ id → 화면 표시용 이름 (백엔드 FAQ 가 바뀌면 여기도 맞춰 주세요)
FAQ_LABELS = {
    "acc-signup": "회원가입", "acc-login": "로그인 유지", "acc-social": "소셜 로그인", "acc-mypage": "닉네임·탈퇴",
    "shop-browse": "도서 보기", "shop-external": "외부 도서검색", "shop-ocr": "표지 사진 검색", "shop-cart": "장바구니",
    "shop-address": "배송지·우편번호", "shop-pay": "결제 방법", "shop-notify": "결제 알림", "shop-cancel": "취소·환불",
    "shop-orders": "주문내역", "shop-delivery": "배송", "shop-stock": "재고",
    "board-write": "글쓰기", "board-tag-like": "태그·좋아요·댓글", "board-social": "리트윗·팔로우",
    "board-edit": "수정·삭제", "board-notice": "공지사항",
    "svc-weather": "날씨", "svc-store": "매장 지도", "svc-news": "책 소식",
    "etc-about": "앱 소개", "etc-agent": "상담원 연결", "etc-hello": "인사",
}


def _sales_context():
    rows = (
        OrderItem.objects.using("oracle")
        .filter(order__order_status="PAID")
        .select_related("order", "book")
    )
    records = [{
        "date": item.order.approved_at.date() if item.order.approved_at else item.order.created_at.date(),
        "category": item.book.category,
        "quantity": item.quantity,
        "amount": item.price * item.quantity,
    } for item in rows]
    return analysis.summarize_sales(records)


def _community_context(days=14):
    since = timezone.now() - timedelta(days=days)
    posts = [{"date": timezone.localtime(p).date() if timezone.is_aware(p) else p.date()}
             for p in Post.objects.using("oracle").filter(deleted=False, created_at__gte=since).values_list("created_at", flat=True)]
    likes = [{"date": timezone.localtime(t).date() if timezone.is_aware(t) else t.date()}
             for t in PostLike.objects.using("oracle").filter(created_at__gte=since).values_list("created_at", flat=True)]
    comments = [{"date": timezone.localtime(t).date() if timezone.is_aware(t) else t.date()}
                for t in Comment.objects.using("oracle").filter(deleted=False, created_at__gte=since).values_list("created_at", flat=True)]
    # POST_HASHTAG 는 PK 가 없는 조인 테이블이라 ORM 모델 대신 SQL 로 집계
    with connections["oracle"].cursor() as cursor:
        cursor.execute(
            "SELECT h.NAME, COUNT(*) FROM POST_HASHTAG ph "
            "JOIN HASHTAGS h ON h.ID = ph.HASHTAG_ID "
            "JOIN POSTS p ON p.ID = ph.POST_ID AND p.DELETED = 0 "
            "GROUP BY h.NAME"
        )
        tag_counts = [(row[0], row[1]) for row in cursor.fetchall()]
    follow_count = Follow.objects.using("oracle").count()
    return analysis.summarize_community(posts, likes, comments, tag_counts, follow_count, days=days,
                                        today=timezone.localdate())


def _chatbot_context(days=30):
    since = timezone.now() - timedelta(days=days)
    logs = list(
        ChatbotLog.objects.using("oracle").filter(created_at__gte=since)
        .order_by("-created_at").values("question", "faq_id", "source")[:5000]
    )
    return analysis.summarize_chatbot(logs, FAQ_LABELS)


@login_required
def dashboard_view(request):
    context = {"db_error": None}
    try:
        context.update(_sales_context())
        context.update(_community_context())
        context.update(_chatbot_context())
    except DatabaseError as e:
        # Oracle 이 꺼져 있어도 대시보드 자체(운영 지표 추이)는 볼 수 있게
        log.warning("Oracle 조회 실패: %s", e)
        context["db_error"] = "Spring Boot DB(Oracle)에 연결하지 못했습니다. ORACLE_* 환경변수와 DB 상태를 확인해 주세요."
        context.update(analysis.summarize_sales([]))
        context.update(analysis.summarize_community([], [], [], [], 0, today=timezone.localdate()))
        context.update(analysis.summarize_chatbot([]))
    context.update(analysis.ops_trend(list(ServiceLog.objects.values(
        "date", "new_users", "paid_orders", "chatbot_questions"))))
    context["sync_enabled"] = bool(settings.SPRING_API_BASE_URL and settings.STATS_SYNC_TOKEN)
    return render(request, "analytics/dashboard.html", context)


@csrf_exempt
def api_receive_statistics(request):
    """Spring Boot StatisticsSyncService → 오늘 운영 지표 저장 (같은 날짜면 최신 값으로 갱신)"""
    if request.method != "POST":
        return JsonResponse({"status": "fail", "message": "POST 요청만 지원합니다."}, status=405)
    token = request.headers.get("X-Sync-Token", "")
    if not settings.STATS_SYNC_TOKEN or token != settings.STATS_SYNC_TOKEN:
        return JsonResponse({"status": "fail", "message": "동기화 토큰이 올바르지 않습니다."}, status=403)
    try:
        data = json.loads(request.body)
        ServiceLog.objects.update_or_create(
            date=data["date"],
            defaults={
                "new_users": int(data.get("newUsers", 0)),
                "new_posts": int(data.get("newPosts", 0)),
                "likes": int(data.get("likes", 0)),
                "comments": int(data.get("comments", 0)),
                "paid_orders": int(data.get("paidOrders", 0)),
                "sales": int(data.get("sales", 0)),
                "chatbot_questions": int(data.get("chatbotQuestions", 0)),
                "chatbot_unanswered": int(data.get("chatbotUnanswered", 0)),
            },
        )
        return JsonResponse({"status": "success", "message": "통계 데이터 갱신 완료"})
    except (KeyError, ValueError, TypeError) as e:
        return JsonResponse({"status": "error", "message": f"잘못된 데이터: {e}"}, status=400)


@login_required
@require_POST
def sync_now(request):
    """대시보드의 '지금 동기화' 버튼 → Spring Boot 에게 오늘 지표를 보내 달라고 요청"""
    if not (settings.SPRING_API_BASE_URL and settings.STATS_SYNC_TOKEN):
        messages.error(request, "SPRING_API_BASE_URL / STATS_SYNC_TOKEN 이 설정되지 않았습니다.")
        return redirect("dashboard")
    url = settings.SPRING_API_BASE_URL.rstrip("/") + "/api/statistics/sync"
    try:
        res = requests.post(url, headers={"X-Sync-Token": settings.STATS_SYNC_TOKEN}, timeout=5)
        if res.status_code == 200:
            messages.success(request, "Spring Boot 에서 오늘 지표를 받아왔습니다.")
        else:
            messages.error(request, f"동기화 실패 (응답 코드 {res.status_code})")
    except requests.RequestException as e:
        messages.error(request, f"Spring Boot 서버에 연결하지 못했습니다: {e}")
    return redirect("dashboard")
