from django.db import models


class ServiceLog(models.Model):
    """
    첨부해주신 python_django.zip의 ServiceLog를 그대로 유지.
    (방문자수 등 주문 테이블만으로는 알 수 없는 지표를 기록하고 싶을 때 사용)
    """
    date = models.DateField(verbose_name="날짜")
    category = models.CharField(max_length=50, verbose_name="카테고리")
    visitor_count = models.IntegerField(default=0, verbose_name="방문자수")
    sales_amount = models.IntegerField(default=0, verbose_name="매출액")

    def __str__(self):
        return f"[{self.date}] {self.category} 로그"
