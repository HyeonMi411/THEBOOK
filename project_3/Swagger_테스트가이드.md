# Swagger 전체 API 테스트 순서 가이드 (장바구니 → 주문 → 결제 포함)

이 문서는 `http://localhost:8080/swagger-ui/index.html`에서 회원가입부터 결제까지 전체 흐름을 순서대로 테스트하는 방법을 정리한 것입니다. 실제 컨트롤러 코드를 확인해서 정확한 요청 형식을 담았습니다.

**전제조건**: 백엔드(`boot3`)가 8080 포트로 정상 기동되어 있어야 합니다. (`BookStore_실행가이드.md` 참고)

---

## 0. Swagger 접속 및 인증 버튼 사용법

1. 브라우저에서 `http://localhost:8080/swagger-ui/index.html` 접속
2. 화면 우측 상단의 **`Authorize`** 버튼을 누르면 인증 방식 선택 창이 뜹니다.
3. 이 프로젝트는 JWT 방식(`bearerAuth`)을 씁니다. **AccessToken 문자열만** 입력하면 됩니다 (`Bearer ` 접두사는 Swagger가 자동으로 붙여줍니다).
4. 아직 토큰이 없으므로, 아래 1~2단계를 먼저 진행해서 토큰을 발급받은 뒤 여기로 돌아와 입력하세요.

> 장바구니/주문/결제 API는 전부 로그인이 필요합니다(`Authentication` 파라미터로 로그인한 사용자 ID를 꺼내 씀). Authorize를 안 하면 401이 납니다.

---

## 1. 회원가입 (`POST /auth/signup`)

**"User Api"** 태그에서 실행합니다. `multipart/form-data` 방식이라 Swagger에서 폼 형태로 입력창이 나옵니다.

| 필드 | 값 예시 |
|---|---|
| email | test@example.com |
| password | test1234 |
| nickname | 테스터 |
| ufile | (선택, 프로필 이미지 파일) |

**Execute** 후 200 응답이 오면 가입 완료입니다. 이때 생성된 계정은 **자동으로 `ROLE_USER`**입니다 (`UserService.createUser()`).

### 관리자(ROLE_ADMIN) 계정이 필요하다면

도서 등록/수정/삭제, 카카오 자동등록, 재고 수정은 `@PreAuthorize("hasRole('ADMIN')")`가 걸려있어서 `ROLE_USER`로는 403이 납니다. API로 가입한 계정은 전부 `ROLE_USER`로 고정되어 있어서, **관리자 계정은 DB에서 직접 role을 바꿔야 합니다.**

```sql
-- 방금 가입한 계정을 관리자로 승격
UPDATE APP_USER SET ROLE = 'ROLE_ADMIN' WHERE EMAIL = 'test@example.com';
COMMIT;
```

---

## 2. 로그인 (`POST /auth/login`)

**"User Api"** 태그, `application/json` 방식입니다.

```json
{
  "email": "test@example.com",
  "password": "test1234",
  "provider": "local"
}
```

