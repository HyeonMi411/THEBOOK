from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from .models import Cart, CartItem
from books.models import Book


def _serialize_cart(cart):
    items = []
    total = 0
    for item in cart.items.select_related("book").all():
        book = item.book
        subtotal = book.price * item.quantity
        total += subtotal
        items.append({
            "id": item.id,
            "bookId": book.id,
            "bookTitle": book.title,
            "bookCover": book.cover.url if book.cover else None,
            "price": book.price,
            "quantity": item.quantity,
            "subtotal": subtotal,
            "stockQuantity": book.stock_quantity,
            "bookDeleted": book.deleted,
        })
    return {"id": cart.id, "items": items, "totalAmount": total, "createdAt": cart.created_at}


@api_view(["GET", "POST", "DELETE"])
@permission_classes([IsAuthenticated])
def cart_view(request):
    cart, _ = Cart.objects.get_or_create(user=request.user)

    if request.method == "GET":
        return Response(_serialize_cart(cart))

    if request.method == "POST":
        book_id = request.data.get("bookId")
        quantity = int(request.data.get("quantity", 1))
        book = Book.objects.get(pk=book_id, deleted=False)

        item, created = CartItem.objects.get_or_create(cart=cart, book=book, defaults={"quantity": quantity})
        if not created:
            item.quantity += quantity
        if item.quantity > book.stock_quantity:
            return Response({"error": "재고를 초과하는 수량입니다."}, status=400)
        item.save()
        return Response(_serialize_cart(cart))

    # DELETE: 전체 비우기
    cart.items.all().delete()
    return Response({"message": "장바구니를 비웠습니다."})


@api_view(["DELETE", "PATCH"])
@permission_classes([IsAuthenticated])
def cart_item_detail(request, item_id):
    cart, _ = Cart.objects.get_or_create(user=request.user)
    item = CartItem.objects.get(pk=item_id, cart=cart)

    if request.method == "DELETE":
        item.delete()
        return Response({"message": "삭제되었습니다."})

    quantity = int(request.data.get("quantity", item.quantity))
    if quantity > item.book.stock_quantity:
        return Response({"error": "재고를 초과하는 수량입니다."}, status=400)
    item.quantity = quantity
    item.save()
    return Response(_serialize_cart(cart))
