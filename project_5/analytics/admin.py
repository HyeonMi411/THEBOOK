from django.contrib import admin
from .models import ServiceLog

@admin.register(ServiceLog)
class ServiceLogAdmin(admin.ModelAdmin):
    list_display = ("date", "category", "visitor_count", "sales_amount")
