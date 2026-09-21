from django.urls import path
from . import views

urlpatterns = [
    path("signup", views.signup),
    path("login", views.login),
    path("email/send-code", views.send_email_code),
    path("email/verify-code", views.verify_email_code),
    path("check-email", views.check_email),
    path("check-nickname", views.check_nickname),
    path("me", views.MeView.as_view()),
    path("<int:user_id>/nickname", views.update_nickname),
    path("<int:user_id>/profile-image", views.update_profile_image),
]
