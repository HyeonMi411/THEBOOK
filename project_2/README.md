# BookStore v2 — 온라인 도서 쇼핑몰 (Spring Boot + Thymeleaf 고도화)

> v1의 XML 기반 Spring MVC를 Spring Boot로 전환하여 자동설정 기반 개발 생산성을 확보한 2차 버전입니다.
> OAuth2 소셜로그인과 PDF 기반 AI RAG 챗봇을 처음 도입했습니다.

## 다른 버전 보기
| 버전 | 설명 | 링크 |
|---|---|---|
| v1 | Spring MVC + MyBatis + JSP | [project_1](../project_1) |
| v2 (현재) | Spring Boot + Thymeleaf + OAuth2 + AI RAG | - |
| v3 | REST API + JWT + Next.js + 카카오페이 (배포됨) | [project_3](../project_3) |
| v4 | Flutter 모바일 앱 | [project_4](../project_4) |
| v5 | Django + Python 재작성 + 통계 대시보드 | [project_5](../project_5) |

## 기간
2026.07.02 ~ 2026.07.14

## 개발 환경
- Backend: Java, Spring Boot(Maven), Spring Security, OAuth2 Client, MyBatis
- Database: Oracle(ojdbc11)
- View: Thymeleaf(thymeleaf-layout-dialect, thymeleaf-extras-springsecurity6)
- AI: PDFBox(PDF 텍스트 추출) + OpenAI GPT API 연동, Gson/Jsoup(외부 API 파싱)
- Others: 이메일 발송(Spring Mail)

## GitHub
https://github.com/HyeonMi411/THEBOOK/tree/main/project_2

## 주요 기능
1. 회원가입/로그인 — 세션 기반 인증 + 구글/카카오/네이버 OAuth2 소셜로그인
2. 도서/공지사항 관리 — Thymeleaf 화면 기반 CRUD
3. AI 문서 챗봇 — PDF 업로드 시 텍스트 추출 후 OpenAI GPT에 질의, 화면에 답변 렌더링

## 담당 업무 및 성과

**Spring Boot 전환 및 OAuth2 소셜로그인 도입**
v1의 XML 설정을 Spring Boot 자동설정으로 대체하고, Spring Security OAuth2 Client로 3개 소셜 제공자 로그인을 연동했습니다.
→ 성과: 인증 방식을 세션 기반에서 OAuth2 위임 인증으로 확장하는 실전 경험 확보

**LLM 기반 RAG 챗봇 최초 구현**
PDFBox로 업로드된 PDF에서 텍스트를 추출하고, 이를 컨텍스트로 삼아 OpenAI Chat Completion API에 질의하는 서비스를 처음부터 설계했습니다.
→ 성과: 외부 유료 API를 프로젝트에 통합할 때 필요한 요청/응답 DTO 설계, 예외 처리, 컨텍스트 구성 방식에 대한 실전 경험 확보 — 이 경험을 v3에서 REST API·비용 통제 로직으로 고도화

## 트러블슈팅

**[연동] PDF 문서 길이에 따른 응답 지연 및 비용 문제**
- 문제: PDF 전체 텍스트를 컨텍스트로 그대로 OpenAI API에 전달하면, 문서가 길어질수록 응답이 느려지고 토큰 비용이 커짐
- 해결: 컨텍스트 길이에 상한을 두어 전달하는 방식으로 개선
- 성과: 응답 속도와 API 비용을 함께 통제할 수 있는 구조 확보

## 프로젝트 소감
세션 기반 인증만 다뤄봤던 v1과 달리, OAuth2 위임 인증과 외부 유료 API(OpenAI) 연동을 처음 시도하며 "내 서버가 아닌 곳에서 발급한 인증 정보를 어떻게 신뢰할 것인가"를 고민한 경험이 인상 깊었습니다.
