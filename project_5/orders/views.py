import requests
from django.conf import settings
from django.db import transaction
from django.utils import timezone
from rest_framework.decorators import api_view, permission_classes
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response

from books.models import Book
from cart.models import Cart
from .models import Orders, OrderItem
from .serializers import OrderSerializer


class OrderPagination(PageNumberPagination):
    page_size = 12
    page_size_query_param = "size"
    page_query_param = "page"


@api_view(["GET", "POST"])
@permission_classes([IsAuthenticated])
def order_list_create(request):
    if request.method == "GET":
        qs = Orders.objects.filter(user=request.user, hidden=False)
        paginator = OrderPagination()
        page = paginator.paginate_queryset(qs, request)
        return paginator.get_paginated_response(OrderSerializer(page, many=True).data)

    # POST: 주문 생성 (PENDING) - cartItemIds 넘기면 장바구니 결제, bookId(+quantity) 넘기면 바로구매
    cart_item_ids = request.data.get("cartItemIds")
    book_id = request.data.get("bookId")
    quantity = request.data.get("quantity", 1)

    order = Orders.objects.create(user=request.user, total_amount=0, order_status="PENDING")
    total = 0

    if cart_item_ids:
        cart = Cart.objects.get(user=request.user)
        for item in cart.items.filter(id__in=cart_item_ids).select_related("book"):
            book = item.book
            OrderItem.objects.create(
                order=order, book=book, book_title_snapshot=book.title,
                price=book.price, quantity=item.quantity,
            )
            total += book.price * item.quantity
        cart.items.filter(id__in=cart_item_ids).delete()
    elif book_id:
        book = Book.objects.get(pk=book_id, deleted=False)
        OrderItem.objects.create(
            order=order, book=book, book_title_snapshot=book.title,
            price=book.price, quantity=quantity,
        )
        total = book.price * int(quantity)
    else:
        order.delete()
        return Response({"error": "cartItemIds 또는 bookId가 필요합니다."}, status=400)

    order.total_amount = total
    order.save()
    return Response(OrderSerializer(order).data, status=201)


@api_view(["GET", "DELETE"])
@permission_classes([IsAuthenticated])
def order_detail(request, pk):
    order = Orders.objects.get(pk=pk, user=request.user)

    if request.method == "GET":
        return Response(OrderSerializer(order).data)

    # DELETE: PENDING은 실제 삭제, 그 외(PAID/CANCELLED/FAILED)는 "숨기기"만 (회계이력 보존)
    if order.order_status == "PENDING":
        order.delete()
    else:
        order.hidden = True
        order.save()
    return Response({"message": "삭제되었습니다."})


def _kakao_headers():
    return {
        "Authorization": f"SECRET_KEY {settings.KAKAO_PAY_SECRET_KEY}",
        "Content-Type": "application/json",
    }


@api_view(["POST"])
@permission_classes([IsAuthenticated])
def kakao_ready(request):
    """결제 준비 - 이 시점에는 재고를 건드리지 않음(project_3와 동일 정책)."""
    order_id = request.data.get("orderId")
    order = Orders.objects.get(pk=order_id, user=request.user, order_status="PENDING")

    if not settings.KAKAO_PAY_SECRET_KEY:
        return Response({"error": "KAKAO_PAY_SECRET_KEY가 설정되지 않았습니다."}, status=503)

    base = settings.FRONTEND_BASE_URL  # project_3의 하드코딩 실수를 반복하지 않기 위해 반드시 환경변수 사용
    payload = {
        "cid": settings.KAKAO_PAY_CID,
        "partner_order_id": str(order.id),
        "partner_user_id": str(request.user.id),
        "item_name": order.items.first().book_title_snapshot if order.items.exists() else "도서 주문",
        "quantity": sum(i.quantity for i in order.items.all()),
        "total_amount": order.total_amount,
        "tax_free_amount": 0,
        "approval_url": f"{base}/payment/complete?orderId={order.id}",
        "cancel_url": f"{base}/payment/cancel?orderId={order.id}",
        "fail_url": f"{base}/payment/fail?orderId={order.id}",
    }
    resp = requests.post(
        "https://open-api.kakaopay.com/online/v1/payment/ready",
        headers=_kakao_headers(), json=payload, timeout=5,
    )
    if resp.status_code != 200:
        return Response({"error": f"카카오페이 결제준비 실패 - {resp.text}"}, status=400)

    data = resp.json()
    order.tid = data["tid"]
    order.save()
    return Response({
        "orderId": order.id,
        "tid": data["tid"],
        "redirectUrl": data.get("next_redirect_pc_url") or data.get("next_redirect_mobile_url"),
    })


@api_view(["POST"])
@permission_classes([IsAuthenticated])
def kakao_approve(request):
    """
    결제 승인 - 이 시점에만 실제 재고 차감.
    project_3에서 배운 동시성 제어(비관적 락)를 select_for_update()로 동일하게 구현.
    """
    order_id = request.data.get("orderId")
    pg_token = request.data.get("pgToken")

    with transaction.atomic():
        order = Orders.objects.select_for_update().get(pk=order_id, user=request.user, order_status="PENDING")

        resp = requests.post(
            "https://open-api.kakaopay.com/online/v1/payment/approve",
            headers=_kakao_headers(),
            json={
                "cid": settings.KAKAO_PAY_CID,
                "tid": order.tid,
                "partner_order_id": str(order.id),
                "partner_user_id": str(request.user.id),
                "pg_token": pg_token,
            },
            timeout=5,
        )
        if resp.status_code != 200:
            return Response({"error": f"카카오페이 결제승인 실패 - {resp.text}"}, status=400)

        # 재고 차감 (행 잠금 상태에서 처리 - 동시 결제로 인한 초과판매 방지)
        for item in order.items.select_related("book"):
            book = Book.objects.select_for_update().get(pk=item.book_id)
            if book.stock_quantity < item.quantity:
                order.order_status = "FAILED"
                order.save()
                return Response({"error": f"'{book.title}' 재고가 부족합니다."}, status=409)
            book.stock_quantity -= item.quantity
            book.save()

        order.order_status = "PAID"
        order.approved_at = timezone.now()
        order.save()

    from django.core.cache import cache
    cache.delete("bestsellers")  # 결제 완료 시 베스트셀러 캐시 즉시 무효화 (project_3와 동일)

    return Response(OrderSerializer(order).data)


@api_view(["POST"])
@permission_classes([IsAuthenticated])
def kakao_cancel(request, pk):
    order = Orders.objects.get(pk=pk, user=request.user)
    order.order_status = "CANCELLED"
    order.save()
    return Response({"message": "결제가 취소되었습니다."})


@api_view(["POST"])
@permission_classes([IsAuthenticated])
def kakao_fail(request, pk):
    order = Orders.objects.get(pk=pk, user=request.user)
    order.order_status = "FAILED"
    order.save()
    return Response({"message": "결제에 실패했습니다."})
