# BookStore v5 — Django + Python 완전 재작성 + 판매 통계 대시보드

> v3(Spring Boot + Next.js)는 그대로 유지한 채, 백엔드·프론트엔드 전체를 Django + Python으로 완전히 새로 작성한 버전입니다.
> pandas 기반 판매 통계 대시보드를 신규 기능으로 추가했습니다.

## 다른 버전 보기
| 버전 | 설명 | 링크 |
|---|---|---|
| v1 | Spring MVC + MyBatis + JSP | [project_1](../project_1) |
| v2 | Spring Boot + Thymeleaf + OAuth2 + AI RAG | [project_2](../project_2) |
| v3 | REST API + JWT + Next.js + 카카오페이 (배포됨) | [project_3](../project_3) |
| v4 | Flutter 모바일 앱 | [project_4](../project_4) |
| v5 (현재) | Django + Python 재작성 + 통계 대시보드 | - |

## GitHub
https://github.com/HyeonMi411/THEBOOK/tree/main/project_5

## 구성
```
project_5/
├── accounts/   회원가입·로그인(JWT)·이메일인증·탈퇴
├── books/      도서 CRUD·검색·베스트셀러·국립중앙도서관/카카오검색
├── cart/       장바구니
├── orders/     주문 + 카카오페이 결제(ready/approve/cancel/fail)
├── notices/    공지사항 게시판
├── analytics/  판매 통계 대시보드 (신규, 관리자 전용)
└── templates/  Django Template 프론트엔드
```

## 개발 환경
- Backend: Python, Django, Django REST Framework, djangorestframework-simplejwt
- Frontend: Django Template (백엔드와 같은 서버에서 직접 서빙)
- Analytics: pandas
- Others: JWT, 카카오페이 결제 API, Gmail SMTP(이메일 인증)

## 로컬 실행
```bash
cd project_5
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python manage.py migrate
python manage.py createsuperuser
python manage.py runserver
```

## 주요 기능
1. 회원가입/로그인 — JWT(SimpleJWT) 인증
2. 도서 관리 — CRUD·검색·베스트셀러(Django cache 프레임워크 기반, 배포 시 Redis 전환 가능)
3. 장바구니/주문/카카오페이 결제
4. 공지사항 게시판
5. **판매 통계 대시보드(신규)** — 카테고리별 매출·판매량, 일자별 트렌드를 pandas로 집계해 Chart.js로 시각화

## 담당 업무 및 성과

**v3에서 겪은 배포 문제들을 설계 단계부터 선반영**
v3 배포 과정에서 실제로 겪었던 하드코딩·경로 누락 문제들을 정리해, v5는 처음부터 이를 방지하도록 설계했습니다.

| v3에서 겪은 문제 | v5에서 미리 막은 방법 |
|---|---|
| `localhost:8080`/`3000` 하드코딩 | 프론트가 백엔드와 같은 서버(Django Template)라 애초에 분리된 도메인이 불필요 |
| `FRONTEND_BASE_URL` 등 하드코딩 | 전부 `.env` 로만 주입되도록 강제 |
| 탈퇴 계정인데 "비밀번호 불일치"로 표시됨 | 로그인 로직에서 탈퇴 여부를 비밀번호 검증보다 먼저 체크 |
| 결제 시 재고 동시성 문제 | `select_for_update()`로 결제 승인 시점에만 락을 걸고 차감 |
| 도서/회원 하드삭제 시 FK 위반 | 처음부터 소프트 삭제(`deleted` 플래그)만 사용 |
| CI/CD 시크릿 누락 반복 | `deploy.yml`에 필요한 시크릿 전부와 수동 실행(`workflow_dispatch`) 처음부터 포함 |

→ 성과: 같은 실수를 다른 프레임워크에서 반복하지 않도록, 문제와 해결책을 "프로젝트에 종속된 지식"이 아니라 "재사용 가능한 체크리스트"로 정리하는 경험을 얻음

**pandas 기반 통계 기능 신규 구현**
결제완료(PAID) 주문 데이터를 pandas DataFrame으로 변환해 카테고리별 집계·점유율·일자별 피벗 테이블을 계산하고, Chart.js로 시각화했습니다.
→ 성과: 실제 서비스 데이터를 데이터 분석 도구로 가공해 의미 있는 지표로 보여주는 경험 확보

## 검증한 내용
`python manage.py check`, `makemigrations`/`migrate` 통과는 물론, 실제 서버를 띄워서 이메일 인증 발송 → 인증확인 → 회원가입 → 로그인(JWT 발급) → 관리자 권한 승격 → 도서 등록 → 장바구니 담기 → 주문 생성까지 전체 흐름을 curl로 직접 실행해 확인했습니다.

## 이번 재작성에서 의도적으로 단순화한 부분
- OAuth2 소셜로그인(구글/카카오/네이버): 모델 필드는 준비, 실제 인가 흐름은 미구현
- AI 문서 챗봇(RAG): 설정만 준비, 엔드포인트 미구현
- Redis: Django cache 프레임워크로 구조만 마련(배포 시 설정 변경으로 연결 가능)
- 이미지 업로드 보안 검증(확장자/MIME): v3에 있던 로직 아직 미이식

## 프로젝트 소감
같은 서비스를 다른 언어·프레임워크로 다시 만들어보면서, "이 프레임워크가 원래 이렇게 동작한다"고 생각했던 것들이 실은 이전 프로젝트에서 겪은 문제를 해결하며 얻은 개별적인 지식이었다는 걸 깨달았습니다. Django로 옮기며 그 지식들을 처음부터 설계에 반영해보니, 문제 해결 경험이 특정 기술에 묶인 지식이 아니라 재사용 가능한 원칙으로 남는다는 것을 체감했습니다.
