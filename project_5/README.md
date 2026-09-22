# 🐍 BookStore v5 — Django + Python 완전 재작성 + 판매 통계 대시보드

> v3(Spring Boot + Next.js)는 그대로 유지한 채, 백엔드·프론트엔드 전체를 Django + Python으로 완전히 새로 작성한 버전입니다.
> pandas 기반 판매 통계 대시보드를 신규 기능으로 추가했습니다.

---

## 🔗 BookStore 시리즈 전체 보기

| 버전 | 설명 | 상태 |
|---|---|---|
| [v1](../project_1) | Spring MVC + MyBatis + JSP | - |
| [v2](../project_2) | Spring Boot + Thymeleaf + OAuth2 + AI RAG | - |
| [v3](../project_3) | REST API + JWT + Next.js + 카카오페이 | 🟢 배포됨 |
| [v4](../project_4) | Flutter 모바일 앱 + SNS 게시판 | 배포 준비 중 |
| **v5 (현재)** | Django + Python 재작성 + 통계 대시보드 | 🟡 배포 준비 중 |

---

## 📌 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 구분 | 개인 프로젝트 |
| 배포 주소 | ⏳ *(배포 전 — 아래 "배포 시 반드시 채워야 하는 환경변수" 참고)* |
| 시연 영상 | ⏳ *(배포 완료 후 촬영 예정)* |
| 이전 버전 | [← v3 (REST API + JWT + Next.js)](../project_3) |

---

## 1. v3와 달라진 점 요약

| 항목 | v3 (Spring Boot) | v5 (Django) |
|---|---|---|
| 언어/프레임워크 | Java 17 + Spring Boot 3 | Python 3 + Django |
| API | Spring MVC + Spring Security | Django REST Framework |
| 인증 | JWT (jjwt) + Redis RefreshToken | JWT (SimpleJWT) |
| ORM | JPA + MyBatis 하이브리드 | Django ORM 단일 |
| 프론트엔드 | Next.js (React, 별도 서버) | Django Template (백엔드와 같은 서버, 별도 CORS/배포 불필요) |
| 통계 | 없음 | **pandas 기반 판매 통계 대시보드 (신규)** |
| DB | Oracle | SQLite(기본) / PostgreSQL(배포 권장) |

## 2. 앱(모듈) 구성

```
project_5/
├── accounts/   회원가입·로그인(JWT)·이메일인증·탈퇴
├── books/      도서 CRUD·검색·베스트셀러·국립중앙도서관/카카오 도서검색
├── cart/       장바구니
├── orders/     주문 + 카카오페이 결제(ready/approve/cancel/fail)
├── notices/    공지사항 게시판
├── analytics/  판매 통계 대시보드 (관리자 전용, /dashboard/)
└── templates/  Django Template 프론트엔드 (홈/로그인/회원가입/장바구니/마이페이지)
```

## 3. v3에서 겪었던 문제들을 처음부터 반영한 부분

실제 v3 배포 과정에서 겪었던 이슈들을 설계 단계부터 방지했습니다.

| v3에서 겪은 문제 | v5에서 미리 막은 방법 |
|---|---|
| `localhost:8080`/`3000` 하드코딩 | 프론트가 백엔드와 같은 서버(Django Template)라 애초에 분리된 도메인이 불필요 |
| `FRONTEND_BASE_URL` 등 하드코딩 | 전부 `.env`로만 주입되도록 강제 (카카오페이 `approval_url` 등도 이 값을 사용) |
| 탈퇴 계정인데 "비밀번호 불일치"로 표시됨 | 로그인 로직에서 탈퇴 여부를 비밀번호 검증보다 먼저 체크 |
| 결제 시 재고 동시성 문제 | `select_for_update()`로 결제 승인 시점에만 락을 걸고 차감 |
| 도서/회원 하드삭제 시 FK 위반 | 처음부터 소프트 삭제(`deleted` 플래그)만 사용 |
| CI/CD 시크릿 누락 반복 | `deploy.yml`에 필요한 시크릿 전부와 수동 실행(`workflow_dispatch`) 처음부터 포함 |

