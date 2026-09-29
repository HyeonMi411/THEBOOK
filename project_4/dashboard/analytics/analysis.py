"""
대시보드 집계 로직 (pandas) - DB 와 분리된 순수 함수라서 Oracle 없이도 테스트할 수 있다 (analytics/tests.py).
views.py 는 DB 에서 레코드(dict 리스트)를 꺼내 이 함수들에 넘기기만 한다.
"""
import pandas as pd


def summarize_sales(records):
    """records: [{date, category, quantity, amount}] (결제완료 주문상품)"""
    empty = {
        "categories": [], "quantities": [], "amounts": [], "shares": [],
        "daily_dates": [], "daily_amounts": [],
        "stats_summary": {"avg_amount": 0, "max_amount": 0, "total_amount": 0, "total_orders": 0},
        "top_category": "데이터 없음",
    }
    if not records:
        return empty
    df = pd.DataFrame(records)
    summary = df.groupby("category")[["quantity", "amount"]].sum().reset_index()
    total_qty = summary["quantity"].sum()
    summary["share"] = (summary["quantity"] / total_qty * 100).round(1) if total_qty else 0
    daily = df.groupby("date")["amount"].sum().sort_index()
    return {
        "categories": summary["category"].tolist(),
        "quantities": [int(v) for v in summary["quantity"].tolist()],
        "amounts": [int(v) for v in summary["amount"].tolist()],
        "shares": [float(v) for v in summary["share"].tolist()],
        "daily_dates": [str(d) for d in daily.index.tolist()],
        "daily_amounts": [int(v) for v in daily.tolist()],
        "stats_summary": {
            "avg_amount": int(round(daily.mean(), 0)),
            "max_amount": int(daily.max()),
            "total_amount": int(df["amount"].sum()),
            "total_orders": int(df.shape[0]),
        },
        "top_category": summary.loc[summary["quantity"].idxmax(), "category"],
    }


def summarize_community(posts, likes, comments, hashtag_counts, follow_count, days=14, today=None):
    """
    posts/likes/comments: [{date}] 최근 활동 (삭제된 글·댓글 제외)
    hashtag_counts: [(태그, 사용 횟수)]
    최근 N일 일자별 활동 추이 + 인기 해시태그 TOP 10
    """
    today = pd.Timestamp(today) if today is not None else pd.Timestamp.today().normalize()
    index = pd.date_range(end=today, periods=days, freq="D").date

    def daily(rows):
        if not rows:
            return [0] * days
        s = pd.DataFrame(rows).groupby("date").size()
        return [int(s.get(d, 0)) for d in index]

    tags = sorted(hashtag_counts, key=lambda t: (-t[1], t[0]))[:10]
    return {
        "activity_dates": [d.strftime("%m-%d") for d in index],
        "activity_posts": daily(posts),
        "activity_likes": daily(likes),
        "activity_comments": daily(comments),
        "top_tags": [t[0] for t in tags],
        "top_tag_counts": [int(t[1]) for t in tags],
        "community_summary": {
            "posts": sum(daily(posts)),
            "likes": sum(daily(likes)),
            "comments": sum(daily(comments)),
            "follows": int(follow_count),
        },
    }


def summarize_chatbot(logs, faq_labels=None):
    """
    logs: [{question, faq_id, source}]
    - source 비율 (menu 버튼 / faq 검색 / ai 보조 / none 미응답)
    - 많이 찾은 FAQ TOP 5
    - 답하지 못한 질문 최근 10개 → FAQ 키워드 보강 대상
    """
    faq_labels = faq_labels or {}
    if not logs:
        return {"chatbot_sources": [], "chatbot_source_counts": [], "top_faqs": [], "unanswered": [],
                "chatbot_total": 0, "answer_rate": 0}
    df = pd.DataFrame(logs)
    source_names = {"menu": "버튼", "faq": "FAQ 검색", "ai": "AI 보조", "none": "미응답"}
    counts = df["source"].value_counts()
    matched = df[df["faq_id"].notna()]
    top = matched["faq_id"].value_counts().head(5)
    total = int(df.shape[0])
    answered = int((df["source"] != "none").sum())
    return {
        "chatbot_sources": [source_names.get(k, k) for k in counts.index.tolist()],
        "chatbot_source_counts": [int(v) for v in counts.tolist()],
        "top_faqs": [{"faq": faq_labels.get(k, k), "count": int(v)} for k, v in top.items()],
        "unanswered": df[df["source"] == "none"]["question"].head(10).tolist(),
        "chatbot_total": total,
        "answer_rate": round(answered / total * 100, 1) if total else 0,
    }


def ops_trend(rows):
    """opslog.ServiceLog (Spring Boot 가 매일 푸시한 지표) → 추이 그래프용"""
    if not rows:
        return {"ops_dates": [], "ops_users": [], "ops_orders": [], "ops_questions": []}
    df = pd.DataFrame(rows).sort_values("date").tail(30)
    return {
        "ops_dates": [str(d) for d in df["date"].tolist()],
        "ops_users": [int(v) for v in df["new_users"].tolist()],
        "ops_orders": [int(v) for v in df["paid_orders"].tolist()],
        "ops_questions": [int(v) for v in df["chatbot_questions"].tolist()],
    }
