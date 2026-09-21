"""
config/settings.py

BookStore project_5 (Django rewrite of project_3) settings.
project_3(Spring Boot) 배포 과정에서 겪었던 문제들을 처음부터 방지하도록 작성:
  - 도메인/프론트주소/시크릿은 전부 환경변수(os.environ)로 관리 (하드코딩 금지)
  - ALLOWED_HOSTS, CORS_ALLOWED_ORIGINS는 배포 도메인을 반드시 .env로 넣어야 동작
  - .env 자체는 .gitignore에 등록되어 있어 git에 올라가지 않음
"""
import os
from datetime import timedelta
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

# ── 환경변수 로딩 (.env) ────────────────────────────────────
# python-dotenv 없이도 동작하도록 간단히 직접 파싱 (배포환경에 추가 설치 최소화)
def _load_dotenv(path: Path):
    if not path.exists():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        os.environ.setdefault(key.strip(), value.strip())

_load_dotenv(BASE_DIR / ".env")


def env(key, default=None, required=False):
    val = os.environ.get(key, default)
    if required and (val is None or val == ""):
        raise RuntimeError(f"필수 환경변수 누락: {key} (.env 파일을 확인하세요)")
    return val


def env_bool(key, default=False):
    val = os.environ.get(key)
    if val is None:
        return default
    return val.strip().lower() in ("1", "true", "yes", "on")


def env_list(key, default=""):
    val = os.environ.get(key, default)
    return [v.strip() for v in val.split(",") if v.strip()]


# ── 기본 보안 설정 ───────────────────────────────────────────
SECRET_KEY = env("DJANGO_SECRET_KEY", default="django-insecure-dev-only-change-me")

DEBUG = env_bool("DJANGO_DEBUG", default=True)

# 배포 시 .env 에 DJANGO_ALLOWED_HOSTS=bookproject5.duckdns.org,localhost 형태로 반드시 채워야 함
ALLOWED_HOSTS = env_list("DJANGO_ALLOWED_HOSTS", default="localhost,127.0.0.1")

# ── 애플리케이션 ─────────────────────────────────────────────
INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "rest_framework",
    "rest_framework_simplejwt",
    "corsheaders",
    "accounts",
    "books",
    "cart",
    "orders",
    "notices",
    "analytics",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "corsheaders.middleware.CorsMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

ROOT_URLCONF = "config.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [BASE_DIR / "templates"],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.debug",
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

WSGI_APPLICATION = "config.wsgi.application"

# ── 사용자 모델 ──────────────────────────────────────────────
AUTH_USER_MODEL = "accounts.AppUser"

# ── 데이터베이스 ─────────────────────────────────────────────
if env("DB_ENGINE", default="sqlite") == "postgres":
    DATABASES = {
        "default": {
            "ENGINE": "django.db.backends.postgresql",
            "NAME": env("DB_NAME", required=True),
            "USER": env("DB_USER", required=True),
            "PASSWORD": env("DB_PASSWORD", required=True),
            "HOST": env("DB_HOST", default="localhost"),
            "PORT": env("DB_PORT", default="5432"),
        }
    }
else:
    DATABASES = {
        "default": {
            "ENGINE": "django.db.backends.sqlite3",
            "NAME": BASE_DIR / "db.sqlite3",
        }
    }

AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator"},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"},
]

LANGUAGE_CODE = "ko-kr"
TIME_ZONE = "Asia/Seoul"
USE_I18N = True
USE_TZ = True

STATIC_URL = "static/"
STATICFILES_DIRS = [BASE_DIR / "static"]
STATIC_ROOT = BASE_DIR / "staticfiles"

MEDIA_URL = "/media/"
MEDIA_ROOT = BASE_DIR / "media"

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

# ── REST Framework / JWT ────────────────────────────────────
REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": (
        "rest_framework_simplejwt.authentication.JWTAuthentication",
    ),
    "DEFAULT_PERMISSION_CLASSES": (
        "rest_framework.permissions.IsAuthenticatedOrReadOnly",
    ),
    "DEFAULT_PAGINATION_CLASS": "rest_framework.pagination.PageNumberPagination",
    "PAGE_SIZE": 12,
}

SIMPLE_JWT = {
    "ACCESS_TOKEN_LIFETIME": timedelta(minutes=15),
    "REFRESH_TOKEN_LIFETIME": timedelta(days=14),
    "ROTATE_REFRESH_TOKENS": True,
    "AUTH_HEADER_TYPES": ("Bearer",),
}

# ── CORS / 프론트엔드 주소 ───────────────────────────────────
FRONTEND_BASE_URL = env("FRONTEND_BASE_URL", default="http://localhost:3000")
CORS_ALLOWED_ORIGINS = env_list("CORS_ALLOWED_ORIGINS", default=FRONTEND_BASE_URL)
CORS_ALLOW_CREDENTIALS = True

# ── 이메일 (회원가입 인증번호 발송) ───────────────────────────
EMAIL_BACKEND = "django.core.mail.backends.smtp.EmailBackend"
EMAIL_HOST = "smtp.gmail.com"
EMAIL_PORT = 587
EMAIL_USE_TLS = True
EMAIL_HOST_USER = env("GMAIL_USERNAME", default="")
EMAIL_HOST_PASSWORD = env("GMAIL_APP_PASSWORD", default="")
DEFAULT_FROM_EMAIL = EMAIL_HOST_USER

# 이메일 미설정 시(.env 없이 로컬 개발) 콘솔에 인증번호를 그냥 찍어주도록 자동 전환
if not EMAIL_HOST_USER or not EMAIL_HOST_PASSWORD:
    EMAIL_BACKEND = "django.core.mail.backends.console.EmailBackend"

# ── 카카오페이 ───────────────────────────────────────────────
KAKAO_PAY_SECRET_KEY = env("KAKAO_PAY_SECRET_KEY", default="")
KAKAO_PAY_CID = env("KAKAO_PAY_CID", default="TC0ONETIME")

# ── OpenAI (AI 문서 챗봇) ────────────────────────────────────
OPENAI_API_KEY = env("OPENAI_API_KEY", default="")

# ── 국립중앙도서관 오픈API ───────────────────────────────────
NL_API_KEY = env("NL_API_KEY", default="")

# ── 카카오 로그인/도서검색 ───────────────────────────────────
KAKAO_REST_API_KEY = env("KAKAO_CLIENT_ID", default="")
