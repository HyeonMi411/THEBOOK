from rest_framework import serializers
from .models import Orders, OrderItem


class OrderItemSerializer(serializers.ModelSerializer):
    bookTitle = serializers.CharField(source="book_title_snapshot", read_only=True)
    bookId = serializers.IntegerField(source="book_id", read_only=True)

    class Meta:
        model = OrderItem
        fields = ["id", "bookId", "bookTitle", "price", "quantity"]


class OrderSerializer(serializers.ModelSerializer):
    items = OrderItemSerializer(many=True, read_only=True)

    class Meta:
        model = Orders
        fields = ["id", "total_amount", "order_status", "tid", "items", "created_at", "approved_at"]
