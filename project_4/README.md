# BookStore v4 — Flutter 모바일 앱 + SNS 게시판

> v3(Spring Boot + Next.js)의 백엔드는 그대로 유지한 채, 프론트엔드를 Flutter 모바일 앱으로 재구성한 버전입니다.
> 여기에 게시글·이미지·댓글·리트윗·해시태그를 갖춘 SNS 스타일 게시판 기능을 새로 추가했습니다.

## 다른 버전 보기
| 버전 | 설명 | 링크 |
|---|---|---|
| v1 | Spring MVC + MyBatis + JSP | [project_1](../project_1) |
| v2 | Spring Boot + Thymeleaf + OAuth2 + AI RAG | [project_2](../project_2) |
| v3 | REST API + JWT + Next.js + 카카오페이 (배포됨) | [project_3](../project_3) |
| v4 (현재) | Flutter 모바일 앱 + SNS 게시판 | - |
| v5 | Django + Python 재작성 + 통계 대시보드 | [project_5](../project_5) |

## GitHub
https://github.com/HyeonMi411/THEBOOK/tree/main/project_4

## 구성
```
project_4/
├── back/     v3(Spring Boot)의 완전한 복사본 + SNS 게시판(Post) API 신규 추가
└── mobile/   Flutter 모바일 앱 (v3의 Next.js 프론트엔드를 대체)
```

## 개발 환경
- Backend: v3와 동일(Java17, Spring Boot 3.3.5, Spring Security, JWT, Redis, JPA, MyBatis, Oracle) + 게시판 도메인(Post/Image/Comment/Retweet/Hashtag/PostLike) 신규 추가
- Mobile: Flutter, Riverpod(상태관리), Dio(HTTP 통신), flutter_secure_storage(토큰 암호화 저장), image_picker, url_launcher

## 주요 기능 (v3 화면 → Flutter 재구성)
| v3(Next.js) | v4(Flutter) |
|---|---|
| 도서 목록/상세 | 하단 탭 1번째 |
| (신규) SNS 게시판 | 하단 탭 2번째 |
| 공지사항 | 하단 탭 3번째 |
| 장바구니 + 카카오페이 결제 | 하단 탭 4번째 (결제창은 외부 브라우저로 오픈) |
| 마이페이지 + 주문내역 | 하단 탭 5번째 |
| 로그인/회원가입 | 별도 화면 |

## 담당 업무 및 성과

**Flutter 상태관리·통신 계층 설계**
Riverpod `Notifier` + Dio로 도서/장바구니/공지/주문/게시판 5개 도메인의 데이터 계층을 구성하고, 각 provider에 v3에서 검증된 JWT 401 자동 재발급 패턴(Axios 인터셉터)을 Dio 인터셉터로 동일하게 이식했습니다.
→ 성과: Next.js와 Flutter처럼 서로 다른 프레임워크 사이에서도, 인증 흐름 같은 공통 아키텍처 패턴은 언어에 무관하게 재사용 가능함을 확인

**SNS 게시판 도메인 통합**
게시글에 연관된 이미지·댓글·리트윗·해시태그 엔티티를 v3의 기존 보안 정책(JWT 인증, IDOR 방지, 소프트 삭제)과 충돌 없이 통합했습니다.
→ 성과: 기존 `SecurityConfig`의 권한 규칙 구조를 이해하고, 새 도메인을 안전하게 얹는 방법을 체득

## 트러블슈팅

**[아키텍처] project_3 API를 그대로 재사용할지, 새로 만들지 판단**
- 문제: Flutter는 클라이언트 프레임워크라 자체적으로 서버를 만들 수 없음
- 해결: 백엔드는 v3(Spring Boot)를 복사해 재사용하고, 클라이언트(화면)만 Flutter로 새로 작성하는 것으로 방향을 명확히 함
- 성과: "프레임워크 전환"이 항상 전체 스택의 재작성을 의미하지는 않으며, 계층별로 적절한 기술을 선택하는 판단 기준을 마련

**[검증 한계] 실제 빌드 환경 부재**
- 문제: 개발 환경에 JDK/Flutter SDK가 없어 실제 컴파일·빌드를 사전에 확인할 수 없었음
- 해결: 중괄호 균형, 모든 import 경로의 실제 파일 존재 여부를 전수 검증하는 방식으로 최대한 보완
- 성과: 로컬 빌드 전 정적 검증만으로 잡아낼 수 있는 오류와, 실제 빌드/실행을 해봐야만 드러나는 오류의 차이를 명확히 인식

## 배포 시 참고
- `deploy.yml`: v3(8080 포트)와 겹치지 않게 백엔드는 8082 포트로 분리
- `application-oauth.yml`의 `OAUTH2_REDIRECT_URL`, `mobile/lib/core/network/api_client.dart`의 배포 도메인은 실제 배포 시 새 값으로 교체 필요

## 프로젝트 소감
"3차 프로젝트를 Flutter로 다시 만든다"는 요청을 그대로 해석하면 모순(Flutter로는 서버를 못 만듦)이 됩니다. 이 과정에서 프레임워크마다 담당할 수 있는 계층이 다르다는 것을 명확히 이해하게 됐고, 재작성 요청을 받았을 때 "무엇을 재작성하는 게 의미 있는가"를 먼저 판단하는 습관을 갖게 됐습니다.
