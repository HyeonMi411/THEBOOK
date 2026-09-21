from django.contrib import admin
from .models import Book, BookLike


@admin.register(Book)
class BookAdmin(admin.ModelAdmin):
    list_display = ("id", "title", "author", "category", "price", "stock_quantity", "deleted")
    search_fields = ("title", "author")


admin.site.register(BookLike)
