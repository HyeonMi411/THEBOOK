import requests
from django.conf import settings
from django.core.cache import cache
from django.db.models import Q
from rest_framework import status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Book
from .permissions import IsAdminOrReadOnly
from .serializers import BookSerializer


class BookPagination(PageNumberPagination):
    page_size = 12
    page_size_query_param = "size"
    page_query_param = "page"


class BookListCreateView(APIView):
    permission_classes = [IsAdminOrReadOnly]

    def get(self, request):
        qs = Book.objects.filter(deleted=False)
        category = request.query_params.get("category")
        if category:
            qs = qs.filter(category=category)
        paginator = BookPagination()
        page = paginator.paginate_queryset(qs, request)
        return paginator.get_paginated_response(BookSerializer(page, many=True, context={"request": request}).data)

    def post(self, request):
        serializer = BookSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        serializer.save(created_by=request.user)
        return Response(serializer.data, status=status.HTTP_201_CREATED)


class BookDetailView(APIView):
    permission_classes = [IsAdminOrReadOnly]

    def get_object(self, pk):
        return Book.objects.get(pk=pk, deleted=False)

    def get(self, request, pk):
        book = self.get_object(pk)
        return Response(BookSerializer(book, context={"request": request}).data)

    def patch(self, request, pk):
        book = self.get_object(pk)
        serializer = BookSerializer(book, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data)

    def delete(self, request, pk):
        book = self.get_object(pk)
        # project_3에서 하드 삭제 시 FK 위반(ORA-02292)을 겪었던 교훈 - 소프트 삭제만 수행
        book.deleted = True
        book.save()
        return Response(pk)


@api_view(["PATCH"])
@permission_classes([IsAdminOrReadOnly])
def update_stock(request, pk):
    book = Book.objects.get(pk=pk, deleted=False)
    book.stock_quantity = request.data.get("stockQuantity", book.stock_quantity)
    book.save()
    return Response(BookSerializer(book).data)


@api_view(["GET"])
@permission_classes([AllowAny])
def search_books(request):
    keyword = request.query_params.get("keyword", "")
    qs = Book.objects.filter(Q(title__icontains=keyword) & Q(deleted=False))
    return Response(BookSerializer(qs, many=True).data)


@api_view(["GET"])
@permission_classes([AllowAny])
def bestsellers(request):
    """
    project_3와 동일하게 결제완료(PAID) 주문 기준 판매량 TOP10.
    Redis 대신 Django cache framework 사용(설정에 따라 Redis/로컬메모리 자동 전환).
    """
    cached = cache.get("bestsellers")
    if cached is not None:
        return Response(cached)

    from orders.models import OrderItem  # 순환참조 방지용 지연 임포트
    from django.db.models import Sum

    top = (
        OrderItem.objects.filter(order__order_status="PAID")
        .values("book_id")
        .annotate(sold=Sum("quantity"))
        .order_by("-sold")[:10]
    )
    result = []
    for rank, row in enumerate(top, start=1):
        try:
            book = Book.objects.get(pk=row["book_id"])
        except Book.DoesNotExist:
            continue
        result.append({
            "rank": rank,
            "soldQuantity": row["sold"],
            "book": BookSerializer(book).data,
        })
    cache.set("bestsellers", result, timeout=600)  # 10분 캐싱 (project_3와 동일)
    return Response(result)


@api_view(["GET"])
@permission_classes([AllowAny])
def search_national_library(request):
    """국립중앙도서관 오픈API 검색 (DB 저장은 안 하고 검색결과만 반환)."""
    keyword = request.query_params.get("keyword", "")
    page = request.query_params.get("page", 1)
    if not settings.NL_API_KEY:
        return Response({"error": "NL_API_KEY가 설정되지 않았습니다."}, status=503)

    resp = requests.get(
        "https://www.nl.go.kr/NL/search/openApi/search.do",
        params={
            "key": settings.NL_API_KEY,
            "kwd": keyword,
            "pageNum": page,
            "pageSize": 20,
            "apiType": "json",
        },
        timeout=5,
    )
    return Response(resp.json())


@api_view(["POST"])
@permission_classes([IsAdminOrReadOnly])
def kakao_book_search_insert(request):
    """카카오 도서검색 API로 검색 후 자동 등록 (관리자 전용)."""
    search = request.query_params.get("search", "")
    if not settings.KAKAO_REST_API_KEY:
        return Response({"error": "KAKAO_CLIENT_ID(REST API 키)가 설정되지 않았습니다."}, status=503)

    resp = requests.get(
        "https://dapi.kakao.com/v3/search/book",
        headers={"Authorization": f"KakaoAK {settings.KAKAO_REST_API_KEY}"},
        params={"query": search},
        timeout=5,
    )
    created = 0
    for doc in resp.json().get("documents", []):
        if Book.objects.filter(title=doc["title"], deleted=False).exists():
            continue
        Book.objects.create(
            title=doc["title"],
            author=", ".join(doc.get("authors", [])) or "미상",
            publisher=doc.get("publisher", "미상"),
            publish_date=(doc.get("datetime") or "2000-01-01T00:00:00")[:10],
            category="카카오검색",
            description=doc.get("contents", ""),
            price=doc.get("price", 0),
            created_by=request.user,
        )
        created += 1
    return Response({"created": created})
