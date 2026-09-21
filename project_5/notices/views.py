from rest_framework.decorators import api_view, permission_classes
from rest_framework.pagination import PageNumberPagination
from rest_framework.response import Response
from books.permissions import IsAdminOrReadOnly
from rest_framework.views import APIView
from django.db.models import Q
from .models import Notice
from .serializers import NoticeSerializer


class NoticePagination(PageNumberPagination):
    page_size = 12
    page_size_query_param = "size"
    page_query_param = "page"


class NoticeListCreateView(APIView):
    permission_classes = [IsAdminOrReadOnly]

    def get(self, request):
        paginator = NoticePagination()
        page = paginator.paginate_queryset(Notice.objects.all(), request)
        return paginator.get_paginated_response(NoticeSerializer(page, many=True).data)

    def post(self, request):
        serializer = NoticeSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        serializer.save(created_by=request.user, ip_address=request.META.get("REMOTE_ADDR"))
        return Response(serializer.data, status=201)


class NoticeDetailView(APIView):
    permission_classes = [IsAdminOrReadOnly]

    def get(self, request, pk):
        notice = Notice.objects.get(pk=pk)
        notice.hit += 1  # 조회수 +1 (project_3와 동일)
        notice.save(update_fields=["hit"])
        return Response(NoticeSerializer(notice).data)

    def patch(self, request, pk):
        notice = Notice.objects.get(pk=pk)
        serializer = NoticeSerializer(notice, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data)

    def delete(self, request, pk):
        Notice.objects.filter(pk=pk).delete()
        return Response(pk)


@api_view(["GET"])
def search_notices(request):
    keyword = request.query_params.get("keyword", "")
    qs = Notice.objects.filter(Q(title__icontains=keyword))
    return Response(NoticeSerializer(qs, many=True).data)
