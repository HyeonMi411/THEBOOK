# BookStore v1 — 온라인 도서 쇼핑몰 (Spring MVC + MyBatis)

> 도서 쇼핑몰(BookStore) 프로젝트를 세 번에 걸쳐 재설계한 여정의 1차 버전입니다.
> Spring의 내부 동작 원리를 기초부터 이해하기 위해, 자동설정 없이 XML 기반으로 프레임워크를 직접 구성했습니다.

## 다른 버전 보기
| 버전 | 설명 | 링크 |
|---|---|---|
| v1 (현재) | Spring MVC + MyBatis + JSP | - |
| v2 | Spring Boot + Thymeleaf + OAuth2 + AI RAG | [project_2](../project_2) |
| v3 | REST API + JWT + Next.js + 카카오페이 (배포됨) | [project_3](../project_3) |
| v4 | Flutter 모바일 앱 | [project_4](../project_4) |
| v5 | Django + Python 재작성 + 통계 대시보드 | [project_5](../project_5) |

## 기간
2026.06.16 ~ 2026.06.22

## 개발 환경
- Backend: Java, Spring MVC(XML 기반 설정), Spring Security(세션 기반), MyBatis
- Database: MySQL, Oracle(ojdbc11) 병행 지원
- View: JSP, JSTL(서버사이드 렌더링)
- Build: Maven

## GitHub
https://github.com/HyeonMi411/THEBOOK/tree/main/project_1

## 주요 기능
1. 회원가입/로그인 — 세션 기반 Spring Security 인증
2. 도서 관리 — 등록/수정/삭제/목록조회
3. 게시판 — 글쓰기/수정/삭제/목록/상세조회

## 담당 업무 및 성과

**Spring MVC 3-Tier 아키텍처 설계**
XML 기반 설정(root-context, servlet-context, security-context)으로 Bean과 요청 흐름을 직접 구성하며 Spring의 내부 동작 원리를 기초부터 이해했습니다.
→ 성과: 이후 Spring Boot의 자동설정(Auto Configuration)이 실제로 무엇을 대신 해주는지 명확히 비교할 수 있는 기반 확보

**MyBatis Mapper 직접 작성**
회원/도서/게시판 각 도메인의 SQL을 Mapper XML로 직접 작성하며, ORM 없이 SQL을 명시적으로 다루는 방식의 장단점을 체감했습니다.
→ 성과: 이후 프로젝트(v3)에서 JPA와 MyBatis를 domain 특성에 맞게 선택하는 하이브리드 설계의 판단 기준을 마련

## 트러블슈팅

**[설정] XML 기반 Bean 등록 범위 문제**
- 문제: root-context.xml과 servlet-context.xml에 Bean을 나눠 등록하는 과정에서 등록 범위가 꼬여 일부 Bean을 찾지 못하는 오류 발생
- 해결: 공통 Bean(Service, DAO)은 root-context, 웹 관련 Bean(Controller)은 servlet-context로 등록 범위를 명확히 분리
- 성과: Spring의 부모-자식 컨텍스트 구조를 실전에서 체득

## 프로젝트 소감
XML 설정을 한 줄씩 직접 작성하며, 이후 Spring Boot의 자동설정이 내부적으로 어떤 일을 대신 해주는지 비교할 수 있는 기준을 마련한 프로젝트였습니다.
