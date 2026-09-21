import pandas as pd
from django.contrib.admin.views.decorators import staff_member_required
from django.shortcuts import render

from orders.models import OrderItem


@staff_member_required
def dashboard_view(request):
    """
    관리자 전용 통계 대시보드.
    project_3에는 없던 '5차 프로젝트' 추가 기능 - 실제 결제완료(PAID) 주문 데이터를
    pandas로 분석해서 카테고리별 매출/판매량, 일자별 트렌드를 보여줌.
    (첨부하신 python_django.zip은 Spring Boot와 별도 프로세스로 동기화하는 구조였는데,
    project_5는 백엔드 자체가 Django이므로 동기화 과정 없이 바로 자체 DB를 분석하도록 단순화함)
    """
    qs = (
        OrderItem.objects.filter(order__order_status="PAID")
        .values("order__created_at", "book__category")
        .annotate()
    )
    rows = OrderItem.objects.filter(order__order_status="PAID").select_related("order", "book")

    records = [{
        "date": item.order.created_at.date(),
        "category": item.book.category,
        "quantity": item.quantity,
        "amount": item.price * item.quantity,
    } for item in rows]

    if records:
        df = pd.DataFrame(records)

        summary_df = df.groupby("category")[["quantity", "amount"]].sum().reset_index()
        total_qty = summary_df["quantity"].sum()
        summary_df["share"] = (summary_df["quantity"] / total_qty * 100).round(1) if total_qty else 0

        stats_summary = {
            "avg_amount": round(df.groupby("date")["amount"].sum().mean(), 0),
            "max_amount": int(df.groupby("date")["amount"].sum().max()),
            "total_amount": int(df["amount"].sum()),
            "total_orders": int(df.shape[0]),
        }
        top_category = summary_df.loc[summary_df["quantity"].idxmax(), "category"]

        pivot_df = df.pivot_table(index="date", columns="category", values="amount", aggfunc="sum", fill_value=0).reset_index()

        categories = summary_df["category"].tolist()
        quantities = summary_df["quantity"].tolist()
        amounts = summary_df["amount"].tolist()
        shares = summary_df["share"].tolist()
        trend_dates = pivot_df["date"].astype(str).tolist()
    else:
        categories, quantities, amounts, shares, trend_dates = [], [], [], [], []
        stats_summary = {"avg_amount": 0, "max_amount": 0, "total_amount": 0, "total_orders": 0}
        top_category = "데이터 없음"

    context = {
        "categories": categories,
        "quantities": quantities,
        "amounts": amounts,
        "shares": shares,
        "stats_summary": stats_summary,
        "top_category": top_category,
        "trend_dates": trend_dates,
    }
    return render(request, "analytics/dashboard.html", context)
