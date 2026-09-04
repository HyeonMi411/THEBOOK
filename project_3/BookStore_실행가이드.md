# BookStore 전체 프로젝트 실행 방법 (boot3 + front3 최종본)

도서/공지사항/회원가입(이메일 인증)/장바구니/주문/카카오페이 결제/국립중앙도서관 검색까지
포함된 전체 프로젝트를 처음부터 실행하는 방법입니다.

---

## 0. 사전 준비물

| 항목 | 버전/확인방법 |
|---|---|
| Java | 17 (`java -version`) |
| Node.js | 16 이상 권장 (`node -v`) |
| Oracle DB | XE 등, 리스너 1521 포트 |
| Redis | 6379 포트 (RefreshToken, 이메일 인증번호, 베스트셀러 캐싱 등에 사용) |
| Gradle | boot3 안에 `./gradlew` 포함되어 있어 별도 설치 불필요 |

---

## 1. Oracle DB 준비

1. Oracle에 `boot` 계정(스키마)을 만들어둡니다.

   > ⚠️ **DB 계정정보는 `.env`가 아니라 `application.yml`에 직접 고정되어 있습니다.**
   > ```yaml
   > username: boot   # ${DB_USERNAME} 대신 하드코딩된 값이 실제로 사용됨
   > password: react  # ${DB_PASSWORD} 대신 하드코딩된 값이 실제로 사용됨
   > ```
   > `.env`에 `DB_USERNAME`/`DB_PASSWORD`를 넣어도 **현재는 무시되고 `boot`/`react`가 그대로 사용됩니다.** Oracle에 이 계정으로 만들어두거나, 다른 계정을 쓰고 싶다면 `application.yml`의 이 두 줄을 직접 고치세요.

2. `ddl-auto` 설정에 따라 테이블(`APP_USER`, `BOOK`, `SBOARD2`, `BOOK_STOCK`, `CART`, `CART_ITEM`, `ORDERS`, `ORDER_ITEMS` 등)이 자동으로 생성됩니다. 테이블을 미리 만드실 필요는 없습니다.

   > ⚠️ **현재 `application.yml`이 `ddl-auto: create-drop`(임시 설정) 상태입니다.** 이 모드는 **서버가 켜지고 꺼질 때마다 데이터베이스 전체를 지우고 새로 만듭니다.**
   > 1) 이 상태로 서버를 **한 번 실행 → 종료**해서 최신 스키마(모든 컬럼 nullable 반영)를 생성하고,
   > 2) `application.yml`에서 `ddl-auto: update`로 **직접 수정**한 뒤,
   > 3) 다시 실행해야 그 이후부터 데이터가 보존됩니다.

3. (선택) 더미데이터를 넣고 싶으면, boot3를 **한 번 실행해서 테이블이 다 생성된 뒤(위 2번 완료 후)** 아래 순서로 SQL을 실행하세요.
   ```
   1) dummy_data_book_sboard2.sql   (도서 20권 + 공지사항 20건 + 관리자 계정)
   2) dummy_data_book_stock.sql     (그 20권에 재고 채워넣기, VERSION 컬럼 포함)
   ```
   관리자 테스트 계정: `admin_test@thejoa703.com` / `admin1234`

---

## 2. boot3 (백엔드) 실행

### 2-1. 환경변수(.env) 준비
`boot3` 폴더 최상위(= `build.gradle`이 있는 위치)에 `.env` 파일을 만들고 아래 값을 채웁니다.

