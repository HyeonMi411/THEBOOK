from django.urls import path
from . import views

urlpatterns = [
    path("", views.cart_view),
    path("<int:item_id>", views.cart_item_detail),
]
