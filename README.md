# 📚 THEBOOK — BookStore 프로젝트 시리즈

> 온라인 도서 쇼핑몰(BookStore) 하나를 **네 번에 걸쳐 처음부터 다시 설계**한 프로젝트 저장소입니다.
> 한 번 만든 결과물에 만족하기보다 "왜 이 구조가 더 나은가"를 스스로 검증하고 싶어서, 기술 스택과 아키텍처를 단계적으로 바꿔가며 같은 서비스를 반복해서 완성했습니다.

**작성자**: 김현미 (Hyeon Mi Kim) · [입사지원서/포트폴리오 문의](mailto:jewelkhm@naver.com)

---

## 🔗 바로가기

| 버전 | 핵심 변화 | 기간 | 상태 |
|---|---|---|---|
| [**v1**](./project_1) | Spring MVC + MyBatis — 3-Tier 아키텍처 기본기 | 2026.06.16 ~ 06.22 | 로컬 실행 |
| [**v2**](./project_2) | Spring Boot 전환 + OAuth2 소셜로그인 + AI RAG 챗봇 최초 구현 | 2026.07.02 ~ 07.14 | 로컬 실행 |
| [**v3**](./project_3) | REST API + JWT + Next.js 완전 분리, 카카오페이 실결제 | 2026.08.12 ~ 08.28 | 🟢 **배포됨** → [bookproject3.duckdns.org](https://bookproject3.duckdns.org/books) |
| [**v4**](./project_4) | Flutter 모바일 앱 + SNS 게시판 + Django 통계 대시보드 | 진행 중 | 🟡 배포 준비 중 |

각 폴더의 README에 버전별 기술 스택, 트러블슈팅, 실행 방법이 상세히 정리되어 있습니다.

---

## 🧭 왜 같은 프로젝트를 네 번 다시 만들었는가

기술 하나를 "써봤다"와 "왜 이걸 선택했는지 비교해서 판단할 수 있다"는 완전히 다른 수준이라고 생각했습니다. 그래서 매 단계마다 이전 버전에서 겪은 한계를 다음 버전에서 의도적으로 다른 방식으로 풀어보았습니다.

```
v1 (2026.06)                    v2 (2026.07)                    v3 (2026.08)                    v4 (진행중)
Spring MVC(XML)      ──▶        Spring Boot                ──▶  REST API + Next.js SPA    ──▶  Flutter 모바일 앱
MyBatis 단일                    Spring Boot 자동설정             JPA + MyBatis 하이브리드          + SNS 게시판
세션 기반 인증                   OAuth2 소셜로그인                 JWT 무상태 인증                   + Django 통계 대시보드
JSP 서버사이드 렌더링              AI RAG 챗봇(Thymeleaf 렌더링)     Redis 캐싱, 카카오페이 결제        (Java+Python 데이터 연동)
                                                                실제 EC2 배포·CI/CD
```

| 축 | v1 | v2 | v3 | v4 |
|---|---|---|---|---|
| 데이터 접근 | MyBatis 단일 | MyBatis 단일 | **JPA + MyBatis 하이브리드** | v3와 동일 + Django ORM(읽기전용) |
| 인증 방식 | 세션 | 세션 + OAuth2 | **JWT + OAuth2 (무상태)** | v3와 동일 |
| 화면 구조 | JSP | Thymeleaf | **Next.js(React) SPA** | **Flutter 모바일 앱** |
| 배포 | 없음 | 없음 | **EC2 + nginx + CI/CD** | 준비 중 |

---

## 🛠 전체 기술 스택 (버전 통틀어 사용)

**Backend** — Java, Spring MVC/Boot, Spring Security, JWT, OAuth2, JPA, MyBatis, Redis
**Frontend** — JSP, Thymeleaf, React(Next.js), Redux-Saga, Ant Design, Flutter(Riverpod, Dio)
**Data/Infra** — Oracle, MySQL, Docker, AWS EC2, nginx, GitHub Actions(CI/CD)
**외부 연동** — 카카오페이 결제, OAuth2(구글/카카오/네이버), OpenAI GPT(RAG 챗봇), 카카오 도서검색, 국립중앙도서관 오픈API
**신규 실험** — Django + pandas(판매 통계 대시보드, v4)

---

## 🐛 버전을 거치며 실제로 겪고 해결한 문제들 (일부)

| 버전 | 문제 | 해결 |
|---|---|---|
| v1 | XML Bean 등록 범위 충돌 | root-context/servlet-context 역할 분리 |
| v2 | PDF 길어질수록 AI 응답 지연·비용 증가 | 컨텍스트 길이 상한 적용 |
| v3 | 결제 승인 시점 재고 동시 차감 | 비관적 락 + 낙관적 락 이중 적용 |
| v3 | JPA 1차 캐시와 MyBatis(raw SQL) 데이터 불일치 | `entityManager.clear()`로 동기화 |
| v3 | 도서·회원 하드 삭제 시 FK 제약 위반(ORA-02292) | Soft Delete 정책으로 전환 |
| v3(배포) | nginx 라우팅 누락, CI/CD 시크릿 누락, 카카오 Redirect URI 미등록 등 | 실제 운영 환경에서 로그 기반으로 원인 추적·해결 |
| v4 | "Flutter로 v3 전체를 다시 만든다"는 요청의 모순(Flutter는 서버를 못 만듦) | 백엔드는 v3 재사용, 클라이언트 계층만 재작성으로 범위 재정의 |

각 항목의 원인·해결 과정은 해당 버전 폴더의 README에 더 자세히 기록되어 있습니다.

---

## 📂 저장소 구조

```
THEBOOK/
├── project_1/   Spring MVC + MyBatis
├── project_2/   Spring Boot + Thymeleaf + AI RAG
├── project_3/   REST API + JWT + Next.js (배포 중)
│   ├── back/         Spring Boot
│   ├── front/         Next.js
│   └── dashboard/    Django 판매 통계 대시보드
├── project_4/   Flutter + SNS 게시판 + 통계 대시보드
│   ├── back/         Spring Boot (v3 기반 + 게시판 API)
│   ├── mobile/        Flutter
│   └── dashboard/    Django 판매 통계 대시보드
└── .github/workflows/deploy.yml   GitHub Actions CI/CD
```

---

## 🎥 시연 영상

- v1: https://www.youtube.com/watch?v=xN2kwFkq-PY
- v2: https://www.youtube.com/watch?v=IdoHBQfeH5Q
- v3: https://www.youtube.com/watch?v=eDxqr3u_LcY&t=9s · https://www.youtube.com/watch?v=eByYCYUFjTI&t=175s
- v4: ⏳ 배포 완료 후 촬영 예정
