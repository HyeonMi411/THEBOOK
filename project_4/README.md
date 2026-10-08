# 📚 BookStore v4 — Flutter 모바일 앱 + Spring Boot API + Django 운영 대시보드

> BookStore 시리즈의 네 번째 버전입니다. v3 웹 쇼핑몰을 **Flutter 모바일 앱**으로 재설계하고, SNS 커뮤니티·외부 API 연동·상담 챗봇·운영 대시보드를 더했습니다.

| 항목 | 내용 |
|---|---|
| 기간 | 2026.09.11 ~ 2026.09.23 |
| 구분 | 개인 프로젝트 (기획·백엔드·앱·대시보드 1인 개발) |
| 시연 영상 | [앱 1편](https://www.youtube.com/watch?v=pUG-v9Hbqv8) · [앱 2편](https://www.youtube.com/watch?v=EuUjMA2sNWk) · [운영 대시보드](https://www.youtube.com/watch?v=K4Q3YSEAb-0) |
| 이전 버전 | [← v3 (REST API + JWT + Next.js)](../project_3) |
| 배포 | 로컬 실행 기준 (서버 비용과 운영 중인 v3 보호를 위해 미배포, 배포 워크플로 `deploy-project4.yml`은 수동 실행용으로 준비) |

| 구성 | 기술 | 포트 |
|---|---|---|
| `back/` API 서버 | Spring Boot 3.3 · JPA + MyBatis · Oracle · Redis · JWT · OAuth2 | 8082 |
| `mobile/` 앱 | Flutter · Riverpod 3 · Dio · flutter_map · webview_flutter · app_links | - |
| `dashboard/` 운영 대시보드 | Django · pandas · Chart.js | 8000 |

---

## ✨ 주요 기능

**쇼핑** — 도서 목록(무한 스크롤), 0.3초 디바운스 검색 자동완성, 베스트셀러 TOP 10(Redis 캐시), 장바구니 선택 주문, 우편번호 검색, 카카오페이 결제, 결제 완료 시 재고 차감·이메일 영수증, 주문내역

**외부 도서 검색** — 카카오 도서·국립중앙도서관 통합검색(관리자는 바로 쇼핑몰 등록), KDC 분류 찾아보기

**커뮤니티** — 이미지 게시글, 해시태그 필터, 좋아요, 댓글, 리트윗, 팔로우 / 팔로잉 피드, 사용자 프로필

**생활 연계** — 현재 위치 기반 날씨와 독서 추천 문구(기상청, 실패 시 Open-Meteo), 매장 지도(OpenStreetMap + 카카오맵 길찾기)

**회원** — 이메일 인증 가입(중복 검사), 로그인 유지와 토큰 자동 재발급, 카카오·네이버·구글 소셜 로그인(딥링크 `bookstore4://` 복귀), 프로필 변경, 탈퇴

**상담 챗봇** — 버튼형 FAQ → 키워드 검색 → (선택) ChatGPT 보조 답변 → 상담원 메일 연결. 모든 질문을 기록해 대시보드에서 미응답 질문을 확인하고 FAQ를 보강

**관리자** — 도서 등록·수정·재고 변경·삭제, 공지사항 관리, 앱 내 **운영 통계** 화면 (`ROLE_ADMIN`에만 표시)

**운영 대시보드** — 판매(카테고리·일자별 매출), 커뮤니티(활동 추이·인기 해시태그), 챗봇(응답률·미응답 질문), 운영 지표 추이. Spring Boot가 하루 지표를 Django로 동기화하고, 앱의 운영 통계는 Spring Boot가 관리자 권한을 확인한 뒤 Django 집계를 중계

---

## 🏗 아키텍처

```
Flutter 앱 ──REST(JWT)──▶ Spring Boot API ──▶ Oracle (업무 데이터, Soft Delete)
                               │                 Redis (베스트셀러 캐시 · Refresh Token · 인증번호)
                               │                 외부 API (카카오페이 · 카카오 · 국립중앙도서관 · 기상청 · OpenAI)
                               └──관리자 확인 후 중계──▶ Django + pandas ──읽기 전용──▶ Oracle
```

---

## 🐛 트러블슈팅

| 문제 | 원인 | 해결 |
|---|---|---|
| 앱에서 약 15분마다 로그인 해제 | 모바일 HTTP 클라이언트는 Refresh Token 쿠키를 자동 저장하지 않음 | 앱 저장소에 토큰 보관, Dio 인터셉터가 401 시 한 번만 재발급 후 원래 요청 재시도 |
| 토큰 만료 시 로그인 페이지로 리다이렉트 | Security 기본 동작 | 인증 실패 시 401 응답으로 변경 |
| 카카오·국립중앙도서관 한글 검색 400·502 | 쿼리 URL 인코딩 누락 | `UriComponentsBuilder … .encode()` 적용 |
| 네이버 책 검색 404 | 직접 호출로 확인 결과 API 서비스 종료 | 앱에서 해당 탭 제거 |
| 베스트셀러 500 | Redis 직렬화 설정 문제 | 직렬화 설정 수정, 깨진 캐시는 지우고 DB로 재집계 |
| Spring → Django 동기화 400 | 청크 전송을 Django 개발 서버가 처리 못함 | JSON 바이트 + Content-Length 명시 |
| 에뮬레이터 소셜 로그인 실패 | OAuth 제공자가 사설 IP Redirect 불허 | `adb reverse` + localhost Redirect URI 등록 |
| 결제 앱이 열리지 않음 | Android 11 패키지 가시성 제한 | 매니페스트 `queries` 선언 |
| 글쓴이 위조 가능 | 작성자를 요청 값에서 받음 | 로그인 토큰에서 작성자 추출 |

---

## 🚀 실행 방법

각 단계는 `project_4/` 폴더 기준, **별도 터미널**에서 실행합니다.

```bash
# 1) 백엔드 (Oracle, Redis 실행 후)
cd back && cp .env.example .env    # 값 채우기 (JWT_SECRET은 필수, 외부 API 키는 비워도 실행됨)
./gradlew bootRun                   # http://localhost:8082/swagger-ui/index.html

# 2) 대시보드
cd dashboard && cp .env.example .env
pip install -r requirements.txt
python manage.py migrate --database=default
python manage.py createsuperuser --database=default   # 대시보드 로그인 계정
python manage.py runserver          # http://localhost:8000

# 3) 앱 (에뮬레이터)
cd mobile && flutter pub get
adb reverse tcp:8082 tcp:8082
flutter run --dart-define=API_BASE_URL=http://localhost:8082
```

---

## 📡 주요 API (v4 신규)

| 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/util/weather?lat&lon` | 위치 기반 날씨 | - |
| GET | `/api/util/stores` | 매장 목록 | - |
| GET | `/api/books/external/search?source=kakao\|nl&keyword` | 외부 도서 통합검색 | - |
| POST | `/api/books/external/import` | 외부 도서 등록 | ADMIN |
| GET | `/api/posts?feed=following&tag=` | 팔로잉 피드 · 해시태그 필터 | 로그인 |
| POST | `/api/posts/{id}/retweet` · `/api/users/{id}/follow` | 리트윗 · 팔로우 토글 | 로그인 |
| GET | `/api/admin/stats` | 운영 통계 (Django 중계) | ADMIN |
| POST | `/auth/app/exchange` | 소셜 로그인 일회용 코드 → 토큰 | - |

---

## 🔗 관련 링크

- GitHub: https://github.com/HyeonMi411/THEBOOK/tree/main/project_4
- 시연 영상: https://www.youtube.com/watch?v=pUG-v9Hbqv8 · https://www.youtube.com/watch?v=EuUjMA2sNWk · https://www.youtube.com/watch?v=K4Q3YSEAb-0
- 이전 버전: [← BookStore v3](../project_3)
