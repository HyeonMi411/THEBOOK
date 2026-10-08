# project_4 — 운영 대시보드 (Django + pandas)

`project_4/back`(Spring Boot)이 쓰는 Oracle DB를 **읽기 전용**으로 연결해 판매·커뮤니티·챗봇 지표를 pandas로 집계하고, Spring Boot가 매일 보내는 운영 지표 추이까지 Chart.js로 시각화하는 관리자 전용 대시보드입니다.

## 왜 이런 구조인가 (DB 직접 읽기 + 최소한의 API)

집계용 원본 데이터는 **같은 Oracle DB를 읽기 전용으로 직접 조회**하고, 꼭 필요한 두 곳에만 토큰(`X-Sync-Token`) 기반 API를 두는 혼합 구조로 정리했습니다.

| 용도 | 방식 |
|---|---|
| 판매·커뮤니티·챗봇 집계 | Oracle 직접 조회 (읽기 전용, 실시간) |
| 일일 운영 지표 저장 | Spring Boot가 매일 23:50(`@Scheduled`)에 Django `api/statistics/`로 전송 |
| 앱 관리자 화면 통계 | Spring Boot `/api/admin/stats`가 Django `api/stats/`를 중계 |

1인 개발 포트폴리오 규모에서는 모든 데이터를 API로 주고받는 것보다 구현·배포 단순성이 더 중요하다고 판단했고, 서비스 간 통신이 꼭 필요한 부분에만 API를 두었습니다.

## 안전장치 — Spring Boot 스키마를 Django가 절대 건드리지 않도록

- `analytics/models.py`: 전부 `managed = False`로 선언 — Django가 이 테이블에 대해 마이그레이션(생성/변경/삭제)을 시도하지 않습니다.
- `config/db_router.py`: `analytics` 앱 모델에 대한 **쓰기 자체를 코드 레벨에서 차단**합니다. 실수로 `.save()`를 호출해도 `RuntimeError`가 발생합니다.
- Django 자체 관리자 계정(로그인용 `auth_user` 등)과 일일 운영 지표(`opslog.ServiceLog`)는 **별도의 로컬 SQLite**(`dashboard_local.sqlite3`)에 저장됩니다 — Oracle에는 어떤 새 테이블도 생기지 않습니다.

## 구성

```
project_4/dashboard/
├── config/
│   ├── settings.py      # DB 2개(default=sqlite, oracle=읽기전용) 분리 설정
│   ├── db_router.py      # analytics 앱을 oracle로 강제 라우팅 + 쓰기 차단
│   └── urls.py
├── analytics/
│   ├── models.py          # BOOK·ORDERS·ORDER_ITEMS·APP_USER·POSTS·POST_LIKES·COMMENTS·FOLLOWS·CHATBOT_LOGS (managed=False)
│   ├── analysis.py        # pandas 집계 함수
│   ├── views.py            # 대시보드 화면(login_required) + 동기화/통계 API
│   ├── urls.py
│   ├── tests.py
│   └── templates/
├── opslog/                # 일일 운영 지표(ServiceLog) — 로컬 SQLite
└── templates/registration/login.html   # Django 자체 로그인 (Spring Boot JWT와 무관)
```

## 로컬 실행

```bash
cd project_4/dashboard
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

cp .env.example .env
# ORACLE_DSN/ORACLE_USER/ORACLE_PASSWORD를 project_4/back/.env와 같은 값으로 채우기

python manage.py migrate --database=default   # 로컬 관리자 계정용 sqlite만 생성
python manage.py createsuperuser --database=default

python manage.py runserver
```

- http://localhost:8000/login 에서 방금 만든 관리자 계정으로 로그인
- 로그인하면 바로 대시보드(`/`)로 이동

## 배포

- 현재 **미배포**(로컬 실행 기준)입니다. `.github/workflows/deploy-project4.yml`(수동 실행 전용)에 `dashboard` job으로 준비해 두었고, project_3(8080), project_4 백엔드(8082)와 겹치지 않게 **8083 포트**로 분리했습니다.
- GitHub Secrets에 `DASHBOARD_SECRET_KEY`, `DASHBOARD_ALLOWED_HOSTS`, `ORACLE_DSN` 추가 필요 (DB 계정은 `DB_USERNAME`/`DB_PASSWORD`를 project_4/back과 그대로 공유합니다).
- nginx에 `location` 블록 추가 필요 (예시):
  ```nginx
  location /dashboard/ {
      proxy_pass http://localhost:8083/;
      proxy_set_header Host $host;
  }
  ```

## 테스트

```bash
python manage.py test analytics
```

Oracle 없이 실행되는 테스트 10개 — pandas 집계 함수, 동기화 API 토큰 검증·같은 날짜 데이터 갱신, Oracle 연결 실패 시 오류 대신 안내 문구 표시.
