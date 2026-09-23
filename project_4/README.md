# 📱 BookStore v4 — Flutter 모바일 앱 + SNS 게시판 + 판매 통계 대시보드

> v3(Spring Boot + Next.js)의 백엔드는 그대로 유지한 채, 프론트엔드를 Flutter 모바일 앱으로 재구성한 버전입니다.
> 게시글·이미지·댓글·리트윗·해시태그를 갖춘 SNS 스타일 게시판과, Django + pandas 기반 판매 통계 대시보드를 새 기능으로 추가했습니다.

---

## 🔗 BookStore 시리즈 전체 보기

| 버전 | 설명 | 상태 |
|---|---|---|
| [v1](../project_1) | Spring MVC + MyBatis + JSP | - |
| [v2](../project_2) | Spring Boot + Thymeleaf + OAuth2 + AI RAG | - |
| [v3](../project_3) | REST API + JWT + Next.js + 카카오페이 | 🟢 배포됨 |
| **v4 (현재)** | Flutter 모바일 앱 + SNS 게시판 + 통계 대시보드 | 🟡 배포 준비 중 |

---

## 📌 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 구분 | 개인 프로젝트 |
| 배포 주소 | ⏳ *(배포 전 — 아래 "배포 전 반드시 채워야 하는 것" 참고)* |
| 시연 영상 | ⏳ *(배포 완료 후 촬영 예정)* |
| 이전 버전 | [← v3 (REST API + JWT + Next.js)](../project_3) |

v3(Spring Boot + Next.js)의 백엔드는 그대로 재사용하고, **프론트엔드만 Flutter 모바일 앱으로 새로 작성**했습니다. Flutter는 클라이언트 전용 프레임워크라 서버를 대체할 수 없으므로, "재작성"의 범위를 백엔드가 아닌 클라이언트 계층으로 명확히 한정했습니다. 여기에 v3의 Oracle DB를 읽기 전용으로 직접 연결하는 Django 통계 대시보드를 별도 구성요소로 추가했습니다.

## 구성

```
project_4/
├── back/       v3(Spring Boot)의 완전한 복사본 + SNS 게시판(Post) API 신규 추가
├── mobile/     Flutter 모바일 앱 (v3의 Next.js 프론트엔드를 대체)
└── dashboard/  Django + pandas 판매 통계 대시보드 (관리자 전용, back과 같은 Oracle DB를 읽기 전용으로 직접 조회)
```

---

## 🛠 기술 스택

**Backend** (v3와 동일 + 신규 도메인)
- Java17, Spring Boot 3.3.5, Spring Security, JWT, Redis, JPA, MyBatis, Oracle
- 게시판 도메인(Post/Image/Comment/Retweet/Hashtag/PostLike) 신규 추가

**Mobile**
- Flutter, Riverpod(상태관리), Dio(HTTP 통신), flutter_secure_storage(토큰 암호화 저장), image_picker, url_launcher

**Dashboard**
- Python, Django, pandas, oracledb — Spring Boot와 같은 Oracle DB를 읽기 전용으로 직접 연결

---

## ✨ 주요 기능 (v3 화면 → Flutter 재구성)

| v3(Next.js) | v4(Flutter) |
|---|---|
| 도서 목록/상세 | 하단 탭 1번째 |
| (신규) SNS 게시판 | 하단 탭 2번째 |
| 공지사항 | 하단 탭 3번째 |
| 장바구니 + 카카오페이 결제 | 하단 탭 4번째 (결제창은 외부 브라우저로 오픈) |
| 마이페이지 + 주문내역 | 하단 탭 5번째 |
| 로그인/회원가입 | 별도 화면 |
| (신규) 판매 통계 | 별도 웹 대시보드(Django, 관리자 전용) |

---

## 🏗 back에 새로 추가한 것 (SNS 게시판)

| 파일 | 역할 |
|---|---|
| `entity/Post.java`, `Image.java`, `Comment.java`, `Retweet.java`, `Hashtag.java`, `PostLike.java` | 게시글 및 연관 데이터 |
| `service/PostService.java` | 게시글 CRUD, 본인 확인(IDOR 방지), 소프트 삭제 |
| `controller/PostController.java` | `/api/posts` — 목록/단건/작성/수정/삭제 |
| `SecurityConfig.java` | `GET /api/posts/**`는 비로그인도 조회 가능, 작성/수정/삭제는 인증 필요 |

## 🏗 dashboard 설계 — DB 직접 연결 vs API 호출

Spring Boot에 새 관리자 API를 추가하는 대신, **같은 Oracle DB를 읽기 전용으로 직접 조회**하는 방식을 택했습니다.

