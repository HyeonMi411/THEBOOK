from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.urls import path, include
from django.views.generic import RedirectView, TemplateView
from rest_framework_simplejwt.views import TokenRefreshView

urlpatterns = [
    path("admin/", admin.site.urls),

    # ── 프론트엔드 화면 (Django Template) ──
    path("", TemplateView.as_view(template_name="pages/home.html"), name="home"),
    path("login", TemplateView.as_view(template_name="pages/login.html"), name="login_page"),
    path("signup", TemplateView.as_view(template_name="pages/signup.html"), name="signup_page"),
    path("mypage", TemplateView.as_view(template_name="pages/mypage.html"), name="mypage_page"),
    path("cart", TemplateView.as_view(template_name="pages/cart.html"), name="cart_page"),

    # ── REST API (project_3의 /auth, /api 구조와 최대한 동일하게 유지) ──
    path("auth/", include("accounts.urls")),
    path("auth/refresh", TokenRefreshView.as_view(), name="token_refresh"),
    path("api/books/", include("books.urls")),
    path("api/cart/", include("cart.urls")),
    path("api/orders/", include("orders.urls")),
    path("api/notices/", include("notices.urls")),

    # ── 통계 대시보드 (관리자 전용) ──
    path("dashboard/", include("analytics.urls")),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
