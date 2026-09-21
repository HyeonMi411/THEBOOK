# BookStore v5 (`project_5`) — Django + Python 완전 재작성

`project_3`(Spring Boot 3 + Next.js)를 그대로 유지한 채, **Django + Python으로 백엔드·프론트엔드를 전부 새로 작성**한 5차 버전입니다.
여기에 첨부하신 `python_django.zip`(analytics 대시보드)을 기반으로, Spring Boot 동기화 없이 자체 DB를 직접 분석하는 **판매 통계 대시보드**를 새 기능으로 추가했습니다.

---

## 1. project_3와 달라진 점 요약

| 항목 | project_3 (Spring Boot) | project_5 (Django) |
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

## 3. project_3에서 겪었던 문제들을 처음부터 반영한 부분

실제 project_3 배포 과정에서 겪었던 이슈들을 설계 단계부터 방지했습니다.

- **도메인/프론트주소 하드코딩 금지**: `FRONTEND_BASE_URL`, `DJANGO_ALLOWED_HOSTS`, `CORS_ALLOWED_ORIGINS`를 전부 `.env`로만 주입 (카카오페이 `approval_url` 등도 이 값을 사용)
- **로그인 시 탈퇴 여부를 비밀번호 검증보다 먼저 확인** — "탈퇴한 계정입니다" 메시지가 정확히 나오도록 함
- **재고 동시성 제어**: 결제 승인(`kakao_approve`) 시점에만 `select_for_update()`로 비관적 락을 걸고 재고 차감
- **소프트 삭제**: 회원탈퇴·도서삭제 모두 실제 DELETE 대신 `deleted` 플래그만 사용 (FK 제약 위반 방지)
- **프론트가 백엔드와 같은 서버(Django Template)** 라서, project_3에서 여러 번 겪었던 `localhost:8080` 하드코딩·nginx 라우팅 누락·CORS 문제 자체가 구조적으로 적게 발생함

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

## 6. 이번 재작성에서 의도적으로 단순화한 부분 (정직하게 밝힙니다)

project_3의 모든 기능을 1:1로 완벽히 재현하기보다, **핵심 이커머스 흐름(회원가입→로그인→도서→장바구니→결제→주문)과 통계 기능이 실제로 끝까지 동작하는 것**을 우선했습니다. 아래는 구조는 마련해뒀지만 완전히 구현하지 않은 부분입니다 — 다음 작업 시 이어서 채워나가시면 됩니다.

- **OAuth2 소셜로그인(구글/카카오/네이버)**: `AppUser.provider` 필드와 모델 구조는 준비되어 있으나, 실제 OAuth2 인가 흐름(콜백 처리 등)은 미구현
- **AI 문서 챗봇(RAG)**: `OPENAI_API_KEY` 설정만 준비, 실제 PDF 업로드+질의응답 엔드포인트는 미구현
- **Redis**: 베스트셀러 캐싱은 Django 기본 `cache` 프레임워크를 사용하도록 만들어서, 배포 시 `CACHES` 설정에 Redis 백엔드를 연결하면 됩니다 (지금은 로컬 메모리 캐시로 동작)
- **이미지 파일 검증**(확장자/MIME 위변조 체크): project_3에서 만들었던 보안 로직을 아직 옮기지 않음 — 배포 전 꼭 추가 권장

## 7. 실제로 검증한 것

이 zip을 만들면서 직접 로컬에서 실행해 아래를 확인했습니다.

- `python manage.py check` — 이상 없음
- `python manage.py makemigrations && migrate` — 전체 앱 마이그레이션 정상 적용
- 실제 서버 기동 후 curl로 **회원가입 → 이메일 인증 → 로그인(JWT 발급) → 관리자 권한 승격 → 도서 등록 → 장바구니 담기 → 주문 생성**까지 전 과정이 에러 없이 동작하는 것을 확인
