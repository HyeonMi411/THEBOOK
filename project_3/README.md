# BookStore v3 — 온라인 도서 쇼핑몰 (React 전환 + JWT 인증 + AI RAG)

> REST API와 JWT 인증으로 백엔드·프론트엔드를 완전히 분리하고, 카카오페이 실결제·Redis 캐싱·AI RAG 챗봇까지 갖춘 실무 수준의 온라인 도서 쇼핑몰입니다.
> 실제로 EC2에 배포되어 운영 중인 최종 버전입니다.

## 배포 주소
https://bookproject3.duckdns.org

## 다른 버전 보기
| 버전 | 설명 | 링크 |
|---|---|---|
| v1 | Spring MVC + MyBatis + JSP | [project_1](../project_1) |
| v2 | Spring Boot + Thymeleaf + OAuth2 + AI RAG | [project_2](../project_2) |
| v3 (현재, 배포됨) | REST API + JWT + Next.js + 카카오페이 | - |
| v4 | Flutter 모바일 앱 (이 프로젝트의 백엔드를 그대로 사용) | [project_4](../project_4) |
| v5 | Django + Python 재작성 + 통계 대시보드 | [project_5](../project_5) |

## 기간
2026.08.12 ~ 2026.08.28

## 개발 환경
- Backend: Java17, Spring Boot 3.3.5, Spring Security, JWT, Redis, JPA, MyBatis, Oracle
- Frontend: Next.js(React), Redux Toolkit, Redux-Saga, Axios, Ant Design
- Others: JWT, OAuth2(Google/Kakao/Naver), 카카오페이 결제 API, Swagger(OpenAPI)

## GitHub
https://github.com/HyeonMi411/THEBOOK/tree/main/project_3

## 주요 기능
1. 회원가입/로그인 — JWT(AccessToken/RefreshToken) 기반 인증, 구글·카카오·네이버 소셜로그인
2. 도서 관리 — 등록/수정/소프트삭제, 검색, 카테고리별 조회, 재고 관리(관리자 전용)
3. 장바구니/주문/결제 — 장바구니 담기·수정·삭제, 장바구니결제·바로구매 주문 생성, 카카오페이 결제(준비→진행→승인)
4. 공지사항 — 목록/상세/검색/페이징
5. 베스트셀러 랭킹 — Redis 캐싱 기반 판매량 TOP 10 조회
6. AI 문서 챗봇(RAG) — PDF 업로드 후 OpenAI GPT 질의응답
7. 국립중앙도서관 연계 검색

## 담당 업무 및 성과

**데이터 접근 계층 설계**
JPA와 MyBatis를 도메인 특성에 맞게 분리 설계(단순 CRUD는 JPA, 복잡한 조회는 MyBatis)했습니다.
→ 성과: 코드 가독성과 조회 성능을 동시에 확보

**재고 동시성 제어**
결제 승인 시점 재고 차감에 비관적 락(SELECT ... FOR UPDATE)과 낙관적 락(@Version)을 함께 적용했습니다.
→ 성과: 동시 결제 상황에서도 재고 정합성 보장

**소프트 삭제 설계**
도서 하드 삭제 시 장바구니·주문 이력의 FK 제약 위반(ORA-02292)이 발생하는 문제를 발견하고 소프트 삭제(DELETED 플래그)로 전환했습니다.
→ 성과: 판매 중단된 상품도 기존 주문 이력을 훼손하지 않고 안전하게 처리

**JWT + Redis 인증 흐름 안정화**
RefreshToken을 Redis에 저장하고, Axios 인터셉터로 AccessToken 만료 시 자동 재발급(Silent Refresh)을 구현했습니다.
→ 성과: 사용자가 세션 만료를 체감하지 못하는 끊김없는 인증 경험 제공

## 트러블슈팅

**[동시성] 결제 승인 시점 재고 동시 차감 문제**
- 문제: 같은 도서를 여러 사용자가 동시에 결제 승인하는 경우, 단순 재고 조회 후 차감 방식으로는 재고가 실제보다 더 많이 빠져나가는 동시성 문제 발생 가능
- 해결: 비관적 락과 낙관적 락을 이중으로 적용해 방지
- 성과: 여러 팀원이 동시에 API를 테스트하는 상황에서도 재고와 주문 데이터의 정합성 안정적으로 유지

**[영속성] JPA 1차 캐시와 MyBatis(raw SQL) 간 데이터 불일치**
- 문제: JPA로 로딩한 엔티티가 같은 트랜잭션 안에서 MyBatis로 변경한 DB 상태를 반영하지 못함
- 해결: `EntityManager.clear()`로 캐시를 명시적으로 비워 이후 조회가 최신 DB 값을 다시 읽도록 처리
- 성과: 서로 다른 데이터 접근 기술을 함께 쓸 때는 트랜잭션 경계와 캐시 동기화까지 고려해야 한다는 것을 실제 예외 로그 분석을 통해 체득

**[보안] IDOR 및 파일 업로드 취약점 점검**
- 문제: 회원정보 수정 API에서 URL 경로의 사용자 ID만 바꾸면 다른 사람의 닉네임·프로필사진을 수정할 수 있는 IDOR 취약점 발견
- 해결: 본인확인 로직을 추가하고, 이미지 업로드도 확장자·MIME 검증과 실제 파일 디코딩 검증을 추가해 위·변조 파일 업로드 가능성을 차단
- 성과: 기능 구현 이후에도 접근제어·입력값 검증 관점에서 스스로 코드를 재점검하는 습관 형성

**[배포] EC2/CI-CD 환경 이슈 다수 해결**
GitHub Actions 배포 경로 불일치(jar/uploads 경로 누락), nginx 라우팅 누락(OAuth2 signup, Swagger), Gmail/카카오페이 시크릿 미반영, 회원탈퇴 계정 로그인 예외 처리, 카카오 로그아웃/결제 Redirect URI 도메인 등록 등 실제 운영 환경에서만 드러나는 문제들을 하나씩 로그 기반으로 원인을 추적해 해결했습니다.

## 프로젝트 소감
하나의 데이터 접근 기술만 쓸 때는 겪지 않았을 문제들(1차 캐시 불일치, 연관관계 매핑 충돌)을 JPA와 MyBatis를 함께 쓰면서 직접 마주하고 해결했습니다. 이 과정에서 각 기술이 내부적으로 어떻게 동작하는지 더 깊이 이해하게 되었고, 실제 예외 스택트레이스를 읽고 원인을 역추적하는 문제해결 역량을 키울 수 있었습니다. 또한 로컬 개발환경과 실제 배포 환경(EC2·nginx·도메인) 사이에 존재하는 차이를 직접 겪으며, "로컬에서 되는 것"과 "배포 환경에서 되는 것"은 전혀 다른 문제라는 것을 체감했습니다.
