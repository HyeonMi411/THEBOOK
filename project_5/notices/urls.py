from django.urls import path
from . import views

urlpatterns = [
    path("", views.NoticeListCreateView.as_view()),
    path("search", views.search_notices),
    path("<int:pk>", views.NoticeDetailView.as_view()),
]
