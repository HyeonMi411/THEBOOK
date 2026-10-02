from django.urls import path

from . import views

urlpatterns = [
    path("", views.dashboard_view, name="dashboard"),
    path("sync", views.sync_now, name="sync_now"),
    # Spring Boot(StatisticsSyncService) 가 호출 - DJANGO_SYNC_URL = http://<대시보드>/api/statistics/
    path("api/statistics/", views.api_receive_statistics, name="api_receive_statistics"),
    # 앱 관리자 통계 화면 - Spring Boot(/api/admin/stats)가 X-Sync-Token 을 붙여 호출
    path("api/stats/", views.api_stats, name="api_stats"),
]
