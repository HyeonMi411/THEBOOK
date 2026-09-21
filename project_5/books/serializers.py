from rest_framework import serializers
from .models import Book


class BookSerializer(serializers.ModelSerializer):
    userNickname = serializers.CharField(source="created_by.nickname", read_only=True, default=None)

    class Meta:
        model = Book
        fields = [
            "id", "title", "author", "publisher", "publish_date", "category",
            "description", "pages", "price", "rating", "review_count",
            "cover", "stock_quantity", "reg_date", "userNickname",
        ]
        read_only_fields = ["id", "reg_date"]
