"""
Spring Boot(StatisticsSyncService)가 매일 밀어주는 운영 지표를 쌓아두는 테이블.
flutter 수업(track011)의 ServiceLog 를 확장한 것으로, Oracle 이 아니라 Django 자체 DB(sqlite)에 저장된다.
(Oracle 은 Spring Boot 소유라 Django 는 절대 쓰지 않는다 - config/db_router.py)
"""
from django.db import models


class ServiceLog(models.Model):
    date = models.DateField(unique=True, verbose_name="날짜")
    new_users = models.IntegerField(default=0, verbose_name="신규 가입")
    new_posts = models.IntegerField(default=0, verbose_name="새 게시글")
    likes = models.IntegerField(default=0, verbose_name="좋아요")
    comments = models.IntegerField(default=0, verbose_name="댓글")
    paid_orders = models.IntegerField(default=0, verbose_name="결제 건수")
    sales = models.BigIntegerField(default=0, verbose_name="매출(원)")
    chatbot_questions = models.IntegerField(default=0, verbose_name="챗봇 질문")
    chatbot_unanswered = models.IntegerField(default=0, verbose_name="챗봇 미응답")
    synced_at = models.DateTimeField(auto_now=True, verbose_name="마지막 동기화")

    class Meta:
        ordering = ["date"]

    def __str__(self):
        return f"[{self.date}] 운영 지표"
