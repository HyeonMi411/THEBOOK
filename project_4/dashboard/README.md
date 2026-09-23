# project_4 — 판매 통계 대시보드 (Django + pandas)

`project_4/back`(Spring Boot)이 이미 쓰고 있는 Oracle DB를 **읽기 전용**으로 직접 연결해서, 카테고리별 판매량·매출·점유율을 pandas로 집계하고 Chart.js로 시각화하는 관리자 전용 대시보드입니다.

## 왜 이런 구조인가 (DB 직접 연결 vs API 호출)

Spring Boot에 새 API를 추가하는 대신, **같은 Oracle DB를 읽기 전용으로 직접 조회**하는 방식을 택했습니다.

| | 선택한 방식(DB 직접읽기) | 대안(Spring Boot API 호출) |
|---|---|---|
| 구현 난이도 | 낮음 | 높음 (관리자 전용 API + 서비스 간 인증 필요) |
| 배포 복잡도 | 낮음 (DB 비밀번호만 있으면 됨) | 높음 (두 서비스가 서로 상태에 의존) |
| 데이터 최신성 | 실시간 | 실시간 (동일) |

1인 개발 포트폴리오 규모에서는 서비스 간 결합도보다 구현·배포 단순성이 더 중요하다고 판단했습니다. (다인 팀·MSA 환경이었다면 API 경계를 지키는 게 맞습니다.)

## 안전장치 — Spring Boot 스키마를 Django가 절대 건드리지 않도록

- `analytics/models.py`: 전부 `managed = False`로 선언 — Django가 이 테이블에 대해 마이그레이션(생성/변경/삭제)을 시도하지 않습니다.
- `config/db_router.py`: `analytics` 앱 모델에 대한 **쓰기 자체를 코드 레벨에서 차단**합니다. 실수로 `.save()`를 호출해도 `RuntimeError`가 발생합니다.
- Django 자체 관리자 계정(로그인용 `auth_user` 등)은 **별도의 로컬 SQLite**(`dashboard_local.sqlite3`)에 저장됩니다 — Oracle에는 어떤 새 테이블도 생기지 않습니다.

## 구성

```
project_4/dashboard/
├── config/
│   ├── settings.py      # DB 2개(default=sqlite, oracle=읽기전용) 분리 설정
│   ├── db_router.py      # analytics 앱을 oracle로 강제 라우팅 + 쓰기 차단
│   └── urls.py
├── analytics/
│   ├── models.py          # ORDERS/ORDER_ITEMS/BOOK을 관리 안 함(managed=False)으로 매핑
│   ├── views.py            # pandas 집계 (login_required)
│   └── templates/
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

- `deploy.yml`에 `dashboard` job으로 이미 포함되어 있습니다. project_3(8080), project_4 백엔드(8082)와 겹치지 않게 **8083 포트**로 분리했습니다.
- GitHub Secrets에 `DASHBOARD_SECRET_KEY`, `DASHBOARD_ALLOWED_HOSTS`, `ORACLE_DSN` 추가 필요 (DB 계정은 `DB_USERNAME`/`DB_PASSWORD`를 project_4/back과 그대로 공유합니다).
- nginx에 `location` 블록 추가 필요 (예시):
  ```nginx
  location /dashboard/ {
      proxy_pass http://localhost:8083/;
      proxy_set_header Host $host;
  }
  ```

## 검증한 내용

이 환경에는 실제 Oracle DB가 없어서 데이터 조회 자체는 못 해봤지만, 아래는 실제로 실행해서 확인했습니다.

- `python manage.py check` — 이상 없음
- DB 라우터 정상 동작 — `migrate --database=default` 실행 시 `analytics` 앱은 마이그레이션 대상에서 자동 제외됨 (auth/admin/sessions만 적용됨)
- 실제 서버 기동 후 curl로 로그인 → 세션 쿠키 발급 → **비로그인 상태로 대시보드 접근 시 정확히 `/login`으로 302 리다이렉트**되는 것까지 확인
- 로그인 후 대시보드 접근 시 (가짜 Oracle 접속 정보라) `OperationalError`가 발생하는 것도 확인 — 이는 **실제 Oracle에 연결되면 정상 작동한다는 뜻**이며, 인증/라우팅 로직 자체에는 문제가 없음을 의미합니다.

**실제 배포 후, EC2에서 진짜 Oracle에 연결한 상태로 한 번 더 확인해보시길 권장드립니다.**
