# BookStore 프로젝트 실행 가이드 (boot3 + front3)

이 문서는 백엔드(`boot3`, Spring Boot)와 프론트엔드(`front3`, Next.js)를 처음부터 로컬에서 실행하는 방법을 정리한 것입니다. 실제 프로젝트 설정 파일을 확인해서 작성했습니다.

---

## 0. 사전 준비물 (설치 필요)

| 항목 | 버전 | 확인 방법 |
|---|---|---|
| Java (JDK) | 17 | `java -version` |
| Node.js | 18 이상 권장 | `node -v` |
| Oracle Database | XE(Express Edition) | - |
| Redis | 6 이상 | `redis-cli ping` → `PONG` |
| Gradle | 프로젝트에 내장된 `gradlew` 사용 (별도 설치 불필요) | - |

**왜 Oracle과 Redis가 필요한가**: 이 프로젝트는 데이터를 Oracle DB에 저장하고(`application.yml`의 `jdbc:oracle:thin:@localhost:1521/XE`), RefreshToken 저장과 베스트셀러 캐싱에 Redis를 사용합니다 (`boot3/src/main/java/com/thejoa703/config/RedisConfig.java`). 둘 다 없으면 백엔드 서버 자체가 기동되지 않습니다.

---

## 1. Redis 실행 (Docker 권장)

```bash
docker run -d --name my-redis -p 6379:6379 redis
```

**동작 확인**
```bash
docker exec -it my-redis redis-cli
> ping
PONG
```

**전체 초기화가 필요할 때** (개발 중 캐시/토큰을 싹 비우고 싶을 때)
```bash
docker exec -it my-redis redis-cli FLUSHALL
```

---

## 2. Oracle Database 실행 (Docker 권장)

```bash
docker run -d --name oracle-xe \
  -p 1521:1521 \
  -e ORACLE_PASSWORD=<원하는_비밀번호> \
  gvenzl/oracle-xe:21-slim
```

컨테이너가 완전히 초기화될 때까지 1~2분 정도 걸립니다. 로그로 확인하세요.
```bash
docker logs -f oracle-xe
# "DATABASE IS READY TO USE!" 메시지가 뜨면 완료
```

**DB 계정 주의사항**: `application.yml`에 실제로 접속할 계정정보가 이미 다음과 같이 **하드코딩**되어 있습니다.
```yaml
# boot3/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:oracle:thin:@localhost:1521/XE
    username: boot   # ${DB_USERNAME} 대신 하드코딩된 값이 실제로 사용됩니다
    password: react  # ${DB_PASSWORD} 대신 하드코딩된 값이 실제로 사용됩니다
```
`.env` 파일에 `DB_USERNAME`/`DB_PASSWORD`를 넣어도 **현재는 무시되고 위 하드코딩 값(`boot`/`react`)이 그대로 사용됩니다.** Oracle에 `boot`라는 사용자 계정을 미리 만들어두거나(권장), 다른 계정을 쓰고 싶다면 `application.yml`의 `username`/`password` 줄을 직접 고쳐서 원하는 계정으로 바꾸세요.

**Oracle에 `boot` 계정 만들기 예시** (컨테이너 접속 후 SQL*Plus에서)
```bash
docker exec -it oracle-xe sqlplus system/<컨테이너에_설정한_비밀번호>@XE
```
```sql
CREATE USER boot IDENTIFIED BY react;
GRANT DBA TO boot;
EXIT;
```

---

## 3. 백엔드 환경변수 설정 (`.env` 파일)

`boot3/` 프로젝트 루트(= `build.gradle`이 있는 위치)에 `.env` 파일을 만드세요. `application.yml`의 `spring.config.import`가 이 파일을 자동으로 읽습니다.

```env
# JWT 서명키 (아무 문자열이나 32자 이상 권장, 외부에 노출되면 안 됨)
JWT_SECRET=여기에_충분히_긴_랜덤_문자열을_넣으세요

# 구글 소셜로그인 (Google Cloud Console에서 발급)
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

# 카카오 소셜로그인 + 도서검색 API (카카오 디벨로퍼스, developers.kakao.com)
KAKAO_CLIENT_ID=

# 카카오페이 결제 (카카오페이 전용 개발자센터, developers.kakaopay.com — 일반 카카오 디벨로퍼스와는 별개 사이트!)
KAKAO_PAY_SECRET_KEY=

# 네이버 소셜로그인 (네이버 개발자센터)
NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=
```

**각 키를 발급받는 곳**
| 키 | 발급처 | 비고 |
|---|---|---|
| `GOOGLE_CLIENT_ID`/`SECRET` | console.cloud.google.com | OAuth 동의화면 + 사용자 인증정보 등록 |
| `KAKAO_CLIENT_ID` | developers.kakao.com | REST API 키 하나로 로그인+도서검색 둘 다 사용됨 |
| `KAKAO_PAY_SECRET_KEY` | developers.kakaopay.com | **일반 카카오 디벨로퍼스와 완전히 다른 사이트**입니다. 2024.01 이후 Admin Key는 사용 불가, 반드시 이 사이트에서 Secret Key(dev)를 새로 발급받아야 합니다 |
| `NAVER_CLIENT_ID`/`SECRET` | developers.naver.com | - |

**각 OAuth 앱에 등록해야 할 리다이렉트 URI**
```
http://localhost:8080/login/oauth2/code/google
http://localhost:8080/login/oauth2/code/kakao
http://localhost:8080/login/oauth2/code/naver
```