```env
# JWT
JWT_SECRET=아무거나_32자_이상의_임의문자열

# 소셜로그인 (구글/카카오/네이버 개발자센터에서 각각 발급)
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
KAKAO_CLIENT_ID=
NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=

# 카카오페이 결제 (카카오페이 "전용" 개발자센터: developers.kakaopay.com)
# ⚠️ 일반 카카오 디벨로퍼스(developers.kakao.com)의 "어드민 키"가 아닙니다.
#    2024.01 API 개편 이후 어드민 키는 사용 불가하고, 반드시 카카오페이 전용
#    사이트에서 발급받은 "Secret Key(dev)"를 넣어야 합니다.
KAKAO_PAY_SECRET_KEY=
# 비워두면 카카오 공식 테스트용 cid(TC0ONETIME)가 자동으로 사용됩니다.
KAKAO_PAY_CID=

# 국립중앙도서관 도서검색 (비워두면 boot1 원본의 테스트용 키가 기본값으로 사용됩니다.
#   안 되면 국립중앙도서관 오픈API 홈페이지에서 본인 명의로 새로 발급받아 넣어주세요)
NL_API_KEY=

# 이메일 인증 (회원가입 시 인증번호 발송 - Gmail SMTP)
# 일반 Gmail 로그인 비밀번호가 아니라, 구글 계정 → 보안 → 2단계 인증 켜기 →
# 앱 비밀번호에서 별도로 발급받은 16자리 값을 "공백 없이" 넣어야 합니다.
# (구글이 화면에는 4자리씩 띄어서 보여주지만 실제 값에는 공백이 없습니다)
GMAIL_USERNAME=
GMAIL_APP_PASSWORD=

# 프론트엔드 주소 (카카오페이 결제완료/취소/실패, 소셜로그인 리다이렉트 URL을 만드는 데 사용)
FRONTEND_BASE_URL=http://localhost:3000
```

> ⚠️ `KAKAO_PAY_SECRET_KEY`가 없으면 결제준비(`/api/payments/kakao/ready`) 호출이 실패합니다.
> ⚠️ `GMAIL_USERNAME`/`GMAIL_APP_PASSWORD`가 없으면 회원가입 시 이메일 인증번호 발송(`/auth/email/send-code`)이 500 에러로 실패합니다.

### 2-2. Redis 실행
```bash
redis-server
```

### 2-3. boot3 실행
```bash
cd boot3
./gradlew bootRun
```
`http://localhost:8080` 에서 뜹니다.

### 2-4. Swagger로 API 확인
```
http://localhost:8080/swagger-ui/index.html
```
`Book Api`, `Notice(Sboard2) Api`, `Cart Api`, `Order Api`, `Payment Api`, `User Api` 그룹이 전부 보이면 정상입니다. `User Api`에서 `/auth/email/send-code`, `/auth/email/verify-code`, `/auth/{userId}/nickname`, `/auth/{userId}/profile-image`, `/auth/me`(탈퇴)도 함께 확인하세요.

---

## 3. front3 (프론트엔드) 실행

```bash
cd front3
npm install
npm run dev
```
`http://localhost:3000` 에서 뜹니다.

> front3의 `next.config.js`에 `eslint.ignoreDuringBuilds: true`가 설정되어 있어서, 스타일(ESLint) 위반이 있어도 빌드/실행 자체는 막히지 않습니다.

### 프로덕션 빌드로 확인하려면
```bash
npm run build
npm start
```

---

## 4. 전체 기능 흐름 한눈에 보기

| 화면 | 경로 | 비고 |
|---|---|---|
| 회원가입 | `/signup` | **이메일 인증번호 확인을 완료해야 가입 버튼이 활성화**됩니다 |
| 로그인 | `/login` | 일반 로그인 + 소셜로그인(구글/카카오/네이버) |
| 소셜 가입확인 | `/oauth2/signup` | 신규 소셜회원만 거치는 닉네임 확인 화면 |
| 도서 목록/상세 | `/books`, `/books/[id]` | 재고배지, 장바구니담기/바로구매 버튼 |
| 도서 등록/수정 | `/books/new` | **관리자 전용** |
| 국립중앙도서관 검색 | `/books/national-library` | 전체공개, 저장은 관리자 전용 |
| 공지사항 | `/notices`, `/notices/[id]` | 상세조회시 조회수 자동 증가 |
| 공지사항 작성 | `/notices/new` | **관리자 전용** |
| 장바구니 | `/cart` | 로그인 회원 전체 |
| 주문/결제확인 | `/order/checkout` | 카카오페이 결제요청 |
| 결제완료/취소/실패 | `/payment/complete`, `/payment/cancel`, `/payment/fail` | 카카오 리다이렉트 처리 |
| 내 주문내역 | `/mypage/orders`, `/mypage/orders/[id]` | |
| 마이페이지 | `/mypage` | 닉네임/프로필이미지 수정(본인만 가능), **회원 탈퇴**(확인창 후 소프트삭제) |

