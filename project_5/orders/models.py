from django.conf import settings
from django.db import models
from books.models import Book


class Orders(models.Model):
    STATUS_CHOICES = (
        ("PENDING", "결제전"),
        ("PAID", "결제완료"),
        ("CANCELLED", "취소"),
        ("FAILED", "실패"),
    )

    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    total_amount = models.IntegerField(default=0)
    order_status = models.CharField(max_length=20, choices=STATUS_CHOICES, default="PENDING")
    tid = models.CharField(max_length=100, null=True, blank=True)  # 카카오페이 결제 고유번호
    hidden = models.BooleanField(default=False)  # 취소/실패 주문의 "숨기기" 처리 (project_3와 동일)
    created_at = models.DateTimeField(auto_now_add=True)
    approved_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ["-created_at"]


class OrderItem(models.Model):
    order = models.ForeignKey(Orders, on_delete=models.CASCADE, related_name="items")
    book = models.ForeignKey(Book, on_delete=models.CASCADE)
    book_title_snapshot = models.CharField(max_length=255)  # 도서가 나중에 바뀌어도 주문 당시 제목 보존
    price = models.IntegerField()
    quantity = models.IntegerField()
