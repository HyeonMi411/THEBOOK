# BookStore v4 — Flutter 앱

Feature-first 구조 (기능별 data / presentation 분리)

```
lib/
├── main.dart / app.dart          # ProviderScope, 딥링크(bookstore4://) 처리, 라우트
├── core/network/                 # ApiClient(서버 주소), DioClient(토큰 자동첨부·재발급), RefreshCookie
├── core/utils/format.dart        # 가격·날짜·이미지 URL
├── shared/                       # 하단 5탭(MainShell), 공통 레이아웃(🎧 챗봇), EmptyView/NetImage
└── features/
    ├── home/        날씨·베스트셀러·공지·책 소식
    ├── books/       목록·자동완성·상세·외부 통합검색·국립중앙도서관 KDC·표지 OCR·관리자 도서 등록/수정
    ├── cart/        장바구니·주문서(우편번호)·카카오페이
    ├── orders/      주문내역
    ├── post/        커뮤니티(피드·팔로잉·태그·좋아요·댓글·리트윗·프로필·팔로우)
    ├── auth/        로그인(소셜)·이메일 인증 가입·소셜 가입확인·마이페이지
    ├── map/         매장 지도 (OpenStreetMap)
    ├── address/     다음 우편번호 WebView
    ├── notices/     공지사항 (관리자 작성·수정·삭제)
    └── chatbot/     상담 챗봇
```

실행: `flutter pub get && flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8082`
※ `--dart-define=API_BASE_URL` 없이 Android로 빌드하면 미배포 서버 주소를 바라보므로 반드시 지정합니다.

테스트: `flutter test` (가격·날짜 포맷 유틸 단위 테스트)
