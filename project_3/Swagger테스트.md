# BookStore 결제 기능(Cart/Order/Payment) Swagger 테스트 가이드

Cart Api / Order Api / Payment Api를 Swagger UI에서 순서대로 테스트하는 방법입니다.

---

## 전체 흐름 요약

```
로그인(JWT) → Authorize 등록 → 재고확인 → 장바구니담기 → 주문생성(PENDING)
→ 결제준비(redirectUrl) → [브라우저에서 실제 결제] → pg_token 확보
→ 결제승인(PAID, 재고차감) → 주문내역 확인
```

---

## 1단계 — 로그인해서 JWT 발급받기

Swagger UI에서 `POST /auth/login` 호출.

```json
{
  "email": "admin_test@thejoa703.com",
  "password": "admin1234"
}
```

응답으로 `accessToken`이 옵니다. 이 값을 복사해둡니다.

---

## 2단계 — Swagger에 토큰 등록 (Authorize)

Swagger UI 우상단의 **자물쇠(Authorize)** 버튼 클릭 → 복사한 `accessToken`을 붙여넣기 → **Authorize** 클릭.

(앞에 `Bearer ` 접두사가 필요한지는 Swagger 설정에 따라 다릅니다 — 보통 토큰값만 넣으면 자동으로 붙습니다)

이제부터 모든 API 호출에 이 토큰이 자동으로 실립니다.

---

## 3단계 — 도서 재고 확인 (있어야 함)

`GET /api/books`로 도서 목록 조회 → `stockQuantity` 값이 0보다 큰 도서의 `bookId`를 하나 골라둡니다.

- `dummy_data_book_stock.sql`을 이미 돌렸다면 이 단계는 생략 가능합니다.
- 재고가 없으면 장바구니 담기/주문 자체가 거부됩니다.
- 재고가 아예 없는 도서라면 `PATCH /api/books/{id}/stock` (관리자 전용, body: `{"stockQuantity": 10}`)로 먼저 채워주세요.

---

## 4단계 — 장바구니에 담기 (Cart Api)

```
POST /api/cart
```
```json
{
  "bookId": 위에서 골라온 ID,
  "quantity": 2
}
```

응답의 `items` 배열에서 `CartItem`의 `id`(cartItemId)를 기억해둡니다 → 다음 단계에서 사용.

추가로 테스트 가능한 것들:
| Method | URL | 설명 |
|---|---|---|
| GET | `/api/cart` | 장바구니 조회 |
| PATCH | `/api/cart/{itemId}` | 수량수정 (body: `{"quantity": N}`) |
| DELETE | `/api/cart/{itemId}` | 항목 선택삭제 |
| DELETE | `/api/cart` | 장바구니 전체비우기 |

---

## 5단계 — 주문 생성 (Order Api)

```
POST /api/orders
```

**장바구니 결제**면:
```json
{ "cartItemIds": [방금 그 cartItemId] }
```

**바로구매**면:
```json
{ "bookId": ID, "quantity": 1 }
```

(이 둘 중 하나만 채우면 됩니다)

응답의 `id`(orderId)를 기억해둡니다. `orderStatus`가 `PENDING`으로 나오면 정상입니다.

---

## 6단계 — 결제준비 (Payment Api)

```
POST /api/payments/kakao/ready
```
```json
{ "orderId": 방금 받은 orderId }
```

응답의 `redirectUrl`을 복사합니다. 이 URL을 **브라우저 주소창에 직접 열면** 카카오페이 테스트 결제창(`cid=TC0ONETIME`)이 뜹니다.

---

## 7단계 — 실제 결제창에서 결제 진행 (브라우저)

카카오 계정으로 로그인 후 테스트 결제를 진행합니다 (실제 돈은 안 나감).

결제가 끝나면 다음 주소로 리다이렉트됩니다:
```
http://localhost:3000/payment/complete?orderId=X&pg_token=T5279...(긴 문자열)
```

- front3가 떠있으면 그 화면이 **자동으로 승인 처리**까지 해버립니다.
- Swagger에서 직접 테스트하려면 front3는 잠깐 꺼두거나, 주소창의 `pg_token=` 뒤 값만 복사해두세요.

---

## 8단계 — 결제승인 (Payment Api)

```
POST /api/payments/kakao/approve
```
```json
{
  "orderId": 그 orderId,
  "pgToken": 방금 복사한 값
}
```

응답의 `orderStatus`가 `PAID`로 바뀌면 성공입니다. **이 시점에 실제로 재고가 차감됩니다** → `GET /api/books/{bookId}`로 `stockQuantity`가 줄었는지 확인해보세요.

---

## 9단계 — 주문내역 확인 (Order Api)

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/orders?page=1&size=12` | 내 주문 목록 (12개씩 페이징) |
| GET | `/api/orders/{id}` | 방금 그 주문 상세 |

`orderStatus=PAID`, 구매한 도서 목록이 정확히 나와야 합니다.

---

## pg_token이란?

카카오페이가 "이 사람이 진짜로 결제를 마쳤다"는 걸 증명하는 **1회용 토큰**입니다. 카카오 서버가 직접 발급하고, 특정 `tid`(결제준비 시 받은 거래번호)에 묶여있어서 임의로 만들어낼 수 없습니다. 그래서 Swagger에서 `/approve`를 테스트하려면 **반드시 6~7단계로 실제 결제창을 한 번 거쳐야** 진짜 값을 얻을 수 있습니다.

---

## 실무 팁

- **6~7단계(브라우저 결제)가 제일 번거로운 부분**입니다. 매번 하기 귀찮으면, `Boot2ApplicationTests_6_PaymentService.java`(JUnit 테스트)를 실행하는 게 낫습니다 — 카카오 API를 가짜(`@MockBean`)로 대체해서 `pg_token` 없이도 승인→재고차감 로직을 자동으로 검증합니다.
  - **Swagger**: "진짜 결제창까지 뜨는지" 눈으로 확인하는 용도
  - **JUnit 테스트**: "비즈니스 로직이 맞는지" 자동 검증하는 용도

- **취소/실패도 테스트하고 싶으면**:
  ```
  POST /api/payments/kakao/cancel/{orderId}   (body 없이 orderId만 경로변수로)
  POST /api/payments/kakao/fail/{orderId}
  ```

- **재고부족 케이스 테스트**: `dummy_data_book_stock.sql`을 돌렸다면 "아몬드"(재고 2권), "에디톨로지"(품절)로 `POST /api/cart`나 `POST /api/orders`를 호출해보면 재고부족 에러(400, 한글 메시지)가 정상적으로 나오는지 확인할 수 있습니다.

- **본인 것 아닌 주문/장바구니 접근 테스트**: 다른 계정으로 로그인해서(Authorize 토큰 교체) 남의 `orderId`/`itemId`로 조회·수정 시도하면 거부되는지도 확인해보세요.
