"""
Oracle 없이 돌아가는 테스트 (python manage.py test)
- pandas 집계 순수 함수 (analysis.py)
- Spring Boot → Django 통계 수신 API (토큰 검증 / 같은 날짜 갱신)
- Oracle 이 꺼져 있어도 대시보드가 안내 문구와 함께 열리는지
"""
import json
from datetime import date
from unittest import mock

from django.contrib.auth.models import User
from django.db.utils import DatabaseError
from django.test import TestCase, override_settings

from opslog.models import ServiceLog

from . import analysis


class AnalysisTests(TestCase):
    databases = {"default"}

    def test_sales_summary(self):
        records = [
            {"date": date(2026, 9, 1), "category": "소설", "quantity": 2, "amount": 30000},
            {"date": date(2026, 9, 1), "category": "IT", "quantity": 1, "amount": 25000},
            {"date": date(2026, 9, 2), "category": "소설", "quantity": 1, "amount": 15000},
        ]
        r = analysis.summarize_sales(records)
        self.assertEqual(r["top_category"], "소설")
        self.assertEqual(r["stats_summary"]["total_amount"], 70000)
        self.assertEqual(r["stats_summary"]["max_amount"], 55000)
        self.assertEqual(r["daily_dates"], ["2026-09-01", "2026-09-02"])
        self.assertAlmostEqual(sum(r["shares"]), 100.0, places=0)

    def test_sales_empty(self):
        self.assertEqual(analysis.summarize_sales([])["top_category"], "데이터 없음")

    def test_community_summary(self):
        posts = [{"date": date(2026, 9, 29)}, {"date": date(2026, 9, 29)}, {"date": date(2026, 9, 28)}]
        likes = [{"date": date(2026, 9, 29)}]
        r = analysis.summarize_community(posts, likes, [], [("java", 3), ("flutter", 5), ("책", 3)], 4,
                                         days=7, today=date(2026, 9, 29))
        self.assertEqual(len(r["activity_dates"]), 7)
        self.assertEqual(r["activity_posts"][-1], 2)
        self.assertEqual(r["activity_posts"][-2], 1)
        self.assertEqual(r["top_tags"][0], "flutter")
        self.assertEqual(r["community_summary"]["follows"], 4)

    def test_chatbot_summary(self):
        logs = [
            {"question": "결제", "faq_id": "shop-pay", "source": "faq"},
            {"question": "결제 방법", "faq_id": "shop-pay", "source": "menu"},
            {"question": "주식 추천", "faq_id": None, "source": "none"},
            {"question": "책 추천", "faq_id": None, "source": "ai"},
        ]
        r = analysis.summarize_chatbot(logs, {"shop-pay": "결제 방법"})
        self.assertEqual(r["chatbot_total"], 4)
        self.assertEqual(r["answer_rate"], 75.0)
        self.assertEqual(r["top_faqs"][0], {"faq": "결제 방법", "count": 2})
        self.assertEqual(r["unanswered"], ["주식 추천"])


@override_settings(STATS_SYNC_TOKEN="test-token")
class ReceiveStatisticsTests(TestCase):
    databases = {"default"}
    url = "/api/statistics/"
    body = {"date": "2026-09-29", "newUsers": 3, "newPosts": 5, "likes": 7, "comments": 2,
            "paidOrders": 4, "sales": 88000, "chatbotQuestions": 12, "chatbotUnanswered": 1}

    def post(self, body, token="test-token"):
        return self.client.post(self.url, data=json.dumps(body), content_type="application/json",
                                HTTP_X_SYNC_TOKEN=token)

    def test_rejects_wrong_token(self):
        self.assertEqual(self.post(self.body, token="nope").status_code, 403)
        self.assertEqual(ServiceLog.objects.count(), 0)

    def test_saves_and_updates_same_date(self):
        self.assertEqual(self.post(self.body).status_code, 200)
        self.assertEqual(self.post({**self.body, "sales": 99000}).status_code, 200)
        self.assertEqual(ServiceLog.objects.count(), 1)
        self.assertEqual(ServiceLog.objects.get().sales, 99000)

    def test_bad_payload(self):
        self.assertEqual(self.post({"newUsers": 1}).status_code, 400)

    def test_get_not_allowed(self):
        self.assertEqual(self.client.get(self.url).status_code, 405)


class DashboardViewTests(TestCase):
    databases = {"default"}

    def test_login_required(self):
        self.assertEqual(self.client.get("/").status_code, 302)

    def test_renders_when_oracle_is_down(self):
        User.objects.create_user("admin", password="pw-123456")
        self.client.login(username="admin", password="pw-123456")
        ServiceLog.objects.create(date=date(2026, 9, 29), new_users=1, paid_orders=2, chatbot_questions=3)
        with mock.patch("analytics.views._sales_context", side_effect=DatabaseError("down")):
            res = self.client.get("/")
        self.assertEqual(res.status_code, 200)
        self.assertContains(res, "Oracle")
        self.assertEqual(res.context["ops_dates"], ["2026-09-29"])
