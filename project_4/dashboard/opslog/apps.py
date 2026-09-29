from django.apps import AppConfig


class OpslogConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "opslog"
    verbose_name = "운영 지표 (Spring Boot 동기화)"
