"""
project_4/dashboard - Django 판매 통계 대시보드 설정

핵심 설계: Spring Boot(project_4/back)와 완전히 같은 Oracle DB를 "읽기 전용"으로
직접 연결합니다. Django ORM으로 새 테이블을 만들지 않고, Spring Boot가 이미 만든
ORDERS/ORDER_ITEMS/BOOK 테이블을 managed=False로 그대로 매핑해서 조회만 합니다.
(analytics/models.py 참고)

인증은 Spring Boot의 JWT/APP_USER와는 별개입니다. Django 자체 관리자 계정
(python manage.py createsuperuser)으로 대시보드 접근을 제어합니다.
"""
import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent


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


SECRET_KEY = env("DJANGO_SECRET_KEY", default="django-insecure-dev-only-change-me")
DEBUG = env_bool("DJANGO_DEBUG", default=True)
ALLOWED_HOSTS = [h.strip() for h in env("DJANGO_ALLOWED_HOSTS", default="localhost,127.0.0.1").split(",") if h.strip()]

INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "analytics",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
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

# ── 데이터베이스 ─────────────────────────────────────────────
# "default": Django 자체 관리자 계정(auth_user 등)을 저장할 아주 작은 로컬 SQLite.
#            Spring Boot의 Oracle DB에는 이 테이블들을 절대 만들지 않기 위해 분리함.
# "oracle" : Spring Boot(project_4/back)와 완전히 동일한 Oracle DB. 읽기 전용으로만 사용.
DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.sqlite3",
        "NAME": BASE_DIR / "dashboard_local.sqlite3",
    },
    "oracle": {
        "ENGINE": "django.db.backends.oracle",
        "NAME": env("ORACLE_DSN", default="localhost:1521/XE", required=True),
        "USER": env("ORACLE_USER", default="boot", required=True),
        "PASSWORD": env("ORACLE_PASSWORD", required=True),
    },
}

# analytics 앱의 모든 모델(ORDERS/ORDER_ITEMS/BOOK 매핑)은 "oracle" DB로만 라우팅
DATABASE_ROUTERS = ["config.db_router.OracleReadOnlyRouter"]

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
DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

LOGIN_URL = "/login"
LOGIN_REDIRECT_URL = "/"
