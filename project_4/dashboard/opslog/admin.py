from django.contrib import admin

from .models import ServiceLog


@admin.register(ServiceLog)
class ServiceLogAdmin(admin.ModelAdmin):
    list_display = ("date", "new_users", "new_posts", "paid_orders", "sales", "chatbot_questions", "chatbot_unanswered", "synced_at")
    ordering = ("-date",)