**결제 전체 흐름**: 회원가입(이메일인증) 또는 로그인 → 도서상세(구매버튼) → 장바구니 → 주문확인 → 카카오페이 결제창 → 결제완료(재고 실제 차감) → 주문내역 확인
(자세한 Swagger 테스트 절차는 별도로 드린 `BookStore_결제기능_Swagger테스트가이드.md` 참고)

---

## 5. 테스트 실행 방법

### 백엔드 (JUnit)
```bash
cd boot3
./gradlew test
```
주요 테스트 파일 (`Boot2ApplicationTests_1_Entity.java`는 게시판 기능 제거 시 함께 삭제되어 현재는 없습니다):
- `Boot2ApplicationTests_2_Service.java` ~ `_6_PaymentService.java` (회원/도서/공지/결제 전 구간)
- `Boot3ApplicationTests.java` (스프링 컨텍스트 로딩 여부만 확인하는 기본 스모크 테스트 — 없어도 다른 테스트들이 이미 커버하므로 삭제해도 무방)

### 프론트엔드 (Jest)
```bash
cd front3
npx jest
```
현재 **11 suites / 180 tests**가 전부 통과해야 정상입니다 (auth, book, notice, cart, order 전체 reducer+saga 커버 — 게시판 기능 제거로 post 관련 2 suite가 빠졌습니다).

---

## 6. 자주 막히는 부분 (Troubleshooting)

| 증상 | 원인 / 해결 |
|---|---|
| `KAKAO_PAY_SECRET_KEY` 없이 결제준비 호출시 실패 | 미발급/오타. **일반 카카오 디벨로퍼스가 아니라 카카오페이 전용 사이트**(developers.kakaopay.com)에서 발급 |
| 회원가입 시 "인증번호 발송에 실패했습니다" | 백엔드 콘솔에서 실제 원인 확인 필요. 가장 흔한 원인: `.env`의 `GMAIL_APP_PASSWORD`에 공백이 섞여 들어감, 또는 `.env` 수정 후 서버 재시작을 안 함, 또는 2단계 인증이 실제로는 꺼져있음 |
| 로그인/DB 접속 실패 | `.env`의 `DB_USERNAME`/`DB_PASSWORD`는 무시됩니다. `application.yml`에 하드코딩된 `boot`/`react` 계정이 Oracle에 실제로 있는지 확인 |
| 서버 재시작마다 데이터가 전부 초기화됨 | `ddl-auto: create-drop` 상태 그대로 쓰고 있는 것. 1번 항목의 안내대로 `update`로 전환 필요 |
| Swagger에서 `pg_token` 테스트가 안 됨 | 실제 결제창을 한 번 거쳐야 나오는 값입니다. 별도 가이드 문서 참고 |
| 재고 0인 도서로 장바구니/주문 시도시 거부됨 | 정상 동작입니다. `PATCH /api/books/{id}/stock`(관리자 전용)으로 재고를 먼저 채워주세요 |
| 다른 사람의 닉네임/프로필사진을 고치려 하면 403 | 정상 동작입니다(본인 확인 로직). URL의 `userId`가 로그인한 본인과 다르면 항상 차단됩니다 |
| 탈퇴한 계정으로 로그인 시도 시 거부됨 | 정상 동작입니다. 회원 탈퇴는 소프트 삭제라 계정 자체는 DB에 남지만, 로그인은 막힙니다 |
