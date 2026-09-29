from django.urls import path

from . import views

urlpatterns = [
    path("", views.dashboard_view, name="dashboard"),
    path("sync", views.sync_now, name="sync_now"),
    # Spring Boot(StatisticsSyncService) 가 호출 - DJANGO_SYNC_URL = http://<대시보드>/api/statistics/
    path("api/statistics/", views.api_receive_statistics, name="api_receive_statistics"),
]
