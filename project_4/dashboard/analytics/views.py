"""
project_4 판매 통계 대시보드.
project_5(Django 완전재작성) 때 만들었던 pandas 집계 로직을 재사용하되,
이번엔 Django 자체 DB가 아니라 Spring Boot(project_4/back) 소유의 Oracle을
읽기 전용으로 직접 조회해서 분석한다 (models.py의 managed=False 매핑 참고).
"""
import pandas as pd
from django.contrib.auth.decorators import login_required
from django.shortcuts import render

from .models import OrderItem


@login_required
def dashboard_view(request):
    # 결제완료(PAID) 주문만 집계 대상 (PENDING/CANCELLED/FAILED 제외)
    rows = (
        OrderItem.objects.using("oracle")
        .filter(order__order_status="PAID")
        .select_related("order", "book")
    )

    records = [{
        "date": item.order.approved_at.date() if item.order.approved_at else item.order.created_at.date(),
        "category": item.book.category,
        "quantity": item.quantity,
        "amount": item.price * item.quantity,
    } for item in rows]

    if records:
        df = pd.DataFrame(records)

        summary_df = df.groupby("category")[["quantity", "amount"]].sum().reset_index()
        total_qty = summary_df["quantity"].sum()
        summary_df["share"] = (summary_df["quantity"] / total_qty * 100).round(1) if total_qty else 0

        daily = df.groupby("date")["amount"].sum()
        stats_summary = {
            "avg_amount": int(round(daily.mean(), 0)),
            "max_amount": int(daily.max()),
            "total_amount": int(df["amount"].sum()),
            "total_orders": int(df.shape[0]),
        }
        top_category = summary_df.loc[summary_df["quantity"].idxmax(), "category"]

        categories = summary_df["category"].tolist()
        quantities = summary_df["quantity"].tolist()
        amounts = summary_df["amount"].tolist()
        shares = summary_df["share"].tolist()
    else:
        categories, quantities, amounts, shares = [], [], [], []
        stats_summary = {"avg_amount": 0, "max_amount": 0, "total_amount": 0, "total_orders": 0}
        top_category = "데이터 없음"

    context = {
        "categories": categories,
        "quantities": quantities,
        "amounts": amounts,
        "shares": shares,
        "stats_summary": stats_summary,
        "top_category": top_category,
    }
    return render(request, "analytics/dashboard.html", context)
