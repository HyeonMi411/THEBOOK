from django.urls import path
from . import views

urlpatterns = [
    path("", views.BookListCreateView.as_view()),
    path("search", views.search_books),
    path("bestsellers", views.bestsellers),
    path("national-library/search", views.search_national_library),
    path("kakao-insert", views.kakao_book_search_insert),
    path("<int:pk>", views.BookDetailView.as_view()),
    path("<int:pk>/stock", views.update_stock),
]
