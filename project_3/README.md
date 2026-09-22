# 📚 BookStore v3 — 온라인 도서 쇼핑몰 (Spring Boot 3 + Next.js)

> BookStore 프로젝트의 최종 버전입니다. 백엔드/프론트엔드를 REST API로 완전히 분리하고, JWT 인증·카카오페이 실결제·Redis 캐싱·AI RAG 챗봇까지 실무 서비스 수준의 기능을 갖췄습니다.

---

## 🔗 BookStore 시리즈 전체 보기

| 버전 | 설명 | 상태 |
|---|---|---|
| [v1](../project_1) | Spring MVC + MyBatis + JSP | - |
| [v2](../project_2) | Spring Boot + Thymeleaf + OAuth2 + AI RAG | - |
| **v3 (현재)** | REST API + JWT + Next.js + 카카오페이 | 🟢 배포됨 |
| [v4](../project_4) | Flutter 모바일 앱 + SNS 게시판 | 준비 중 |
| [v5](../project_5) | Django + Python 재작성 + 통계 대시보드 | 준비 중 |

---

## 📌 프로젝트 개요 

| 항목 | 내용 |
|---|---|
| 기간 | 2026.08.12(수) ~ 2026.08.28(금) |
| 구분 | 개인 프로젝트 (팀 프로젝트 버전: `project_3-team`, 6인) *별도 확인 필요 |
| 배포 주소 | https://bookproject3.duckdns.org/books |
| 이전 버전 | [← v2 (Spring Boot + Thymeleaf)](../project_2) · [v1 (Spring MVC + MyBatis)](../project_1) |

v1 → v2 → v3로 이어지는 BookStore 시리즈의 최종 단계입니다. 데이터 접근 계층을 도메인 특성에 따라 **JPA와 MyBatis로 의도적으로 분리**했고, 개발 과정에서 발견한 보안 취약점(IDOR, 파일 업로드 검증 미비, 계정 중복 등)과 외부 API 연동 이슈(Jackson 역직렬화, 비용 통제)를 실제로 찾아 수정한 이력이 있습니다.

---

## 🛠 기술 스택

**Backend**
- Java 17, Spring Boot 3
- Spring Security + JWT (Access/Refresh Token)
- OAuth2 (구글 / 카카오 / 네이버 소셜로그인)
- Redis — RefreshToken 저장, 이메일 인증번호 관리, 베스트셀러 캐싱

**Database**
- Oracle
- JPA (Spring Data) — 회원 / 장바구니 / 주문 등 단순 CRUD 도메인
- MyBatis — 도서 검색 / 공지사항 등 동적 조건·페이징이 복잡한 도메인
- 비관적 락(`FOR UPDATE`) + 낙관적 락(`@Version`) — 재고 동시성 제어

**Frontend**
- React (Next.js)
- Redux Toolkit + Redux-Saga
- Axios (Silent Refresh — AccessToken 만료 시 자동 재발급)
- Ant Design

**외부 연동 API**
- 카카오페이 결제 (준비 → 진행 → 승인 3단계)
- OpenAI GPT — PDF 문서 기반 RAG 챗봇
- Gmail SMTP — 이메일 인증
- 카카오 도서검색 / 국립중앙도서관 오픈API

**기타**
- Swagger (OpenAPI) — API 문서화 및 테스트
- JUnit5 — 회원/도서/공지/결제 전 구간 통합테스트
- GitHub Actions — CI/CD, nginx + Let's Encrypt(HTTPS) 배포

---

## ✨ 주요 기능

1. **회원가입 / 로그인** — JWT(AccessToken/RefreshToken) 기반 인증, 구글·카카오·네이버 소셜로그인
   - 로컬·소셜 가입 공통으로 이메일 실소유 인증을 거쳐야 가입 완료
   - 닉네임 / 프로필이미지 수정 API는 본인 확인(IDOR 방지) 로직 적용
   - 회원 탈퇴는 소프트 삭제(향후 재로그인 차단)
2. **도서 관리** — 등록 / 수정 / 소프트삭제, 검색, 카테고리별 조회, 재고 관리(관리자 전용)
3. **장바구니 / 주문 / 결제** — 장바구니 담기·수정·삭제, 장바구니결제·바로구매 주문 생성, 카카오페이 결제(준비→진행→승인)
4. **공지사항** — 목록 / 상세 / 검색 / 페이징 (관리자 CRUD)
5. **베스트셀러 랭킹** — Redis 캐싱 기반 판매량 TOP 10 조회
6. **AI 문서 챗봇 (RAG)** — PDF 문서를 업로드하고 질문하면, 문서 내용을 근거로 GPT가 답변

---

## 🏗 시스템 아키텍처

```
Client (Next.js SPA)
   │  REST API
   ▼
Spring Boot 3
 ├─ Controller / Security(JWT, OAuth2)
 ├─ Service (비즈니스 로직, 락 제어)
 ├─ JPA Repository   ─┐  단순 CRUD 도메인(회원/장바구니/주문)
 ├─ MyBatis Mapper   ─┘  복잡한 검색·페이징 도메인(도서/공지사항)
 └─ LLM RAG (PDF 파싱 + GPT 질의)
        │
        ├─ Oracle DB (회원·도서·주문·재고, Soft Delete 적용)
        ├─ Redis (RefreshToken / 인증번호 / 베스트셀러 캐싱)
        └─ 외부 API (카카오페이, OAuth2, OpenAI, Gmail)
```

