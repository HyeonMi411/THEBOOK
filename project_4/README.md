# BookStore project_4 — project_3 전체를 Flutter 클라이언트로 재구성

`project_3`(Spring Boot + Next.js)는 그대로 유지한 채, project_3의 프론트엔드 기능 전체(도서/장바구니/결제/공지사항/마이페이지)와 `flutter.zip`의 SNS 게시판 기능을 합쳐서 **Flutter 모바일 앱 하나로 재구성**한 버전입니다.

## 구성

```
project_4/
├── back/     project_3(Spring Boot)의 완전한 복사본 + SNS 게시판(Post) API 신규 추가
└── mobile/   Flutter 모바일 앱 (Next.js 프론트엔드를 대체)
```

Next.js 웹 프론트엔드는 포함하지 않았습니다 — project_4의 클라이언트는 Flutter 앱 하나입니다.

## mobile 화면 구성 (project_3 메뉴 → Flutter 하단 탭)

| project_3(Next.js) | project_4(Flutter) |
|---|---|
| BOOK (도서 목록/상세) | 하단 탭 1번째 — `book_list_page.dart`, `book_detail_page.dart` |
| (신규) 게시판 | 하단 탭 2번째 — `post_list_page.dart` 등 (flutter.zip 기반) |
| NOTICE (공지사항) | 하단 탭 3번째 — `notice_list_page.dart`, `notice_detail_page.dart` |
| CART + 카카오페이 결제 | 하단 탭 4번째 — `cart_page.dart` (결제창은 `url_launcher`로 외부 브라우저 오픈) |
| MYPAGE + 주문내역 | 하단 탭 5번째 — `users_page.dart` + `orders_page.dart` |
| 로그인/회원가입 | 별도 화면(named route) — `login_page.dart`, `signup_page.dart` |

각 화면의 데이터 계층(`*_provider.dart`)은 Riverpod `Notifier` + Dio로 구성했고, `auth_provider.dart`에 이미 있던 JWT 자동 재발급(401 인터셉터) 패턴을 `cart_provider.dart`, `order_provider.dart`에도 동일하게 적용했습니다.

## back에 새로 추가한 것 (flutter.zip 기반)

| 파일 | 역할 |
|---|---|
| `entity/Post.java`, `Image.java`, `Comment.java`, `Retweet.java`, `Hashtag.java`, `PostLike.java` | 게시글 및 연관 데이터 |
| `service/PostService.java` | 게시글 CRUD, 본인 확인(IDOR 방지), 소프트 삭제 |
| `controller/PostController.java` | `/api/posts` — 목록/단건/작성/수정/삭제 |
| `SecurityConfig.java` | `GET /api/posts/**`는 비로그인도 조회 가능, 작성/수정/삭제는 인증 필요 |

## 실제 배포하실 예정이라 반영한 것

- `deploy.yml`: project_3(포트 8080)과 겹치지 않도록 **project_4 백엔드는 8082 포트**로 pm2에 등록
- `deploy.yml`: Flutter APK 빌드까지 CI에 포함(`build-mobile-apk` job) — 빌드된 APK는 Actions 결과물(Artifact)로 다운로드 가능
- `.env` 생성 스텝에 Gmail·카카오페이·OpenAI 시크릿 전부 포함 (project_3에서 여러 번 재발했던 누락 문제를 처음부터 방지)

## ⚠️ 배포 전 반드시 채워야 하는 것

1. **`back/src/main/resources/application-oauth.yml`의 `OAUTH2_REDIRECT_URL`** — project_4 전용 도메인이 정해지면 GitHub Secrets에 등록 (project_3 도메인과 절대 같으면 안 됨)
2. **`mobile/lib/core/network/api_client.dart`의 `_deployedBaseUrl`** — 지금은 `bookproject4.duckdns.org` 임시값. 실제 도메인으로 교체
3. **nginx**: project_3 서버에 project_4용 `location` 블록을 별도로 추가해야 함 (8082 포트로 proxy_pass). project_3 때 겪으셨던 `/oauth2/signup`, `/swagger-ui` 라우팅 누락 같은 실수를 이번엔 배포 시점에 미리 챙겨주세요
4. **`back/.env`**: 민감정보라 zip에서 제외했습니다. project_3에서 쓰시던 값을 참고해 새로 작성

## 검증한 내용

- back 신규 파일 15개 전체 중괄호 균형, 참조 클래스가 project_3 코드에 실제 존재하는지 확인
- mobile 전체 23개 dart 파일 — 중괄호/소괄호 균형, **모든 상대경로 import가 실제 파일을 정확히 가리키는지 전수 검증**
- AI 생성 흔적(`##`), 부자연스러운 문장체(`-습니다`) — back·mobile 전체 0건
- `deploy.yml` YAML 문법 검증

**다만 이 환경에는 JDK와 Flutter SDK가 설치되어 있지 않아 실제 `./gradlew build`, `flutter analyze`/`flutter build`는 실행하지 못했습니다.** 받으신 뒤 로컬에서 두 가지 모두 한 번씩 빌드해보시길 권장드립니다.
