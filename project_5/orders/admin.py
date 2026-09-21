from django.contrib import admin
from .models import Orders, OrderItem


class OrderItemInline(admin.TabularInline):
    model = OrderItem
    extra = 0


@admin.register(Orders)
class OrdersAdmin(admin.ModelAdmin):
    list_display = ("id", "user", "total_amount", "order_status", "created_at")
    list_filter = ("order_status",)
    inlines = [OrderItemInline]