| | 선택한 방식(DB 직접읽기) | 대안(Spring Boot API 호출) |
|---|---|---|
| 구현 난이도 | 낮음 | 높음 (관리자 전용 API + 서비스 간 인증 필요) |
| 배포 복잡도 | 낮음 | 높음 (두 서비스가 서로 상태에 의존) |

1인 개발 포트폴리오 규모에서는 서비스 간 결합도보다 구현·배포 단순성이 더 중요하다고 판단했습니다. Spring Boot 스키마를 Django가 실수로 건드리지 않도록 `models.py`는 전부 `managed=False`로 선언했고, DB 라우터로 쓰기 자체를 코드 레벨에서 차단했습니다.

---

## 💡 담당 업무 및 성과

**Flutter 상태관리·통신 계층 설계**
Riverpod `Notifier` + Dio로 도서/장바구니/공지/주문/게시판 5개 도메인의 데이터 계층을 구성하고, v3에서 검증된 JWT 401 자동 재발급 패턴(Axios 인터셉터)을 Dio 인터셉터로 동일하게 이식했습니다.
→ Next.js와 Flutter처럼 서로 다른 프레임워크 사이에서도, 인증 흐름 같은 공통 아키텍처 패턴은 언어에 무관하게 재사용 가능함을 확인했습니다.

**SNS 게시판 도메인 통합**
게시글에 연관된 이미지·댓글·리트윗·해시태그 엔티티를 v3의 기존 보안 정책(JWT 인증, IDOR 방지, 소프트 삭제)과 충돌 없이 통합했습니다.

**서로 다른 언어/프레임워크 간 데이터 계층 연동**
Java(Spring Boot/JPA)가 소유한 스키마를, Python(Django ORM)에서 `managed=False`로 안전하게 매핑해 재사용하는 경험을 확보했습니다.

---

## 🐛 트러블슈팅

**[아키텍처] project_3 API를 그대로 재사용할지, 새로 만들지 판단**
- 문제: Flutter는 클라이언트 프레임워크라 자체적으로 서버를 만들 수 없음
- 해결: 백엔드는 v3(Spring Boot)를 복사해 재사용하고, 클라이언트(화면)만 Flutter로 새로 작성하는 것으로 방향을 명확히 함
- 배운 점: "프레임워크 전환"이 항상 전체 스택의 재작성을 의미하지는 않으며, 계층별로 적절한 기술을 선택하는 판단 기준을 마련

**[검증 한계] 실제 빌드 환경 부재**
- 문제: 개발 환경에 JDK/Flutter SDK가 없어 실제 컴파일·빌드를 사전에 확인할 수 없었음
- 해결: 중괄호 균형, 모든 import 경로의 실제 파일 존재 여부를 전수 검증하는 방식으로 최대한 보완
- 배운 점: 로컬 빌드 전 정적 검증만으로 잡아낼 수 있는 오류와, 실제 빌드/실행을 해봐야만 드러나는 오류의 차이를 명확히 인식

---

## ⚠️ 배포 전 반드시 채워야 하는 것

1. `back/src/main/resources/application-oauth.yml`의 `OAUTH2_REDIRECT_URL` — v3 도메인과 겹치지 않는 새 도메인
2. `mobile/lib/core/network/api_client.dart`의 배포 도메인 — 임시값을 실제 도메인으로 교체
3. nginx에 v4 백엔드(8082 포트), 대시보드(8083 포트)용 `location` 블록 각각 추가
4. `back/.env`, `dashboard/.env` 새로 작성 (민감정보라 저장소에 포함하지 않음)
5. GitHub Secrets에 `DASHBOARD_SECRET_KEY`, `DASHBOARD_ALLOWED_HOSTS`, `ORACLE_DSN` 추가
6. 카카오 로그인/카카오페이 콘솔에 v4 신규 도메인 등록
7. 배포 완료 후 이 README의 **배포 주소·시연 영상 항목을 실제 값으로 교체**

---

## 🚀 실행 방법

### 백엔드
```bash
cd project_4/back
# .env 파일 생성 후
./gradlew bootRun
```
`http://localhost:8080/swagger-ui/index.html` 에서 API 확인 가능 (배포 시 8082 포트)

### 모바일 앱
```bash
cd project_4/mobile
flutter pub get
flutter analyze
flutter run
```

### 통계 대시보드
```bash
cd project_4/dashboard
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env   # ORACLE_DSN 등 채우기
python manage.py migrate --database=default
python manage.py createsuperuser --database=default
python manage.py runserver
```
`http://localhost:8000/login` 에서 로그인 후 대시보드 확인 (배포 시 8083 포트)

---

## 🔗 관련 링크

- GitHub: https://github.com/HyeonMi411/THEBOOK/tree/main/project_4
- 배포 주소: ⏳ 배포 후 업데이트 예정
- 시연 영상: ⏳ 배포 후 촬영 예정
- 이전 버전: [← BookStore v3](../project_3)
