"""
Spring Boot(project_4/back)가 JPA로 이미 만들어둔 Oracle 테이블을 그대로 매핑.
managed=False: Django가 이 테이블에 대해 마이그레이션(생성/변경/삭제)을 절대 하지 않음
(스키마의 주인은 어디까지나 Spring Boot/JPA).

컬럼명은 project_4/back/src/main/java/com/thejoa703/entity/의
Orders.java, OrderItem.java, Book.java에 있는 @Column(name=...) 값과 정확히 일치시켰음.
"""
from django.db import models


class Book(models.Model):
    id = models.BigAutoField(db_column="BOOK_ID", primary_key=True)
    title = models.CharField(max_length=255, db_column="TITLE")
    author = models.CharField(max_length=100, db_column="AUTHOR")
    publisher = models.CharField(max_length=100, db_column="PUBLISHER")
    category = models.CharField(max_length=50, db_column="CATEGORY")

    class Meta:
        managed = False
        db_table = "BOOK"


class Orders(models.Model):
    id = models.BigAutoField(db_column="ID", primary_key=True)
    app_user_id = models.BigIntegerField(db_column="APP_USER_ID")
    total_amount = models.IntegerField(db_column="TOTAL_AMOUNT")
    order_status = models.CharField(max_length=20, db_column="ORDER_STATUS")
    created_at = models.DateTimeField(db_column="CREATED_AT")
    approved_at = models.DateTimeField(db_column="APPROVED_AT", null=True)

    class Meta:
        managed = False
        db_table = "ORDERS"


class OrderItem(models.Model):
    id = models.BigAutoField(db_column="ID", primary_key=True)
    order = models.ForeignKey(
        Orders, on_delete=models.DO_NOTHING, db_column="ORDER_ID", related_name="items"
    )
    book = models.ForeignKey(
        Book, on_delete=models.DO_NOTHING, db_column="BOOK_ID"
    )
    quantity = models.IntegerField(db_column="QUANTITY")
    price = models.IntegerField(db_column="PRICE")
    book_title_snapshot = models.CharField(max_length=255, db_column="BOOK_TITLE_SNAPSHOT")

    class Meta:
        managed = False
        db_table = "ORDER_ITEMS"
