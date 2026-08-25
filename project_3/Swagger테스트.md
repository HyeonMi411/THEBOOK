# BookStore 전체 Swagger 테스트 가이드

`http://localhost:8080/swagger-ui/index.html` 에서 전체 API(회원/도서/공지사항/
국립중앙도서관/장바구니/주문/카카오페이 결제)를 순서대로 테스트하는 방법입니다.

---

## 0. API 그룹 한눈에 보기

| 그룹 | 인증 필요 여부 |
|---|---|
| User Api | 회원가입/로그인은 불필요, 그 외는 필요 |
| Book Api | 조회는 불필요, 등록/수정/삭제/재고수정은 **ROLE_ADMIN** |
| Notice(Sboard2) Api | 조회는 불필요, 글쓰기/수정/삭제는 **ROLE_ADMIN** |
| Cart Api / Order Api / Payment Api | **로그인만 하면 누구나** (관리자 제한 없음) |
| Poist Api | 게시판(찜/좋아요 없는 기본 글) |

---

## 1단계 — 회원가입 & 로그인 (User Api)

### 1-1. (필요시) 회원가입
```
POST /auth/signup
```
이미 더미데이터(`admin_test@thejoa703.com` / `admin1234`)를 넣어두셨다면 생략 가능합니다.

### 1-2. 로그인해서 JWT 발급받기
```
POST /auth/login
```
```json
{ "email": "admin_test@thejoa703.com", "password": "admin1234" }
```
응답의 `accessToken`을 복사합니다.

### 1-3. Swagger에 토큰 등록 (Authorize)
Swagger UI 우상단 **자물쇠(Authorize)** 버튼 클릭 → 복사한 토큰 붙여넣기 → Authorize.
이제부터 모든 API 호출에 이 토큰이 자동으로 실립니다.

> 관리자 전용 기능(도서등록, 공지작성, 재고수정)을 테스트하려면 **관리자 계정으로**
> 로그인해야 합니다. 일반회원 전용 흐름(장바구니/주문/결제)을 보고 싶으면 일반회원
> 계정으로 다시 로그인해서 토큰을 교체하시면 됩니다.

### 1-4. 내 정보 확인
```
GET /auth/me
```
`role`이 `ROLE_ADMIN`인지 `ROLE_USER`인지 여기서 확인 가능합니다.

---

## 2단계 — 도서 (Book Api)

### 2-1. 목록/상세/검색 (인증 불필요)
```
GET /api/books?page=1&size=12
GET /api/books/{id}
GET /api/books/search?keyword=자바
```

### 2-2. 도서 등록 (★관리자 전용)
```
POST /api/books
```
`multipart/form-data`로 도서 정보 + 표지이미지(선택) 전송.

### 2-3. 재고 등록/수정 (★관리자 전용) — 결제 테스트 전에 필수!
```
PATCH /api/books/{id}/stock
```
```json
{ "stockQuantity": 20 }
```
> ⚠️ 재고가 0이면 장바구니 담기/주문 자체가 거부됩니다. 결제 관련 테스트를
> 하시려면 **이 단계를 먼저 반드시 실행**해주세요. (더미데이터 SQL을 이미
> 돌리셨다면 생략 가능)

### 2-4. 도서 수정/삭제 (★관리자 전용)
```
PATCH /api/books/{id}
DELETE /api/books/{id}
```

### 2-5. 카카오 도서검색 자동등록 (★관리자 전용)
```
POST /api/books/kakao-insert?search=자바
```

### 2-6. 국립중앙도서관 검색/저장
```
GET /api/books/national-library/search?keyword=인공지능&page=1   (인증 불필요)
POST /api/books/national-library/save                            (★관리자 전용)
```

---

## 3단계 — 공지사항 (Notice(Sboard2) Api)

```
GET /api/notices?page=1&size=12          (인증 불필요)
GET /api/notices/{id}                     (인증 불필요, ★조회수 자동 +1)
GET /api/notices/search?keyword=공지      (인증 불필요)
POST /api/notices                         (★관리자 전용)
PATCH /api/notices/{id}                   (★관리자 전용)
DELETE /api/notices/{id}                  (★관리자 전용)
```

---

## 4단계 — 장바구니 (Cart Api, 로그인 회원 전체)

> 이 단계부터는 **일반회원 계정으로 다시 로그인**하셔도 되고, 관리자 계정 그대로
> 진행하셔도 됩니다 (장바구니/주문/결제는 권한 제한이 없습니다).

### 4-1. 담기
```
POST /api/cart
```
```json
{ "bookId": 2단계에서_확인한_ID, "quantity": 2 }
```
응답의 `items[0].id`(cartItemId)를 기억해둡니다.

### 4-2. 조회
```
GET /api/cart
```

### 4-3. 수량수정
```
PATCH /api/cart/{itemId}
```
```json
{ "quantity": 3 }
```