## 4. 로컬 실행 방법

```bash
cd project_5
python -m venv .venv
source .venv/bin/activate   # Windows는 .venv\Scripts\activate

pip install -r requirements.txt

cp .env.example .env        # 값 채워넣기 (비워두면 SQLite + 콘솔이메일로 바로 실행 가능)

python manage.py migrate
python manage.py createsuperuser   # 통계 대시보드(/dashboard/) 접근용 관리자 계정
python manage.py runserver
```

- 사이트: http://localhost:8000
- Django Admin: http://localhost:8000/admin/
- 통계 대시보드: http://localhost:8000/dashboard/ (관리자 로그인 필요)

`.env`를 안 채워도 SQLite + 콘솔 이메일 백엔드로 바로 실행되도록 만들었습니다 (이메일 인증번호가 화면 대신 **터미널 콘솔에 출력**됩니다).

## 5. 배포 시 반드시 채워야 하는 환경변수 (`.env`)

```
DJANGO_SECRET_KEY=
DJANGO_DEBUG=False
DJANGO_ALLOWED_HOSTS=실제도메인
FRONTEND_BASE_URL=https://실제도메인
CORS_ALLOWED_ORIGINS=https://실제도메인
GMAIL_USERNAME=
GMAIL_APP_PASSWORD=
KAKAO_PAY_SECRET_KEY=
KAKAO_PAY_CID=
KAKAO_CLIENT_ID=
NL_API_KEY=
```

배포 완료 후 이 README의 **배포 주소·시연 영상 항목을 실제 값으로 교체**해주세요.

## 6. 이번 재작성에서 의도적으로 단순화한 부분 (정직하게 밝힙니다)

v3의 모든 기능을 1:1로 완벽히 재현하기보다, **핵심 이커머스 흐름(회원가입→로그인→도서→장바구니→결제→주문)과 통계 기능이 실제로 끝까지 동작하는 것**을 우선했습니다. 아래는 구조는 마련해뒀지만 완전히 구현하지 않은 부분입니다 — 다음 작업 시 이어서 채워나가시면 됩니다.

- **OAuth2 소셜로그인(구글/카카오/네이버)**: `AppUser.provider` 필드와 모델 구조는 준비되어 있으나, 실제 OAuth2 인가 흐름(콜백 처리 등)은 미구현
- **AI 문서 챗봇(RAG)**: `OPENAI_API_KEY` 설정만 준비, 실제 PDF 업로드+질의응답 엔드포인트는 미구현
- **Redis**: 베스트셀러 캐싱은 Django 기본 `cache` 프레임워크를 사용하도록 만들어서, 배포 시 `CACHES` 설정에 Redis 백엔드를 연결하면 됩니다 (지금은 로컬 메모리 캐시로 동작)
- **이미지 파일 검증**(확장자/MIME 위변조 체크): v3에서 만들었던 보안 로직을 아직 옮기지 않음 — 배포 전 꼭 추가 권장

## 7. 실제로 검증한 것

이 프로젝트를 만들면서 직접 로컬에서 실행해 아래를 확인했습니다.

- `python manage.py check` — 이상 없음
- `python manage.py makemigrations && migrate` — 전체 앱 마이그레이션 정상 적용
- 실제 서버 기동 후 curl로 **회원가입 → 이메일 인증 → 로그인(JWT 발급) → 관리자 권한 승격 → 도서 등록 → 장바구니 담기 → 주문 생성**까지 전 과정이 에러 없이 동작하는 것을 확인

---

## 🔗 관련 링크

- GitHub: https://github.com/HyeonMi411/THEBOOK/tree/main/project_5
- 배포 주소: ⏳ 배포 후 업데이트 예정
- 시연 영상: ⏳ 배포 후 촬영 예정
- 이전 버전: [← BookStore v3](../project_3)