**응답 예시**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9....",
  "user": { "id": 1, "email": "test@example.com", "role": "ROLE_ADMIN", ... }
}
```

**`accessToken` 값을 복사**해서, 상단의 `Authorize` 버튼에 붙여넣고 `Authorize` → `Close` 하세요. 이후 모든 요청에 자동으로 `Authorization: Bearer {token}` 헤더가 실립니다.

> AccessToken은 **15분 후 만료**됩니다(`access-token-exp-seconds: 900`). 테스트 중 갑자기 401이 나면 다시 로그인해서 새 토큰으로 Authorize 하세요.

---

## 3. 구매할 도서 준비 — 도서 등록 (`POST /api/books`)

**"Book Api"** 태그. **관리자 계정으로 Authorize한 상태여야** 합니다. `multipart/form-data` 방식입니다.

| 필드 | 값 예시 |
|---|---|
| title | 테스트 도서 |
| author | 홍길동 |
| publisher | 테스트출판사 |
| publishDate | 2024-01-01 (비워도 됨 — 미상 처리) |
| category | 소설 |
| price | 15000 |
| cover | (선택, 표지 이미지 파일) |

**Execute** 후 응답에서 `id`(도서ID)를 기억해두세요. 이후 재고를 등록해야 장바구니/구매가 가능합니다.

### 재고 등록 (`PATCH /api/books/{id}/stock`)

```json
{ "stockQuantity": 10 }
```
`{id}`에 방금 만든 도서 ID를 넣고 실행합니다. (재고가 0이면 장바구니 담기부터 막힙니다.)

---

## 4. 장바구니 테스트 ("Cart Api" 태그)

**이제부터는 구매자(일반 사용자든 관리자든 로그인만 되어 있으면 됨) 계정으로 진행합니다.** 관리자로 계속 진행해도 되고, 별도 일반회원으로 다시 로그인해도 됩니다.

### 4-1. 장바구니에 담기 (`POST /api/cart`)
```json
{ "bookId": 1, "quantity": 2 }
```
`bookId`는 3단계에서 만든 도서 ID로 바꾸세요. 응답으로 현재 장바구니 전체 상태(`items`, `totalAmount`)가 돌아옵니다. 이 응답에서 **`items[0].id`(장바구니 항목 ID)**를 기억해두세요 — 다음 단계에서 씁니다.

### 4-2. 장바구니 조회 (`GET /api/cart`)
파라미터 없이 바로 실행. 방금 담은 항목이 보이는지 확인합니다.

### 4-3. 수량 수정 (`PATCH /api/cart/{itemId}`)
```json
{ "quantity": 3 }
```
`{itemId}`에 4-1에서 기억해둔 장바구니 항목 ID를 넣습니다. 재고보다 많은 수량을 넣으면 400 에러가 나는지도 확인해보세요.

### 4-4. 항목 선택삭제 (`DELETE /api/cart/{itemId}`) — 선택사항
지금 단계에서는 실행하지 마세요(다음 단계에서 이 항목으로 주문을 만들 것입니다). 삭제 동작만 확인하고 싶다면 별도 도서를 하나 더 담아서 테스트하세요.

---

## 5. 주문 생성 ("Order Api" 태그)

### 방법 A — 장바구니 결제 (`POST /api/orders`)
```json
{ "cartItemIds": [1] }
```
`cartItemIds`에 4단계에서 담은 장바구니 항목 ID들을 배열로 넣습니다. 성공하면 주문이 **PENDING(결제대기)** 상태로 생성되고, 사용했던 장바구니 항목은 자동으로 비워집니다.

### 방법 B — 바로구매 (`POST /api/orders`)
장바구니를 거치지 않고 즉시 주문을 만들고 싶다면:
```json
{ "bookId": 1, "quantity": 1 }
```

**응답에서 `id`(주문 ID)를 기억해두세요.** 결제 단계에서 필요합니다.

### 주문 목록/상세 확인
- `GET /api/orders?page=1&size=12` — 내 주문내역 페이징 조회
- `GET /api/orders/{id}` — 방금 만든 주문 상세 조회 (상태가 `PENDING`인지 확인)

---

## 6. 결제 ("Payment Api" 태그) — 여기서부터는 완전 자동화가 어렵습니다

**중요**: 결제 승인에는 카카오페이가 실제로 발급하는 `pg_token`이 필요합니다. 이건 **실제 카카오페이 결제창에서 결제를 진행해야만** 받을 수 있는 값이라, Swagger에서 임의의 값을 넣으면 카카오페이 서버가 거부해서 승인이 실패합니다. 아래 순서로 실제 흐름을 재현해야 합니다.

### 6-1. 결제 준비 (`POST /api/payments/kakao/ready`)
```json
{ "orderId": 1 }
```
`orderId`에 5단계에서 만든 주문 ID를 넣습니다. 성공하면 응답에 `redirectUrl`(카카오페이 결제창 주소)이 돌아옵니다.

```json
{
  "orderId": 1,
  "tid": "T1234567890123456789",
  "redirectUrl": "https://mockup-pg-web.kakao.com/v1/..."
}
```

### 6-2. 실제 결제 진행 (브라우저에서 별도로)
응답받은 `redirectUrl`을 **브라우저 주소창에 직접 붙여넣어 접속**하세요. 카카오페이 결제창이 뜨고, 카카오페이 개발자센터에서 등록한 테스트 계정(또는 실제 카카오페이 계정, `cid`가 `TC0ONETIME`이면 테스트 결제)으로 결제를 진행합니다.

결제가 완료되면 `application.yml`/`PaymentService`에 설정된 `approval_url`(`{frontend-base-url}/payment/complete?orderId={orderId}`)로 리다이렉트되면서, URL 쿼리스트링에 **`pg_token`**이 함께 붙어서 돌아옵니다. 예:
```
http://localhost:3000/payment/complete?orderId=1&pg_token=T5789...
```
이 `pg_token` 값을 복사하세요.

### 6-3. 결제 승인 (`POST /api/payments/kakao/approve`)
```json
{ "orderId": 1, "pgToken": "6-2에서 복사한 pg_token 값" }
```
성공하면 주문 상태가 **PAID(결제완료)**로 바뀌고, 이 시점에 실제로 재고가 차감됩니다. 응답으로 최종 주문 정보가 돌아옵니다.

### 결제를 실제로 완료하지 않고 흐름만 확인하고 싶다면
- `POST /api/payments/kakao/cancel/{orderId}` — 결제 취소 상태로 전환 (pg_token 불필요)
- `POST /api/payments/kakao/fail/{orderId}` — 결제 실패 상태로 전환 (pg_token 불필요)

이 두 API는 `pg_token` 없이 바로 호출 가능해서, "결제 승인까지 안 가고 취소/실패 흐름만 확인하고 싶을 때" 유용합니다.

---

## 7. 결제 후 확인

- `GET /api/orders/{id}` — 주문 상태가 `PAID`로 바뀌었는지, `approvedAt`(승인시각)이 채워졌는지 확인
- `GET /api/cart` — 결제에 사용한 장바구니 항목이 비워졌는지 확인
- `GET /api/books/{id}` — 재고(`stockQuantity`)가 주문 수량만큼 줄었는지 확인
- `GET /api/books/bestsellers` — 결제 완료 건이 베스트셀러 집계에 반영되는지 확인 (캐시 TTL 10분이라, 방금 결제했다면 캐시가 즉시 무효화되어 바로 반영됩니다)

---

## 8. 주문 삭제 (`DELETE /api/orders/{id}`)

- 주문이 아직 `PENDING`(결제 전)이면 → 실제로 DB에서 삭제됩니다.
- 주문이 `PAID`/`CANCELLED`/`FAILED`면 → 실제로 지워지지 않고 "숨기기" 처리되어, 이후 `GET /api/orders` 목록에는 안 보입니다(회계 이력 보존).

두 경우를 각각 테스트해보면 차이를 확인할 수 있습니다 (PENDING 주문 하나, PAID 주문 하나를 각각 삭제해보세요).

---

## 9. 전체 흐름 요약 (순서대로)

```
1. POST /auth/signup                       회원가입
2. (DB에서 직접 ROLE_ADMIN 으로 변경)        관리자 승격
3. POST /auth/login                        로그인 → accessToken 획득
4. (Swagger Authorize 버튼에 토큰 입력)
5. POST /api/books                         테스트용 도서 등록 (관리자)
6. PATCH /api/books/{id}/stock             재고 등록 (관리자)
7. POST /api/cart                          장바구니 담기
8. POST /api/orders  { cartItemIds:[...] } 주문 생성 (PENDING)
9. POST /api/payments/kakao/ready          결제 준비 → redirectUrl 획득
10. (브라우저에서 redirectUrl 접속 → 실제 결제 → pg_token 획득)
11. POST /api/payments/kakao/approve       결제 승인 (PAID, 재고차감)
12. GET  /api/orders/{id}                  결과 확인
```

---

## 10. 자주 겪는 문제

| 증상 | 원인 | 해결 |
|---|---|---|
| 장바구니/주문 API 호출시 401 | Authorize 안 했거나 토큰 만료(15분) | 다시 로그인 → 새 토큰으로 Authorize |
| 도서 등록/재고수정 시 403 | 일반회원(ROLE_USER) 계정으로 시도함 | DB에서 해당 계정 ROLE을 `ROLE_ADMIN`으로 변경 |
| 장바구니 담기 시 "재고가 부족합니다" | 재고 등록을 안 했거나 부족함 | `PATCH /api/books/{id}/stock`로 재고 먼저 등록 |
| 결제 준비(ready) 시 실패 | 주문이 이미 결제완료/취소된 상태 | 새로 `POST /api/orders`로 PENDING 주문을 다시 생성 |
| 결제 승인(approve) 시 카카오페이가 거부 | `pg_token`이 실제 결제 없이 임의로 넣은 값 | 반드시 6-2단계처럼 실제 `redirectUrl`에서 결제를 진행해 받은 값을 사용 |
| 카카오페이 결제 준비 자체가 실패 (`error_code:-1`) | `.env`의 `KAKAO_PAY_SECRET_KEY`가 비어있거나 잘못됨 | `developers.kakaopay.com`(카카오페이 전용 사이트)에서 발급받은 키인지 재확인 |
