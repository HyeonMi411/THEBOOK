# 📚 BookStore v2 — 온라인 도서 쇼핑몰 (Spring Boot + Thymeleaf 고도화)

> BookStore 프로젝트의 두 번째 버전입니다. v1의 XML 기반 Spring MVC를 Spring Boot로 전환하고, OAuth2 소셜로그인과 PDF 기반 AI RAG 챗봇을 처음으로 도입했습니다.

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
2. **PDF 문서를 업로드하면 OpenAI GPT가 그 내용을 근거로 답변하는 AI 문서 챗봇(RAG)** 을 처음부터 설계·구현하는 것

---

## 🛠 기술 스택

**Backend**
- Java, Spring Boot (Maven)
- Spring Security + OAuth2 Client (구글 / 카카오 / 네이버 소셜로그인)
- MyBatis

**Database**
- Oracle (ojdbc11)

**View**
- Thymeleaf (`thymeleaf-layout-dialect`, `thymeleaf-extras-springsecurity6`)

**AI**
- PDFBox 3.x — PDF 텍스트 추출
- OpenAI GPT (Chat Completions API) 연동
- Gson, Jsoup — 외부 API(도서검색 등) 파싱

**Others**
- Spring Mail (Gmail SMTP) — 이메일 발송

---

## ✨ 주요 기능

1. **회원가입 / 로그인** — 세션 기반 인증 + 구글 / 카카오 / 네이버 OAuth2 소셜로그인
2. **도서 / 공지사항 관리** — Thymeleaf 화면 기반 CRUD
3. **AI 문서 챗봇** — PDF 업로드 시 텍스트를 추출한 뒤 OpenAI GPT에 질의, 화면에 답변을 렌더링

---

## 🏗 아키텍처

```
com.thejoa703
├── controller
├── service
├── oauth2        # OAuth2 소셜로그인 처리
├── llmrag        # AiService, RagInitializer — PDF 파싱 + GPT 질의
├── dao / mapper   # MyBatis
└── security
```

**AI 챗봇 처리 흐름**
```
PDF 업로드 → PDFBox 텍스트 추출 → 컨텍스트 구성 → OpenAI Chat Completions API 호출
→ 응답 파싱 → Thymeleaf 화면에 답변 렌더링
```

---

## 💡 담당 업무 및 배운 점

**Spring Boot 전환 및 OAuth2 소셜로그인 도입**
v1의 XML 설정을 Spring Boot 자동설정으로 대체하고, Spring Security OAuth2 Client로 3개 소셜 제공자 로그인을 연동했습니다.
→ 인증 방식을 세션 기반에서 OAuth2 위임 인증으로 확장하는 실전 경험을 확보했습니다.

**LLM 기반 RAG 챗봇 최초 구현**
PDFBox로 업로드된 PDF에서 텍스트를 추출하고, 이를 컨텍스트로 삼아 OpenAI Chat Completion API에 질의하는 서비스를 처음부터 설계했습니다.
→ 외부 유료 API를 프로젝트에 통합할 때 필요한 요청/응답 DTO 설계, 예외 처리, 컨텍스트 구성 방식에 대한 실전 경험을 확보했습니다 — 이 경험을 v3에서 REST API화·비용 통제 로직으로 고도화했습니다.

---

## 🚀 실행 방법

```bash
# 1. Oracle DB 준비
# 2. .env 또는 application.yml 에 아래 값 설정
#    - GOOGLE_CLIENT_ID / SECRET, KAKAO_CLIENT_ID, NAVER_CLIENT_ID / SECRET
#    - GMAIL_USERNAME / GMAIL_APP_PASSWORD
#    - OPENAI_API_KEY
# 3. 빌드 및 실행
mvn clean install
mvn spring-boot:run
```

`http://localhost:8080` 에서 확인 가능합니다.

---

## 🔗 관련 링크

- 시연 영상: https://www.youtube.com/watch?v=IdoHBQfeH5Q
- 이전 버전: [← BookStore v1](../project_1)
- 다음 버전: [BookStore v3 →](../project_3)
