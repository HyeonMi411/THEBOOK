from django.contrib.auth.models import AbstractUser
from django.db import models


class AppUser(AbstractUser):
    """
    project_3의 APP_USER 테이블에 대응.
    Django의 username 대신 email을 로그인 식별자로 사용.
    """
    ROLE_CHOICES = (
        ("ROLE_USER", "일반회원"),
        ("ROLE_ADMIN", "관리자"),
    )
    PROVIDER_CHOICES = (
        ("local", "일반가입"),
        ("google", "구글"),
        ("kakao", "카카오"),
        ("naver", "네이버"),
    )

    email = models.EmailField(unique=True)
    nickname = models.CharField(max_length=50, unique=True)
    mobile = models.CharField(max_length=20, null=True, blank=True)
    role = models.CharField(max_length=20, choices=ROLE_CHOICES, default="ROLE_USER")
    provider = models.CharField(max_length=20, choices=PROVIDER_CHOICES, default="local")
    provider_id = models.CharField(max_length=100, null=True, blank=True)
    profile_image = models.ImageField(upload_to="uploads/profile/", null=True, blank=True)
    # 소프트 삭제 - project_3에서 회원탈퇴 후 재가입 이슈를 겪었던 경험을 반영해,
    # 여기서는 탈퇴 시 email/nickname을 함께 비워서(anonymize) 재가입까지 막지 않도록 설계.
    deleted = models.BooleanField(default=False)

    USERNAME_FIELD = "email"
    REQUIRED_FIELDS = ["username", "nickname"]

    def __str__(self):
        return f"{self.email} ({self.role})"


class EmailVerificationCode(models.Model):
    """
    회원가입 시 이메일 인증번호 (project_3의 Redis TTL 방식을 DB expires_at으로 대체).
    """
    email = models.EmailField()
    code = models.CharField(max_length=6)
    verified = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)
    expires_at = models.DateTimeField()

    class Meta:
        indexes = [models.Index(fields=["email"])]

    def __str__(self):
        return f"{self.email} - {self.code}"
