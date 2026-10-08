# 📚 BookStore v2 — 온라인 도서 쇼핑몰 (Spring Boot + Thymeleaf 고도화)

> BookStore 프로젝트의 두 번째 버전입니다. v1의 XML 기반 Spring MVC를 Spring Boot로 전환하고, OAuth2 소셜로그인과 외부 API 연동을 더했습니다.

---

## 🔗 BookStore 시리즈 전체 보기

| 버전 | 설명 | 상태 |
|---|---|---|
| [v1](../project_1) | Spring MVC + MyBatis + JSP | - |
| **v2 (현재)** | Spring Boot + Thymeleaf + OAuth2 소셜로그인 | - |
| [v3](../project_3) | REST API + JWT + Next.js + 카카오페이 | 🟢 배포됨 |
| [v4](../project_4) | Flutter 모바일 앱 + SNS 게시판 + 통계 대시보드 | - |

---

## 📌 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 기간 | 2026.07.02(목) ~ 2026.07.14(화) |
| 구분 | 개인 프로젝트 |
| 이전 버전 | [← v1 (Spring MVC + MyBatis)](../project_1) |
| 다음 버전 | [v3 (REST API + JWT + Next.js) →](../project_3) |

이 버전의 핵심 목표는 두 가지입니다.
1. v1의 XML 기반 Spring MVC를 **Spring Boot 자동설정** 기반으로 전환해 개발 생산성을 높이는 것
2. **OAuth2 소셜로그인**(구글 / 카카오 / 네이버)으로 인증 방식을 확장하는 것

---

## 🛠 기술 스택

**Backend**
- Java 17, Spring Boot 3.5 (Maven)
- Spring Security + OAuth2 Client (구글 / 카카오 / 네이버 소셜로그인)
- MyBatis (mybatis-spring-boot-starter 3.0)

**Database**
- Oracle (ojdbc11)

**View**
- Thymeleaf (`thymeleaf-layout-dialect`, `thymeleaf-extras-springsecurity6`)

**Others**
- Jackson(ObjectMapper) — 외부 API(카카오·네이버 도서검색) JSON 파싱
- JavaMail (네이버 SMTP) — 이메일 발송

---

## ✨ 주요 기능

1. **회원가입 / 로그인** — 세션 기반 인증 + 구글 / 카카오 / 네이버 OAuth2 소셜로그인
2. **도서 / 공지사항 관리** — Thymeleaf 화면 기반 CRUD
3. **외부 API 연동** — 카카오·네이버·국립중앙도서관 도서검색 및 저장, 네이버 OCR, 기상청 날씨, 메일 발송

---

## 🏗 아키텍처

```
com.the703
├── controller
├── service
├── oauth2        # OAuth2 소셜로그인 처리
├── dao / mapper   # MyBatis
└── security
```


---

## 💡 담당 업무 및 배운 점

**Spring Boot 전환 및 OAuth2 소셜로그인 도입**
v1의 XML 설정을 Spring Boot 자동설정으로 대체하고, Spring Security OAuth2 Client로 3개 소셜 제공자 로그인을 연동했습니다.
→ 인증 방식을 세션 기반에서 OAuth2 위임 인증으로 확장하는 실전 경험을 확보했습니다.

---

## 🚀 실행 방법

```bash
# 1. Oracle DB 준비 (접속 정보: src/main/resources/application.properties)
# 2. src/main/resources/application-oauth.properties 생성 (.gitignore 대상)
#    - OAuth2 클라이언트 (구글 / 카카오 / 네이버)
#    - openai.api.key, naver.host/user/password (메일), kakaoRestApi, book.clientId/clientSecret 등
# 3. 업로드 폴더 C:/upload 생성
# 4. 실행 (빌드 시: ./mvnw clean package -DskipTests)
./mvnw spring-boot:run
```

`http://localhost:8080` 에서 확인 가능합니다. 별도로 배포된 서버는 없습니다.

---

## 🔗 관련 링크

- GitHub: https://github.com/HyeonMi411/THEBOOK/tree/main/project_2
- 시연 영상: https://www.youtube.com/watch?v=IdoHBQfeH5Q
- 배포 주소: 없음 (로컬 실행 전용)
- 이전 버전: [← BookStore v1](../project_1)
- 다음 버전: [BookStore v3 →](../project_3)
