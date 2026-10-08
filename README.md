# 📚 THEBOOK — BookStore 프로젝트 시리즈

> 온라인 도서 쇼핑몰(BookStore) 하나를 **네 번에 걸쳐 처음부터 다시 설계**한 프로젝트 저장소입니다.
> 한 번 만든 결과물에 만족하기보다 "왜 이 구조가 더 나은가"를 스스로 검증하고 싶어서, 기술 스택과 아키텍처를 단계적으로 바꿔가며 같은 서비스를 반복해서 완성했습니다. 웹 버전(v3)은 AWS에 배포했고, v4는 Flutter 모바일 앱과 운영 대시보드로 확장했습니다.

**작성자**: 김현미 (Hyeon Mi Kim) · [jewelkhm@naver.com](mailto:jewelkhm@naver.com) · [포트폴리오 사이트](https://hyeonmi411.github.io/2026-AI-FULLSTACK/)

---

## 🔗 바로가기

| 버전 | 핵심 변화 | 기간 | 결과물 |
|---|---|---|---|
| [**v1**](./project_1) | Spring MVC + MyBatis — 3-Tier 아키텍처 기본기 | 2026.06.16 ~ 06.22 | 시연 영상 |
| [**v2**](./project_2) | Spring Boot 전환 + OAuth2 소셜로그인 | 2026.07.02 ~ 07.14 | 시연 영상 |
| [**v3**](./project_3) | REST API + JWT + Next.js 분리, 카카오페이 결제, AI 문서 챗봇, AWS 배포 | 2026.08.12 ~ 08.28 | 🟢 **배포됨** → [bookproject3.duckdns.org](https://bookproject3.duckdns.org/books) |
| [**v4**](./project_4) | Flutter 모바일 앱 + SNS 커뮤니티 + Django 운영 대시보드 | 2026.09.11 ~ 09.23 | 시연 영상 · 앱 소스 |

각 폴더의 README에 버전별 기술 스택, 트러블슈팅, 실행 방법이 정리되어 있습니다.

---

## 🧭 왜 같은 프로젝트를 네 번 다시 만들었는가

기술 하나를 "써봤다"와 "왜 이걸 선택했는지 비교해서 판단할 수 있다"는 완전히 다른 수준이라고 생각했습니다. 그래서 매 단계마다 이전 버전에서 겪은 한계를 다음 버전에서 의도적으로 다른 방식으로 풀어보았습니다.

```
v1 (2026.06)              v2 (2026.07)              v3 (2026.08)                 v4 (2026.09)
Spring MVC(XML)    ──▶    Spring Boot        ──▶    REST API + Next.js SPA ──▶   Flutter 모바일 앱
MyBatis 단일              자동설정                  JPA + MyBatis 하이브리드      + SNS 커뮤니티
세션 인증                 OAuth2 소셜로그인         JWT 무상태 인증               + 외부 API 장애 대응
JSP                       외부 API 연동             Redis 캐싱, 카카오페이        + Django 운영 대시보드
                                                    AI 문서 챗봇(RAG)
                                                    EC2 배포 · CI/CD              (Java ↔ Python 연동)
```

| 축 | v1 | v2 | v3 | v4 |
|---|---|---|---|---|
| 데이터 접근 | MyBatis | MyBatis | **JPA + MyBatis 하이브리드** | v3 + Django ORM(읽기 전용) |
| 인증 | 세션 | 세션 + OAuth2 | **JWT + OAuth2** | JWT + 앱 토큰 저장·자동 재발급 |
| 화면 | JSP | Thymeleaf | **Next.js(React) SPA** | **Flutter 앱** |
| 배포 | 로컬 | 로컬 | **EC2 + nginx + CI/CD** | 워크플로 작성, 시연 영상 |

---

## 🛠 전체 기술 스택

**Backend** — Java 17, Spring MVC / Boot 3, Spring Security, JWT, OAuth2, JPA, MyBatis, Redis
**Frontend** — JSP, Thymeleaf, React(Next.js), Redux-Saga, Ant Design, Flutter(Riverpod 3, Dio)
**Data / Infra** — Oracle, MySQL, Docker, AWS EC2, nginx, GitHub Actions
**Data 분석** — Python, Django, pandas, Chart.js
**외부 연동** — 카카오페이, OAuth2(구글·카카오·네이버), OpenAI GPT, 카카오 도서·Local, 국립중앙도서관, 기상청, Gmail SMTP

---

## 🐛 버전을 거치며 겪고 해결한 문제들 (일부)

| 버전 | 문제 | 해결 |
|---|---|---|
| v1 | XML Bean 등록 범위 충돌 | root-context / servlet-context 역할 분리 |
| v3 | AI 챗봇에 긴 PDF를 그대로 보내면 응답 지연·비용 증가 | 입력 6,000자·응답 500토큰 상한 적용 |
| v3 | 결제 승인 시점 재고 동시 차감 | 비관적 락 + 낙관적 락 이중 적용 |
| v3 | JPA 1차 캐시와 MyBatis 데이터 불일치 | `entityManager.clear()`로 동기화 |
| v3 | 하드 삭제 시 FK 제약 위반(ORA-02292) | Soft Delete 정책으로 전환 |
| v3 배포 | nginx 라우팅·CI/CD 시크릿·Redirect URI 누락 | 운영 로그로 원인 추적 후 해결 |
| v3 운영 | 도서검색 0건·베스트셀러 500·CI 빌드 실패 | 한글 URL 인코딩, Redis 날짜 직렬화, Gradle 플러그인 저장소 설정 수정 |
| v3 운영 | 카카오페이 승인 후에 재고를 확인하는 순서 | 재고 확인·차감을 승인 API 호출 앞으로 옮겨 "결제됐는데 재고 없음" 방지 |
| v4 | 앱에서 15분마다 로그인 해제 | 앱 저장소에 토큰 보관 + Dio 인터셉터 자동 재발급 |
| v4 | 한글 검색 시 외부 API 400·502 | URL 인코딩 누락 수정 |
| v4 | 기상청 API 장애·키 미설정 | Open-Meteo 대체 경로 |
| v4 | 에뮬레이터에서 소셜 로그인 실패 | 사설 IP Redirect 제한 → `adb reverse` + localhost 등록 |

---

## 📂 저장소 구조

```
THEBOOK/
├── project_1/   Spring MVC + MyBatis
├── project_2/   Spring Boot + Thymeleaf + OAuth2
├── project_3/   REST API + JWT + Next.js (AWS 배포됨)
│   ├── back/       Spring Boot
│   └── front/      Next.js
├── project_4/   Flutter 앱 + Spring Boot API + Django 대시보드
│   ├── back/       Spring Boot
│   ├── mobile/     Flutter
│   └── dashboard/  Django 운영 대시보드
└── .github/workflows/   GitHub Actions (프로젝트별 배포 워크플로)
```

---

## 🎥 시연 영상

- v1: https://www.youtube.com/watch?v=xN2kwFkq-PY
- v2: https://www.youtube.com/watch?v=IdoHBQfeH5Q
- v3: https://www.youtube.com/watch?v=eDxqr3u_LcY · https://www.youtube.com/watch?v=eByYCYUFjTI
- v4: https://www.youtube.com/watch?v=pUG-v9Hbqv8 · https://www.youtube.com/watch?v=EuUjMA2sNWk · https://www.youtube.com/watch?v=K4Q3YSEAb-0 (운영 대시보드)
