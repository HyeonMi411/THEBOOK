from django.db import models
from django.conf import settings


class Book(models.Model):
    """project_3의 BOOK + BOOK_STOCK을 하나로 합침 (재고를 별도 테이블로 뺐던 이유는
    JPA/MyBatis 하이브리드 설계 때문이었는데, Django는 단일 ORM이라 합쳐도 무방)."""

    title = models.CharField(max_length=255)
    author = models.CharField(max_length=255)
    publisher = models.CharField(max_length=255)
    publish_date = models.DateField()
    category = models.CharField(max_length=100)
    description = models.TextField(blank=True, null=True)
    pages = models.IntegerField(null=True, blank=True)
    price = models.IntegerField(default=0)
    rating = models.FloatField(default=0)
    review_count = models.IntegerField(default=0)
    cover = models.ImageField(upload_to="uploads/books/", null=True, blank=True)
    stock_quantity = models.IntegerField(default=0)

    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.SET_NULL, null=True, blank=True
    )
    # 소프트 삭제 - project_3에서 하드 삭제 시 주문이력 FK 충돌(ORA-02292)을 겪었던 것을
    # 처음부터 반영해서, Book도 삭제 대신 deleted 플래그만 사용.
    deleted = models.BooleanField(default=False)
    reg_date = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-reg_date"]

    def __str__(self):
        return self.title


class BookLike(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    book = models.ForeignKey(Book, on_delete=models.CASCADE)

    class Meta:
        unique_together = ("user", "book")
