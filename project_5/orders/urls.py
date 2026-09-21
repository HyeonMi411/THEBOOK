from django.urls import path
from . import views

urlpatterns = [
    path("", views.order_list_create),
    path("<int:pk>", views.order_detail),
    path("payments/kakao/ready", views.kakao_ready),
    path("payments/kakao/approve", views.kakao_approve),
    path("payments/kakao/cancel/<int:pk>", views.kakao_cancel),
    path("payments/kakao/fail/<int:pk>", views.kakao_fail),
]
