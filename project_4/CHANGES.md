# project_4 변경 내역 (Flutter 전환 + 전체 API 연동 + 커뮤니티 + 상담 챗봇 + 대시보드)

> 이 zip 을 저장소 루트(`THEBOOK/`)에 **덮어쓰기** 하세요. `project_4/mobile/lib` 는 새로 구성했으므로
> 기존 `lib` 폴더를 **먼저 지운 뒤** 덮어써야 예전 파일(users_page.dart 등)이 남지 않습니다.
> 저장소에 `project_4/.github/` 폴더가 남아 있다면 삭제하세요 (워크플로는 루트 `.github/workflows/deploy-project4.yml`).

## 1. 기존 앱에서 고친 실제 버그

| 위치 | 문제 | 결과 |
|---|---|---|
| 도서 목록 | 서버 응답은 `content` 인데 앱은 `results` 를 찾음 → 목록 파싱 실패 | 수정 + 무한 스크롤 |
| 도서 표지/재고 | 앱은 `cover`·`stock_quantity`, 서버는 `bookCover`·`stockQuantity` → 표지·재고 항상 빈 값 | 수정 |
| 공지 목록 | 도서와 같은 `results` 문제 | 수정 |
| 회원가입 | 서버는 이메일 인증 필수인데 앱에 인증 단계가 없어 가입이 항상 실패 | 인증번호 발송/확인 화면 추가 |
| 로그인 유지 | 앱을 다시 켜면 토큰이 있어도 로그아웃 상태로 보임 | 시작 시 `/auth/me` 로 복원 |
| 토큰 재발급 | Provider 마다 Dio·재발급 로직이 따로 있어 동시 401 시 재발급 중복 | 공통 `DioClient` (single-flight) |
| 앱바 제목 | 모든 탭 제목이 '마이페이지' 고정 | 탭별 제목 |
| 대시보드 로그아웃 | Django 5+ 는 로그아웃이 POST 만 허용 → 링크(GET)가 405 | POST 폼 |
| `AppUser` | `@Builder` 가 `deleted` 기본값을 무시해 null 저장 | `@Builder.Default` |
| 설정 | 국립중앙도서관 API 키가 yml 에 하드코딩(공개 저장소 노출) | `.env` 로 이동 — **기존 키는 재발급 권장** |

## 2. 추가 기능

- **외부 API**: 기상청 날씨(격자 변환), 매장 지도(OpenStreetMap)·카카오 주소→좌표, 다음 우편번호, 네이버·카카오·국립중앙도서관 통합검색 + 관리자 등록, CLOVA OCR 표지 검색, jsoup RSS 도서 뉴스(스케줄러), 결제완료 이메일(HTML 영수증)·CoolSMS 문자, ChatGPT(챗봇 보조)
- **커뮤니티**: 리트윗, 팔로우, 팔로잉 피드, 프로필, 팔로워/팔로잉 목록, 좋아요한 글 (해시태그·좋아요·댓글 유지)
- **소셜 로그인 앱 연동**: `/oauth2/app/callback` → Redis 60초 일회용 코드 → `bookstore4://` → `POST /auth/app/exchange` (토큰을 앱 URL 에 싣지 않음)
- **결제 복귀**: 결제 완료 페이지의 "앱으로 돌아가기" → 주문내역 자동 이동
- **상담 챗봇**: PDF RAG(`llmrag/`, `AiController`, `company.pdf`, pdfbox) 삭제 → 버튼·FAQ·AI 보조·상담원 하이브리드, 질문 로그(`CHATBOT_LOGS`)
- **대시보드**: 커뮤니티·챗봇 분석, Spring→Django 운영 지표 동기화(`opslog` 앱, 공유 토큰), 테스트 10개

## 2-1. 3차 웹 기능 중 앱에 추가로 옮긴 것 (v4.1)

| 기능 | 앱 위치 | 사용 API |
|---|---|---|
| 도서 직접 등록·수정 (표지 업로드, 출판일 달력) | 도서 탭 `도서 등록` 버튼 / 도서 상세 › 관리자 메뉴 | `POST·PATCH /api/books` |
| 재고 수량 변경 · 도서 삭제 | 도서 상세 › 관리자 메뉴 | `PATCH /api/books/{id}/stock`, `DELETE /api/books/{id}` |
| 공지 작성·수정·삭제 (첨부 이미지) | 공지사항 `공지 작성` 버튼 / 공지 상세 › 관리자 메뉴 | `POST·PATCH·DELETE /api/notices` |
| 실시간 검색 자동완성 (300ms 디바운스, 3차 BookSearchBox 와 동일) | 도서 탭 검색창 | `GET /api/books/search` |
| 국립중앙도서관 KDC 분류 찾아보기 + 관리자 저장 | 홈 바로가기 `국립중앙도서관`, 외부 도서검색 › `KDC 분류` | `GET /api/books/national-library/search`, `POST …/save` |
| 장바구니 체크박스로 골라서 주문 (전체 선택) | 장바구니 탭 | `POST /api/orders` (cartItemIds) |
| 회원가입 시 프로필 사진 | 회원가입 화면 | `POST /auth/signup` (ufile) |

관리자 메뉴는 `ROLE_ADMIN` 계정에만 보이고, 서버도 `@PreAuthorize` 로 한 번 더 막습니다.

## 3. 새로 추가된 DB 객체 (JPA `ddl-auto: update` 가 자동 생성)

`FOLLOWS`(+`FOLLOW_SEQ`), `CHATBOT_LOGS`(+`CHATBOT_LOG_SEQ`), `ORDERS` 에 `RECEIVER_NAME / RECEIVER_PHONE / ZIPCODE / ADDRESS / ADDRESS_DETAIL` 컬럼(nullable)

## 4. GitHub Secrets 추가 (모두 선택 — 비우면 해당 기능만 "설정 안 됨")

`KMA_API_KEY`, `NAVER_BOOK_CLIENT_ID`, `NAVER_BOOK_CLIENT_SECRET`, `NAVER_OCR_SECRET`, `NAVER_OCR_INVOKE_URL`,
`COOLSMS_API_KEY`, `COOLSMS_API_SECRET`, `COOLSMS_FROM`, `STATS_SYNC_TOKEN`, `OAUTH2_SIGNUP_CONFIRM_URL_P4`

값을 바꿔야 하는 기존 Secret: `OAUTH2_REDIRECT_URL_P4` = `https://<project_4 도메인>/oauth2/app/callback`,
`OAUTH2_SIGNUP_CONFIRM_URL_P4` = `https://<project_4 도메인>/oauth2/app/signup`
(카카오·네이버·구글 개발자 콘솔의 Redirect URI 는 기존 `/login/oauth2/code/{provider}` 그대로)

## 5. 앱 패키지 추가

`app_links`(딥링크), `webview_flutter`(우편번호), `flutter_map`·`latlong2`(지도), `geolocator`(위치)
Android: 위치 권한, `bookstore4` intent-filter, 로컬 개발용 http 허용(10.0.2.2/localhost 만) / iOS: 위치·카메라·사진 권한 문구, URL 스킴
