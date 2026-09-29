# 📚 BookStore v4 — Flutter 모바일 앱 + Spring Boot API + Django 대시보드

project_3(Next.js 웹 쇼핑몰)를 **Flutter 모바일 앱**으로 옮기고, SNS 커뮤니티·외부 API 연동·상담 챗봇·운영 대시보드를 더한 4차 프로젝트입니다.

| 구성 | 기술 | 포트 |
|---|---|---|
| `back/` API 서버 | Spring Boot 3.3 · JPA + MyBatis · Oracle · Redis · JWT · OAuth2 | 8082 |
| `mobile/` 앱 | Flutter · Riverpod 3 · Dio · flutter_map · webview_flutter · app_links | - |
| `dashboard/` 운영 대시보드 | Django 6 · pandas · Chart.js | 8083 (로컬 8000) |

## 주요 기능

**쇼핑** — 도서 목록(무한 스크롤)·**실시간 검색 자동완성**·상세·베스트셀러 TOP 10(Redis 캐시), 장바구니(체크박스 선택 주문), 주문서(다음 우편번호 검색), 카카오페이 결제, 결제완료 **이메일 영수증 + 문자(SMS)**, 주문내역

**도서 검색 확장** — 카카오·네이버·국립중앙도서관 **통합검색**(관리자는 바로 쇼핑몰 등록), 국립중앙도서관 **KDC 분류 찾아보기**, **책 표지 사진 검색**(CLOVA OCR)

**관리자** — 도서 등록·수정·재고 변경·삭제, 공지사항 작성·수정·삭제 (앱에서 `ROLE_ADMIN` 계정에만 표시)

**커뮤니티** — 이미지 5장 게시글, 해시태그 필터, 좋아요, 댓글, **리트윗**, **팔로우 / 팔로잉 피드**, 사용자 프로필, 좋아요한 글

**편의 기능** — 홈 **날씨**(기상청 초단기실황, 현재 위치 → 격자 변환), **매장 지도**(OpenStreetMap + 가까운 순 + 카카오맵 길찾기), **오늘의 책 소식**(jsoup RSS 크롤링, 30분 주기 스케줄러), 공지사항

**회원** — 이메일 인증 회원가입(프로필 사진 선택), 로그인 유지(앱 재시작 시 복원·토큰 자동 재발급), **카카오·네이버·구글 소셜 로그인**(브라우저 → 딥링크 `bookstore4://` 복귀), 프로필 사진·닉네임 변경, 탈퇴

**상담 챗봇** (PDF RAG 챗봇 대체) — 실제 쇼핑몰·은행 앱에서 가장 많이 쓰는 하이브리드 방식
1. 버튼형 FAQ 메뉴 → 2. 입력 문장 키워드 검색 → 3. 못 찾으면 **ChatGPT 보조 답변**(FAQ 를 근거자료로만, IP당 시간 제한) → 4. 상담원 이메일 연결
모든 질문은 로그로 남겨 대시보드에서 "답 못한 질문"을 보고 FAQ 를 보강합니다.

**운영 대시보드** — ① 판매(카테고리·일자별 매출) ② 커뮤니티(활동 추이·인기 해시태그) ③ 챗봇(응답률·많이 찾은 질문·미응답 질문) ④ 운영 지표 추이(Spring Boot 가 매일 23:50 푸시, "지금 동기화" 버튼)

## 수업 자료 반영

| 수업 | 반영 위치 |
|---|---|
| boot1 (DAY065 API: ChatGPT·날씨·메일·우편번호·지도·모바일 문자·카카오페이·네이버 book·크롤링, + OCR·카카오/국립중앙도서관) | `back/.../api/*`, `UtilApiController`, `OrderNotificationService`, `static/postcode.html` |
| track011 flutter (Feature-first 구조, Follow·Retweet, Spring→Django 통계 동기화) | `mobile/lib/features/*`, `entity/Follow`, `StatisticsSyncService` |
| track010 python+django (pandas 대시보드, ServiceLog) | `dashboard/analytics`, `dashboard/opslog` |

boot1 코드에서 고친 점: 네이버 책 검색이 `setTitle()` 을 세 번 호출해 저자·표지가 제목을 덮어쓰던 버그, 날씨가 "오늘 06시·서울 고정"이던 것(→ 위치별 격자 + 최신 발표시각), CoolSMS 구형 SDK 대신 공식 REST API(HMAC 서명), 코드에 박혀 있던 API 키 제거.

## 실행

```bash
# 1) 백엔드
cd back && cp .env.example .env     # 값 채우기 (외부 API 키는 비워도 실행됨)
./gradlew bootRun                    # http://localhost:8082/swagger-ui/index.html

# 2) 대시보드
cd dashboard && cp .env.example .env
pip install -r requirements.txt
python manage.py migrate --database=default && python manage.py createsuperuser --database=default
python manage.py runserver           # http://localhost:8000
python manage.py test                # Oracle 없이 돌아가는 테스트 10개

# 3) 앱 (에뮬레이터)
cd mobile && flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8082
```

자세한 확인 순서는 `실행확인_project4_백엔드.local.md`, `실행확인_project4_모바일.local.md` 를 참고하세요.

## API 요약 (신규)

| 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/util/weather?lat&lon` | 기상청 초단기실황 | - |
| GET | `/api/util/stores` · `/api/util/geocode?address` | 매장 목록 · 주소→좌표 | - |
| GET | `/api/util/book-news` | 도서 뉴스(RSS) | - |
| POST | `/api/util/ocr` | 표지 사진 → 추천 검색어 | 로그인 |
| GET | `/api/books/external/search?source=kakao\|naver\|nl&keyword` | 외부 도서 통합검색 | - |
| POST | `/api/books/external/import` | 외부 도서 등록 | ADMIN |
| GET | `/api/posts?feed=following&tag=` | 팔로잉 피드 | 로그인 |
| POST | `/api/posts/{id}/retweet` · `/api/users/{id}/follow` | 리트윗 · 팔로우 토글 | 로그인 |
| GET | `/api/users/{id}/profile·posts·followers·followings` | 프로필 | - |
| GET | `/api/users/me/liked-posts` | 좋아요한 글 | 로그인 |
| POST | `/auth/app/exchange` | 소셜 로그인 일회용 코드 → 토큰 | - |
| POST | `/api/statistics/sync` | Django 동기화 요청 | 공유 토큰 |

## 검증한 내용

- **백엔드**: JDK 17 + project_3 빌드 산출물의 의존성으로 `javac` 전체 컴파일 통과 (Lombok 포함). 기상청 격자 변환을 실행해 공식 표와 일치 확인(서울 60,127 / 부산 98,76 / 인천 55,124)
- **챗봇**: FAQ 26개에 대해 질문 52개 라우팅 시뮬레이션 — 불일치 0건
- **대시보드**: `manage.py check` 통과, 테스트 10개 통과
- **앱**: Dart 구문 파싱 + import 경로·내부 심볼·미사용 import 검사 0건 (오류를 일부러 넣어 검사기가 잡는지 확인)

**직접 확인이 필요한 것**: Flutter SDK·Oracle·Redis 가 없는 환경에서 작업해서 `flutter analyze / flutter run` 과 실제 API 키로 외부 API 호출은 실행하지 못했습니다. 위 실행 순서대로 한 번 돌려 보시고 오류가 나면 메시지를 그대로 알려주세요.
