from django.contrib import admin
from django.contrib.auth.admin import UserAdmin
from .models import AppUser, EmailVerificationCode


@admin.register(AppUser)
class AppUserAdmin(UserAdmin):
    list_display = ("id", "email", "nickname", "role", "provider", "deleted", "date_joined")
    search_fields = ("email", "nickname")
    ordering = ("-date_joined",)


@admin.register(EmailVerificationCode)
class EmailVerificationCodeAdmin(admin.ModelAdmin):
    list_display = ("email", "code", "verified", "expires_at")