**국립중앙도서관 API 키는 선택사항**: `NL_API_KEY`를 안 넣으면 `application-oauth.yml`에 있는 기본 테스트 키가 자동으로 쓰입니다. 정상 동작하지 않으면 국립중앙도서관 오픈API 홈페이지에서 본인 명의로 새로 발급받아 `.env`에 `NL_API_KEY=`로 추가하세요.

---

## 4. 백엔드 실행 (`boot3`)

### 4-1. 최초 1회 — 스키마 생성 모드로 실행

프로젝트를 처음 내려받으면 `application.yml`의 `ddl-auto`가 다음과 같이 되어 있을 수 있습니다.
```yaml
ddl-auto: create-drop
```
`create-drop`은 **서버가 켜지고 꺼질 때마다 데이터베이스 전체를 지우고 새로 만드는 모드**입니다. 최초 1회 테이블 구조를 잡을 때만 쓰고, 그 다음엔 반드시 `update`로 바꿔야 데이터가 유지됩니다.

```bash
cd boot3
./gradlew bootRun
```
정상 기동 로그가 뜨면 `Ctrl+C`로 종료하고, `application.yml`을 열어 다음과 같이 고치세요.
```yaml
ddl-auto: update   # 이후부터는 데이터를 보존하며 부분 수정만 반영
```

### 4-2. 이후 실행 (평상시)

```bash
cd boot3
./gradlew bootRun
```

IDE(IntelliJ, Eclipse 등)를 쓴다면 `Boot3Application.java`를 직접 Run 해도 됩니다.

**기동 확인**: 콘솔에 `Tomcat started on port 8080`이 뜨면 성공입니다.

**Swagger로 API 직접 테스트**: 브라우저에서 아래 주소로 접속하면 모든 REST API를 문서로 보면서 바로 호출해볼 수 있습니다.
```
http://localhost:8080/swagger-ui/index.html
```

---

## 5. 프론트엔드 실행 (`front3`)

### 5-1. 의존성 설치 (최초 1회)

```bash
cd front3
npm install
```

### 5-2. 환경변수 (선택사항)

기본값으로 백엔드 주소가 `http://localhost:8080`으로 잡혀 있어서(`front3/api/axios.js`), 로컬에서 기본 설정 그대로 쓴다면 별도 설정이 필요 없습니다. 백엔드 주소를 바꾸고 싶다면 `front3/` 루트에 `.env.local` 파일을 만드세요.
```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

### 5-3. 개발 모드 실행

```bash
npm run dev
```
`http://localhost:3000` 으로 접속합니다.

### 5-4. 프로덕션 빌드로 실행 (배포 방식과 동일하게 확인하고 싶을 때)

```bash
npm run build
npm run start
```

### 5-5. 프론트엔드 테스트 실행

```bash
npm run test
```

---

## 6. 전체 기동 순서 요약 (매번 개발할 때)

```
1. Redis 컨테이너 실행 확인 (docker ps 로 my-redis 가 떠있는지)
2. Oracle 컨테이너 실행 확인 (docker ps 로 oracle-xe 가 떠있는지)
3. cd boot3 && ./gradlew bootRun      (8080 포트)
4. cd front3 && npm run dev            (3000 포트)
5. 브라우저에서 http://localhost:3000 접속
```

---

## 7. 자주 겪는 문제 (트러블슈팅)

| 증상 | 원인 | 해결 |
|---|---|---|
| 백엔드 기동 시 `Connection refused` (Oracle) | Oracle 컨테이너가 아직 초기화 중이거나 안 켜짐 | `docker logs -f oracle-xe`로 "READY" 확인 후 재시도 |
| 백엔드 기동 시 Redis 관련 예외 | Redis 컨테이너가 안 켜짐 | `docker start my-redis` 또는 위 1번 명령으로 재실행 |
| 로그인은 되는데 재시작하면 다 초기화됨 | `ddl-auto: create-drop`인 상태 그대로 계속 쓰는 중 | `application.yml`에서 `update`로 변경 (4-1 참고) |
| 소셜 로그인 버튼 클릭 시 에러 | `.env`에 해당 `CLIENT_ID`/`SECRET`이 비어있음, 또는 OAuth 앱에 리다이렉트 URI 등록 안 함 | 3번 표의 발급처에서 키 발급 + 리다이렉트 URI 정확히 등록 |
| 카카오페이 결제 시 `error_code:-1` | `KAKAO_PAY_SECRET_KEY`가 일반 카카오 디벨로퍼스 키이거나 비어있음 | developers.kakaopay.com(별도 사이트)에서 발급받은 키인지 확인 |
| 이미지 업로드 후 화면에 안 보임 | `uploads/` 폴더 권한 문제, 또는 서버 실행 위치가 매번 바뀜 | `boot3/uploads/` 폴더에 쓰기 권한 있는지 확인 |
| 프론트에서 CORS 에러 | 백엔드 포트를 8080이 아닌 다른 포트로 바꿈 | `SecurityConfig.java`의 `allowedOrigins`, `application.yml` 양쪽 다 확인 |

---

## 8. 주요 접속 주소 정리

| 용도 | 주소 |
|---|---|
| 프론트엔드 | http://localhost:3000 |
| 백엔드 API | http://localhost:8080 |
| API 문서(Swagger) | http://localhost:8080/swagger-ui/index.html |
| OAuth2 콜백(프론트) | http://localhost:3000/oauth2/callback |