### 4-4. 항목삭제 / 전체비우기
```
DELETE /api/cart/{itemId}
DELETE /api/cart
```

---

## 5단계 — 주문 (Order Api)

### 5-1. 주문 생성
```
POST /api/orders
```
**장바구니 결제**면:
```json
{ "cartItemIds": [4-1에서 담은 cartItemId] }
```
**바로구매**면:
```json
{ "bookId": 도서ID, "quantity": 1 }
```
(둘 중 하나만 채우기) 응답의 `id`(orderId)를 기억해둡니다. `orderStatus`가
`PENDING`이면 정상입니다.

### 5-2. 내 주문내역 조회
```
GET /api/orders?page=1&size=12
GET /api/orders/{id}
```

---

## 6단계 — 카카오페이 결제 (Payment Api)

### 6-1. 결제준비
```
POST /api/payments/kakao/ready
```
```json
{ "orderId": 5-1에서 받은 orderId }
```
응답의 `redirectUrl`을 복사합니다.

### 6-2. 실제 결제창에서 결제 진행 (브라우저)
복사한 `redirectUrl`을 **브라우저 주소창에 직접 열면** 카카오페이 테스트
결제창(`cid=TC0ONETIME`)이 뜹니다. 카카오 계정으로 로그인 후 테스트 결제 진행
(실제 돈은 안 나감).

결제가 끝나면 다음 주소로 리다이렉트됩니다:
```
http://localhost:3000/payment/complete?orderId=X&pg_token=T5279...(긴 문자열)
```
- front3가 떠있으면 자동으로 승인 처리까지 됩니다.
- Swagger에서 직접 테스트하려면 front3는 잠깐 꺼두고, `pg_token=` 뒤 값만 복사합니다.

> **pg_token이란?** 카카오페이가 "결제를 실제로 마쳤다"는 걸 증명하는 1회용
> 토큰입니다. 실제 결제창을 거쳐야만 발급되므로 Swagger에서 임의로 만들어 넣을
> 수 없습니다.

### 6-3. 결제승인
```
POST /api/payments/kakao/approve
```
```json
{ "orderId": 그 orderId, "pgToken": 방금 복사한 값 }
```
`orderStatus`가 `PAID`로 바뀌면 성공입니다. **이 시점에 실제로 재고가
차감됩니다** → `GET /api/books/{bookId}`로 `stockQuantity`가 줄었는지 확인.

### 6-4. 결제 취소/실패 (선택)
```
POST /api/payments/kakao/cancel/{orderId}   (body 없음)
POST /api/payments/kakao/fail/{orderId}     (body 없음)
```

---

## 7단계 — 결과 최종 확인

```
GET /api/orders/{id}
```
`orderStatus=PAID`, 구매도서 목록, 결제금액이 정확히 나오는지 확인하면
전체 흐름 테스트가 끝난 것입니다.

---

## 전체 흐름 요약도

```
로그인 → [Book] 등록/재고설정 → [Cart] 담기 → [Order] 주문생성(PENDING)
→ [Payment] 결제준비 → 실제 결제창(브라우저) → [Payment] 결제승인(PAID, 재고차감)
→ [Order] 주문내역 확인
```

---

## 자주 막히는 부분

| 증상 | 원인 / 해결 |
|---|---|
| 장바구니/주문 시 "재고가 부족합니다" | `PATCH /api/books/{id}/stock`로 재고부터 채우기 (2-3단계) |
| 도서등록/공지작성/재고수정에서 403 | 관리자 계정(`ROLE_ADMIN`)으로 로그인했는지 확인 (`GET /auth/me`) |
| 결제준비(`ready`)에서 401/실패 | `.env`의 `KAKAO_PAY_ADMIN_KEY` 미설정 |
| `approve`에서 `pg_token`을 뭘 넣어야 할지 모르겠음 | 6-1~6-2단계처럼 실제 결제창을 한 번 거쳐야 함, 임의값 불가 |
| 결제완료 페이지가 자동으로 처리해버려서 Swagger로 테스트가 안 됨 | front3 개발서버를 잠시 꺼두고 진행 |
| 남의 장바구니/주문에 접근시 거부 | 정상 동작입니다. 본인 것만 조회/수정 가능하도록 설계되어 있음 |

---

## 참고: 빠르게 반복 테스트하려면

매번 브라우저로 실제 결제창을 거치기 번거로우시면, `Boot2ApplicationTests_6_PaymentService.java`
(JUnit 테스트)를 실행하시는 걸 추천합니다. 카카오페이 API를 가짜(`@MockBean`)로
대체해서 `pg_token` 없이도 결제승인→재고차감 로직을 자동으로 검증합니다.
- **Swagger**: 실제 결제창까지 눈으로 직접 확인하는 용도
- **JUnit 테스트**: 비즈니스 로직이 맞는지 반복 검증하는 용도
