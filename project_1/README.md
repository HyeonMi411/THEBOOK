# 📚 BookStore v1 — 온라인 도서 쇼핑몰 (Spring MVC + MyBatis)

> BookStore 프로젝트의 첫 번째 버전입니다. Spring MVC(XML 기반 설정)와 MyBatis를 이용해 전통적인 3-Tier(Controller–Service–DAO) 아키텍처를 학습하고 구현하는 데 목표를 두었습니다.

---

## 🔗 BookStore 시리즈 전체 보기

| 버전 | 설명 | 상태 |
|---|---|---|
| **v1 (현재)** | Spring MVC + MyBatis + JSP | - |
| [v2](../project_2) | Spring Boot + Thymeleaf + OAuth2 + AI RAG | - |
| [v3](../project_3) | REST API + JWT + Next.js + 카카오페이 | 🟢 배포됨 |
| [v4](../project_4) | Flutter 모바일 앱 + SNS 게시판 | 준비 중 |
| [v5](../project_5) | Django + Python 재작성 + 통계 대시보드 | 준비 중 |

---

## 📌 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 기간 | 2026.06.16(화) ~ 2026.06.22(월) |
| 구분 | 개인 프로젝트 |
| 다음 버전 | [v2 (Spring Boot + Thymeleaf)](../project_2) → [v3 (REST API + JWT + Next.js)](../project_3) |

이 버전은 이후 v2, v3로 이어지는 BookStore 시리즈의 **가장 기초 단계**입니다. XML 기반 Spring 설정을 직접 다루면서 프레임워크의 내부 동작 원리를 기초부터 이해하고, MyBatis Mapper를 직접 작성하며 SQL을 명시적으로 다루는 경험을 쌓는 데 집중했습니다.

---

## 🛠 기술 스택

**Backend**
- Java, Spring MVC (XML Context 설정: `root-context.xml`, `servlet-context.xml`, `security-context.xml`)
- Spring Security (세션 기반 폼 로그인)
- MyBatis + MyBatis-Spring

**Database**
- MySQL, Oracle(ojdbc11) 병행 지원
- log4jdbc-log4j2-jdbc4 (SQL 로깅)

**View**
- JSP, JSTL (서버사이드 렌더링)

**Build**
- Maven

---

## ✨ 주요 기능

1. **회원가입 / 로그인** — 세션 기반 Spring Security 인증
2. **도서 관리** — 등록 / 수정 / 삭제 / 목록 조회
3. **게시판** — 글쓰기 / 수정 / 삭제 / 목록 / 상세 조회

---

## 🏗 아키텍처

```
com.the703
├── controller   # BookController, UserController, BoardController
├── service      # 비즈니스 로직
├── dao          # MyBatis Mapper 연동
├── dto          # 요청/응답 객체
├── security     # LoginSuccessHandler, CustomUserDetailsService, DeniedHandler
└── util

src/main/resources (config)
├── root-context.xml       # 공통 Bean 설정
├── servlet-context.xml    # MVC 설정
├── security-context.xml   # Spring Security 설정
├── mybatis-config.xml
└── *-mapper.xml           # user/book/board Mapper
```

---

## 💡 담당 업무 및 배운 점

**Spring MVC 3-Tier 아키텍처 설계**
XML 기반 설정으로 Bean과 요청 흐름을 직접 구성하며 Spring의 내부 동작 원리를 기초부터 이해했습니다.
→ 이후 Spring Boot의 자동설정(Auto Configuration)이 실제로 무엇을 대신해주는지 명확히 비교할 수 있는 기반이 되었습니다.

**MyBatis Mapper 직접 작성**
회원 / 도서 / 게시판 각 도메인의 SQL을 Mapper XML로 직접 작성하며, ORM 없이 SQL을 명시적으로 다루는 방식의 장단점을 체감했습니다.
→ 이후 v3에서 JPA와 MyBatis를 도메인 특성에 맞게 선택하는 하이브리드 설계의 판단 기준을 마련했습니다.

---

## 🐛 트러블슈팅

**[설정] XML 기반 Bean 등록 범위 문제**
- 문제: `root-context.xml`과 `servlet-context.xml`에 Bean을 나눠 등록하는 과정에서 등록 범위가 꼬여 일부 Bean을 찾지 못하는 오류 발생
- 해결: 공통 Bean(Service, DAO)은 `root-context`, 웹 관련 Bean(Controller)은 `servlet-context`로 등록 범위를 명확히 분리
- 배운 점: Spring의 부모-자식 컨텍스트 구조를 실전에서 체득

---

## 🚀 실행 방법

```bash
# 1. MySQL 또는 Oracle DB 준비 후 계정/스키마 생성
# 2. config/*.xml 에서 DB 접속 정보 확인
# 3. Maven 빌드
mvn clean install

# 4. Tomcat 등 서블릿 컨테이너에 war 배포 후 실행
```

배포된 버전은 없으며, 로컬 서블릿 컨테이너(Tomcat)에서 실행해 확인하는 버전입니다.

---

## 🔗 관련 링크

- GitHub: https://github.com/HyeonMi411/THEBOOK/tree/main/project_1
- 시연 영상: https://www.youtube.com/watch?v=xN2kwFkq-PY
- 배포 주소: 없음 (로컬 실행 전용)
- 다음 버전: [BookStore v2 →](../project_2)