### ERD 핵심 구조
`APP_USER` — `BOOK`(1:1 `BOOK_STOCK`) — `CART`/`CART_ITEM` — `ORDERS`/`ORDER_ITEM` — `SBOARD2`(공지사항)

### 배포 구조
```
Client(브라우저) → HTTPS(Let's Encrypt) → nginx(리버스 프록시)
   ├─ /               → Next.js (3000)
   ├─ /auth, /api      → Spring Boot (8080)
   ├─ /oauth2/signup, /oauth2/callback → Next.js
   ├─ /swagger-ui, /v3/api-docs → Spring Boot
   └─ /uploads/        → 정적 파일(도서표지/프로필/첨부)
GitHub Actions → EC2 SSH 배포 → pm2 (backend/frontend 프로세스 관리)
```

---

## 🔀 핵심 기능 흐름

**회원가입 · 인증**
```
이메일 인증번호 발송/확인 → 회원가입 or 소셜로그인 → JWT 발급
→ API 요청(AccessToken) → 401 발생 시 Silent Refresh로 자동 재발급
```

**카카오페이 결제 (3단계)**
```
1. 결제 준비 — 재고 확인만, 차감하지 않음
2. 사용자 결제 진행 — 카카오페이 결제창
3. 결제 승인 — 이 시점에만 실제 재고 차감(비관적+낙관적 락)
```
결제 준비 시점이 아니라 **승인 시점**에 재고를 차감하는 이유는, 결제창에서 이탈하는 사용자 때문에 재고가 미리 묶이는 것을 방지하기 위해서입니다.

---

## 🐛 트러블슈팅

**개발 단계**

| 문제 | 원인 | 해결 |
|---|---|---|
| FK 제약 위반 (ORA-02292) | 도서·회원 하드 삭제 시 장바구니/주문 이력과 충돌 | Soft Delete(DELETED 플래그)로 전환해 이력 보존 |
| IDOR 취약점 | 닉네임/프로필이미지 수정 API에 본인 확인 로직 누락 | URL의 userId와 로그인 사용자 ID를 대조해 403 처리 |
| JPA 1차 캐시 불일치 | JPA·MyBatis 혼용 시 캐시가 raw SQL 변경을 인지 못함 | `entityManager.clear()` / `getReference()`로 동기화 |
| Jackson 역직렬화 오류 | OpenAI 응답 DTO를 Lombok `@Value`로 설계해 발생 | Java `record`로 전환해 Jackson 기본 지원 활용 |
| 재고 동시 차감 | 결제 승인 시 여러 사용자가 동시 요청 시 재고 정합성 깨짐 | 비관적 락(`FOR UPDATE`) + 낙관적 락(`@Version`) 이중 적용 |

**배포 단계 (EC2/CI-CD)**

| 문제 | 원인 | 해결 |
|---|---|---|
| jar 실행 실패 | GitHub Actions 배포 경로와 EC2 실제 경로(`project_3/back/...`) 불일치 | `deploy.yml`·`application.yml`의 경로를 전부 `project_3/` 기준으로 통일 |
| nginx 404 | `/oauth2/signup`, `/swagger-ui` 등 경로에 대한 location 블록 누락 | 필요한 location 블록 전부 추가 |
| 이메일 인증 실패 | Gmail 앱 비밀번호가 `deploy.yml`의 `.env` 생성 스텝에서 누락 | 필요한 시크릿(Gmail/카카오페이/OpenAI) 전부 포함하도록 수정 |
| "탈퇴한 계정" 오탐 | 소프트 삭제된 계정으로 비밀번호를 바꿔도 탈퇴 플래그 때문에 로그인 차단 | 탈퇴 여부를 먼저 확인해 정확한 에러 메시지 노출, 실제 계정 삭제 절차 수립 |
| 카카오 로그아웃/결제 리다이렉트 실패 (KOE007) | 카카오 개발자 콘솔에 배포 도메인이 Redirect URI로 미등록 | 카카오 로그인/카카오페이 콘솔 양쪽에 실제 도메인 등록 |

---

## 🚀 실행 방법

### 백엔드
```bash
# 1. Oracle DB 준비, Redis 실행
redis-server

# 2. .env 파일 생성 (JWT_SECRET, GOOGLE/KAKAO/NAVER 소셜로그인 키,
#    KAKAO_PAY_SECRET_KEY, GMAIL_USERNAME/APP_PASSWORD, OPENAI_API_KEY 등)

# 3. 실행
./gradlew bootRun
```
`http://localhost:8080/swagger-ui/index.html` 에서 전체 API 확인 가능합니다.

### 프론트엔드
```bash
npm install
npm run dev
```
`http://localhost:3000` 에서 확인 가능합니다.

### 테스트
```bash
./gradlew test      # 백엔드 (JUnit5)
npx jest             # 프론트엔드 — 11 suites / 180 tests
```

---

## 🔗 관련 링크

- GitHub: https://github.com/HyeonMi411/THEBOOK/tree/main/project_3
- **배포 주소**: https://bookproject3.duckdns.org/books
- 시연 영상 1: https://www.youtube.com/watch?v=eDxqr3u_LcY&t=9s
- 시연 영상 2: https://www.youtube.com/watch?v=eByYCYUFjTI&t=175s
- 이전 버전: [← BookStore v2](../project_2) · [BookStore v1](../project_1)
- 다음 버전: [BookStore v4 (Flutter) →](../project_4) · [BookStore v5 (Django) →](../project_5)
