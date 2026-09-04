# 포트폴리오 종합 점검 및 평가 — BookStore (boot3 + front3)

> 이 문서는 실제 프로젝트 코드를 근거로 작성했습니다(2차 개정판 — 이메일 인증, IDOR 방지, 이미지 업로드 검증, 회원 탈퇴 소프트삭제 반영). 각 답변에는 참고한 파일의 정확한 경로를 표기했고, 구현되지 않은 부분은 "미구현"으로 명확히 밝히고 개선 방향을 제시했습니다.

**프로젝트 구조**
- 백엔드: `boot3/src/main/java/com/thejoa703/` (Spring Boot 3, Java, Oracle)
- 프론트엔드: `front3/` (Next.js, Redux Toolkit + Redux-Saga)

---

## 1. 포트폴리오 종합 점검 및 평가 

### 프로젝트 총평

**한 줄 요약**: 도서 쇼핑몰(회원가입/이메일인증/로그인/OAuth2/도서 CRUD/장바구니/카카오페이 결제/공지사항)을 Spring Boot 3 + Next.js로 구현한 풀스택 프로젝트. 데이터 접근 계층을 도메인 특성에 따라 JPA(Spring Data)와 MyBatis로 의도적으로 분리한 것이 구조적 특징이며, 개발 과정에서 발견한 보안 취약점(IDOR, 파일 업로드 검증 미비, 계정 중복 등)을 실제로 찾아 수정한 이력이 있습니다.

**잘 설계된 부분**
1. **데이터 접근 계층의 하이브리드 설계** — 단순 CRUD(회원/장바구니/주문)는 Spring Data JPA `Repository`로, JOIN·동적 검색·페이징이 복잡한 도메인(도서 검색, 공지사항)은 MyBatis `Mapper`로 분리했습니다. (`boot3/src/main/java/com/thejoa703/repository/`, `boot3/src/main/java/com/thejoa703/mapper/`)
2. **동시성 제어의 이중 방어** — 재고 차감에 비관적 락(`BookStockRepository.findByIdForUpdate`)과 낙관적 락(`BookStock.version`, `@Version`)을 함께 적용했습니다.
3. **소프트 삭제를 도서·회원 양쪽에 일관되게 적용** — 도서(`Book.deleted`)뿐 아니라 회원 탈퇴(`AppUser.deleted`)도 하드 삭제 시 FK 제약 위반이 나는 문제를 직접 겪고 나서 같은 패턴으로 통일했습니다. (`boot3/src/main/java/com/thejoa703/entity/Book.java`, `entity/AppUser.java`)
4. **회원가입 단계의 이메일 실소유 확인** — 로컬 회원가입은 6자리 인증번호를 이메일로 발송·검증해야만 가입이 완료되도록 했고, 소셜로그인도 같은 이메일이 다른 방법으로 이미 가입돼 있으면 신규가입 대신 안내하도록 별도로 검증합니다. (`boot3/src/main/java/com/thejoa703/service/EmailService.java`, `security/EmailVerificationStore.java`, `oauth2/OAuth2SuccessHandler.java`)
5. **본인 확인(IDOR 방지)** — 닉네임/프로필이미지 수정 API가 URL의 사용자ID와 로그인한 본인ID를 대조해서, 다른 사람 정보를 몰래 수정하는 시도를 403으로 차단합니다. (`boot3/src/main/java/com/thejoa703/controller/UserController.java`)
6. **이미지 업로드 3중 검증** — 확장자 화이트리스트, Content-Type 검증에 더해 `ImageIO`로 실제 이미지 디코딩까지 확인하고, 저장 파일명은 UUID로만 생성해 Path Traversal을 원천 차단했습니다. (`boot3/src/main/java/com/thejoa703/util/FileStorageService.java`)
7. **Silent Refresh 패턴** — Axios 인터셉터로 AccessToken 만료(401) 시 자동으로 RefreshToken을 이용해 재발급받는 흐름이 구현되어 있습니다. (`front3/api/axios.js`)

**향후 보완이 필요한 부분 (정직하게 밝히는 부분)**
1. Redis 캐시 조회 시 Redis 자체가 다운되는 상황에 대한 명시적 fallback(try-catch)이 없습니다 → Q3에서 상세 설명
2. 외부 API(카카오페이, 카카오/국립중앙도서관 도서검색, Gmail SMTP) 호출 실패 시 재시도(Retry) 로직이 없습니다 → Q6에서 상세 설명
3. 로그아웃이 AccessToken 블랙리스트 방식이 아니라 RefreshToken 삭제 방식입니다 → Q1에서 상세 설명
4. 실시간 검색에 `useCallback`은 쓰지 않고 `useRef` 기반 디바운스만 적용했습니다 → Q12에서 상세 설명
5. N+1 쿼리 최적화(`@EntityGraph`, `fetch join` 등)를 적용하지 않은 조회 지점이 있습니다 → Q5에서 상세 설명
6. 이메일 인증번호에 발송 재시도 제한(예: 1분 내 재요청 금지)이 없어, 이론적으로 짧은 시간에 반복 요청이 가능합니다 → Q1에서 상세 설명

### 향후 보완점 매핑 표

| 우선순위 | 항목 | 관련 파일 | 개선 방향 |
|---|---|---|---|
| 높음 | Redis 장애 Fallback | `service/BookService.java` | try-catch로 캐시 조회 실패 시 DB 직접 조회로 폴백 |
| 높음 | 외부 API Retry | `api/KakaoPayApiService.java`, `service/EmailService.java` | Spring Retry(`@Retryable`) 또는 수동 재시도 루프 도입 |
| 중간 | 이메일 인증 요청 속도 제한 | `controller/UserController.java` | Redis에 마지막 발송시각을 저장해 1분 내 재요청 차단 |
| 중간 | AccessToken 즉시 무효화 | `security/TokenStore.java` | Redis 블랙리스트(만료시간=남은 AccessToken 유효시간)로 확장 |
| 낮음 | N+1 쿼리 | `service/OrderService.java` 등 | `@EntityGraph` 또는 fetch join 적용 |
| 낮음 | `useCallback` 최적화 | `components/BookSearchBox.js` | 검색 핸들러를 `useCallback`으로 감싸 리렌더링 최소화 |

---

## 2. 백엔드 & 데이터베이스 (Q1 ~ Q8-1)

### Q1. 로그인 & Security/JWT (로그아웃 Blacklist 포함)

**참고 파일**
- `boot3/src/main/java/com/thejoa703/security/JwtProvider.java`
- `boot3/src/main/java/com/thejoa703/security/JwtAuthenticationFilter.java`
- `boot3/src/main/java/com/thejoa703/security/TokenStore.java`
- `boot3/src/main/java/com/thejoa703/security/EmailVerificationStore.java`
- `boot3/src/main/java/com/thejoa703/service/EmailService.java`
- `boot3/src/main/java/com/thejoa703/controller/UserController.java`
- `boot3/src/main/java/com/thejoa703/service/UserService.java`
- `boot3/src/main/java/com/thejoa703/oauth2/OAuth2SuccessHandler.java`
- `boot3/src/main/resources/application.yml`

**인증 흐름**
1. 로그인 성공 시 `JwtProvider.createAccessToken()`으로 AccessToken(만료 15분), `createRefreshToken()`으로 RefreshToken(만료 14일)을 발급합니다.
2. RefreshToken은 `TokenStore`(Redis)에 `refresh:{userId}` 키로 저장하고, 클라이언트에는 HttpOnly 쿠키로 내려줍니다.
3. 매 요청마다 `JwtAuthenticationFilter`가 `Authorization: Bearer {AccessToken}` 헤더를 파싱해서 `SecurityContext`에 인증 정보를 세팅합니다.

**회원가입 단계의 이메일 인증 — 실제로 겪은 문제와 해결**
처음에는 이메일/비밀번호만 있으면 누구나 가입이 가능했는데, 이 경우 **본인이 소유하지 않은 이메일 주소로도 가입이 가능**하다는 문제가 있었습니다. 소셜로그인은 구글/카카오/네이버가 이미 이메일 소유를 검증해주지만, 로컬 회원가입은 우리가 직접 확인하는 절차가 없었기 때문입니다.

```java
// UserController.java
@PostMapping("/email/send-code")
public ResponseEntity<Void> sendEmailVerificationCode(@RequestParam("email") String email) {
    String code = emailService.generateVerificationCode();       // SecureRandom 기반 6자리
    emailVerificationStore.saveCode(email, code, emailCodeExpSeconds); // Redis, 5분 TTL
    emailService.sendVerificationCode(email, code);               // Gmail SMTP 발송
    return ResponseEntity.ok().build();
}
```
인증번호 확인에 성공하면 Redis에 "이 이메일은 인증완료" 상태를 30분간 세워두고, `UserService.createUser()`가 local 가입일 때 이 상태를 확인하지 않으면 가입 자체를 막습니다. 가입이 끝나면 이 상태를 즉시 정리해서 재사용을 막습니다.

**계정 중복(같은 이메일, 다른 로그인 방법) 문제 — 실제로 겪은 문제**
소셜로그인 코드를 작성하다가, `findByEmailAndProvider(email, provider)`로만 신규회원 여부를 판단하면 **같은 이메일이 이미 local로 가입돼 있어도 provider가 다르면 다시 신규가입 처리되어 계정이 쪼개지는** 문제를 발견했습니다.
```java
// OAuth2SuccessHandler.java
var sameEmailUser = userService.findByEmail(userInfo.getEmail()); // provider 무관 조회
if (sameEmailUser.isPresent()) {
    response.sendRedirect(redirectUrl + "?error=email_already_exists&existingProvider=" + ...);
    return; // 신규가입 대신 안내
}
```

**로그아웃 처리 — 솔직한 답변**
이 프로젝트의 로그아웃은 **"AccessToken 블랙리스트" 방식이 아니라 "RefreshToken 삭제" 방식**입니다.
```java
// UserController.java - /auth/logout
if (refreshToken != null && !refreshToken.isBlank()) {
    var claims = jwtProvider.parse(refreshToken).getBody();
    tokenStore.deleteRefreshToken(claims.getSubject());   // Redis 에서 RefreshToken 삭제
}
```
로그아웃 시 서버는 Redis에 저장된 RefreshToken만 지웁니다. 이미 발급된 AccessToken은 자체 만료 시간(15분)이 될 때까지는 여전히 유효합니다.

**회원 탈퇴 — 실제로 겪은 버그**
회원 탈퇴 API(`DELETE /auth/me`) 자체는 이미 만들어져 있었는데, 코드를 다시 점검하다가 `appUserRepository.deleteById()`로 **하드 삭제**를 하고 있는 걸 발견했습니다. `AppUser`는 Book/Sboard2(작성자), Cart, Orders 등 여러 테이블이 FK로 참조하는 부모 행이라, 도서를 하나라도 등록했거나 주문 이력이 있는 계정이 탈퇴하면 FK 제약 위반(`ORA-02292`)이 날 수 있는 상태였습니다. `AppUser.deleted` 플래그를 이용한 소프트 삭제로 전환하고, `login()`과 소셜로그인 양쪽에 탈퇴 계정 로그인 차단 로직을 추가했습니다.

**면접 답변 예시**: "AccessToken 만료시간을 15분으로 짧게 잡아서 탈취 시 피해 범위를 최소화하는 방향으로 설계했습니다. 로그아웃 즉시 AccessToken을 완전히 무효화하려면 Redis에 블랙리스트를 등록하고 매 요청마다 확인하는 방식으로 확장할 수 있는데, 시간 관계상 이번 프로젝트에는 반영하지 못했습니다. 대신 이메일 인증, 계정 중복 방지, 회원탈퇴 시 소프트 삭제처럼 실제로 코드를 점검하며 발견한 문제들은 그때그때 찾아서 고쳤습니다."

---

### Q2. 카카오페이 결제 & 재고 관리

**참고 파일**
- `boot3/src/main/java/com/thejoa703/service/PaymentService.java`
- `boot3/src/main/java/com/thejoa703/api/KakaoPayApiService.java`
- `boot3/src/main/java/com/thejoa703/repository/BookStockRepository.java`

**결제 3단계 흐름**
1. **결제 준비(ready)**: 주문 항목의 재고를 "확인만" 합니다(차감하지 않음). 카카오페이 API를 호출해 `tid`와 결제창 URL을 받아 주문에 저장합니다.
2. **사용자 결제 진행**: 카카오페이 결제창에서 실제 결제.
3. **결제 승인(approve)**: 카카오페이 승인 API 호출이 성공한 시점에만 재고를 실제로 차감합니다.

```java
// PaymentService.java - approve()
for (OrderItem item : order.getItems()) {
    BookStock stock = bookStockRepository.findByIdForUpdate(item.getBook().getId()).orElseThrow(...); // 비관적 락
    if (stock.getStockQuantity() < item.getQuantity()) { throw new IllegalStateException(...); }
    stock.setStockQuantity(stock.getStockQuantity() - item.getQuantity());
    bookStockRepository.saveAndFlush(stock); // 낙관적 락(@Version) 충돌은 여기서 즉시 감지
}
```
"준비 시점"이 아니라 "승인 시점"에 차감하는 이유는, 결제 준비만 하고 결제창에서 이탈하는 사용자 때문에 재고가 미리 묶이는 걸 막기 위해서입니다.

---

### Q3. Redis 활용처 (토큰, 캐싱, 멱등성 & 다운 시 Fallback)

**참고 파일**
- `boot3/src/main/java/com/thejoa703/config/RedisConfig.java`
- `boot3/src/main/java/com/thejoa703/security/TokenStore.java`
- `boot3/src/main/java/com/thejoa703/security/EmailVerificationStore.java`
- `boot3/src/main/java/com/thejoa703/service/BookService.java`

이 프로젝트에서 Redis는 **세 가지 용도**로 쓰입니다.

**1) RefreshToken 저장소** — `refresh:{userId}` 키, TTL 14일.

**2) 이메일 인증번호/인증완료 상태 저장** — `email-verify-code:{email}`(TTL 5분), `email-verified:{email}`(TTL 30분). 인증번호는 확인 시도 시(성공/실패 무관) 즉시 삭제해서, 같은 코드로 여러 번 무차별 대입을 시도하지 못하게 막습니다.
```java
// EmailVerificationStore.java
public void deleteCode(String email) {
    stringRedisTemplate.delete(codeKey(email));
}
```

**3) 베스트셀러(판매량 TOP 10) 캐싱** — `RedisTemplate<String, Object>`, TTL 10분. 결제 승인이 성공할 때마다 명시적으로 캐시를 무효화합니다.

**멱등성(Idempotency) — 솔직한 답변**: Redis를 이용한 명시적인 멱등성 키 처리는 구현하지 않았습니다. 다만 결제 승인 로직 자체에 `order.getOrderStatus() == OrderStatus.PAID` 체크가 있어서, 승인 API가 중복 호출되어도 재승인을 시도하지 않는 정도의 방어는 되어 있습니다.

**Redis 다운 시 Fallback — 솔직한 답변**: **현재는 명시적인 fallback이 구현되어 있지 않습니다.** Redis 연결이 끊기면 베스트셀러 조회, 로그인(RefreshToken 저장), 이메일 인증 전부 그대로 예외가 나서 실패합니다.

**면접 답변 예시**: "Redis가 다운되면 로그인과 이메일 인증까지 영향을 받는 구조라, 실무라면 최소한 RefreshToken 저장 실패 시에도 AccessToken 발급 자체는 성공시키고 재로그인 빈도만 늘어나게 하는 등의 완화책을 고려했을 것 같습니다. 이번에는 시간 관계상 반영하지 못했습니다."

---

### Q4. 동시성 제어 & 비관적 락(`@Lock`, `FOR UPDATE` & Timeout)

**참고 파일**
- `boot3/src/main/java/com/thejoa703/repository/BookStockRepository.java`
- `boot3/src/main/java/com/thejoa703/entity/BookStock.java`
- `boot3/src/main/java/com/thejoa703/service/BookService.java`

**이중 방어 구조**

1) **비관적 락(Pessimistic Lock)** — 결제 승인 시점, 재고 행을 SELECT하면서 즉시 잠급니다.
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM BookStock s WHERE s.bookId = :bookId")
Optional<BookStock> findByIdForUpdate(@Param("bookId") Long bookId);
```

2) **낙관적 락(Optimistic Lock)** — `BookStock.version`에 `@Version`을 붙여 이중 방어합니다.

**@MapsId 연관관계에서 실제로 겪은 문제**: 재고를 관리자가 처음 등록할 때, `BookStock.book`이 `@MapsId`로 `Book`을 참조하는데, MyBatis로 조회한(JPA가 관리 안 하는) `Book` 객체를 그대로 연관관계에 넘기면 Hibernate가 이를 cascade persist 하려다 예외가 났습니다. 반대로 ID만 직접 세팅하면 `@MapsId`의 ID 생성 전략이 연관 엔티티 자체를 필요로 해서 또 다른 예외가 났습니다. `EntityManager.getReference()`로 "관리 대상 참조(프록시)"를 만들어 연관관계에 넘겨서 두 문제를 동시에 해결했습니다.
```java
// BookService.java
newStock.setBook(entityManager.getReference(Book.class, bookId));
```

**Timeout — 솔직한 답변**: 비관적 락에 별도의 락 대기 타임아웃을 명시적으로 지정하지 않았습니다. 실무에서는 `@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))` 같은 명시적 타임아웃을 추가하는 것이 좋습니다.

---

### Q5. DB 쿼리 최적화

**참고 파일**
- `boot3/src/main/resources/mapper/BookMapper.xml`
- `boot3/src/main/resources/mapper/OrderItemMapper.xml`
- `boot3/src/main/java/com/thejoa703/service/OrderService.java`

**적용한 최적화**
1. **N+1을 피하기 위한 명시적 LEFT JOIN** — 도서 목록 조회 시 작성자·재고 정보를 MyBatis `resultMap`의 `association`으로 한 번의 SELECT에 담습니다.
2. **집계는 DB에서 GROUP BY로 처리** — 베스트셀러 랭킹은 애플리케이션에서 합산하지 않고 DB에 GROUP BY + ORDER BY + FETCH FIRST로 위임합니다.
3. **페이징에 Oracle 네이티브 문법 사용** — `OFFSET ... ROWS FETCH NEXT ... ROWS ONLY`.

**미해결 N+1 — 솔직한 답변**: 주문 목록 페이징 조회(`OrderService.getMyOrders()`)에는 아직 N+1 패턴이 남아있습니다.
```java
Page<Orders> result = ordersRepository.findByUser_IdAndHiddenByUserFalseOrderByIdDesc(userId, pageable);
result.getContent().forEach(o -> o.setItems(orderItemRepository.findByOrder_Id(o.getId())));
// 주문 10건 = 주문목록 조회(1) + 각 주문의 상품목록 조회(10) = 총 11번 쿼리
```

---

### Q6. 외부 API 예외 처리 & Retry/Fallback

**참고 파일**
- `boot3/src/main/java/com/thejoa703/api/KakaoPayApiService.java`
- `boot3/src/main/java/com/thejoa703/api/ApiKakaoBook.java`
- `boot3/src/main/java/com/thejoa703/service/EmailService.java`
- `boot3/src/main/java/com/thejoa703/exception/GlobalExceptionHandler.java`

이 프로젝트가 호출하는 외부 API는 4가지입니다: 카카오페이 결제, 카카오 도서검색, 국립중앙도서관 도서검색, **Gmail SMTP(이메일 인증)**.

**현재 구현된 예외 처리**
```java
// KakaoPayApiService.java
} catch (RestClientException ex) {
    log.warn("카카오페이 API 호출 실패: {}", ex.getMessage());
    throw new IllegalStateException("결제 서비스 연결에 실패했습니다.", ex);
}
```

**Gmail SMTP 예외 처리에서 실제로 겪은 문제**: 이메일 인증 기능을 붙이는 과정에서, `GlobalExceptionHandler`에 `MailException`(SMTP 인증/연결 실패시 발생) 처리가 없어서 실제 원인(비밀번호 오류인지 연결 문제인지)이 전부 500 에러로 뭉개져 나오는 걸 발견했습니다. 프론트는 이 500을 그냥 "발송에 실패했습니다"로만 보여줘서, 실제 디버깅은 백엔드 콘솔의 스택트레이스(`jakarta.mail.AuthenticationFailedException` 등)를 직접 확인해야 했습니다.

**Retry — 솔직한 답변**: **재시도 로직은 구현되어 있지 않습니다.** 카카오페이/도서검색/이메일 발송 어디에도 `@Retryable`이나 수동 재시도 루프가 없습니다.

**Fallback 차이**: 카카오페이는 결제 특성상 실패 시 명확한 예외를 던져서 프론트가 사용자에게 알리도록 했고, 도서검색 API는 실패해도 빈 결과를 돌려줘서 화면이 깨지지 않도록 했습니다.

---

### Q7. JPA & 영속성 컨텍스트 1차 캐시

**참고 파일**
- `boot3/src/main/java/com/thejoa703/entity/BookStock.java`
- `boot3/src/main/java/com/thejoa703/service/BookService.java`
- `boot3/src/test/java/com/thejoa703/Boot2ApplicationTests_6_PaymentService.java`

**실제로 겪은 문제**: `Book`은 MyBatis로, `BookStock`은 JPA로 관리하는 하이브리드 구조에서, 통합 테스트(`@Transactional`이라 하나의 영속성 컨텍스트가 테스트 전체에 유지됨)에서 `CartItem.book`을 한 번 JPA로 로딩하면 그 `Book` 객체가 1차 캐시에 올라갑니다. 이후 `deleteBook()`(MyBatis, raw SQL로 DB만 직접 갱신)을 호출해도, 캐시된 `Book` 객체는 이 변경을 전혀 모릅니다.
```java
bookService.deleteBook(book.getId());  // MyBatis 로 DB 만 직접 갱신
entityManager.clear();                  // 1차 캐시를 비워야 이후 조회가 최신값을 읽음
```
이 문제는 실제 운영에서는 발생하지 않습니다(매 HTTP 요청마다 독립적인 영속성 컨텍스트가 생성되므로). 테스트에서만 나타나는 특성이라, `@Transactional` 통합테스트를 작성할 때는 서로 다른 데이터 접근 기술을 섞어 쓰면 이런 동기화 문제가 생길 수 있다는 걸 실제로 겪고 배웠습니다.

**@MapsId 연관관계 문제**는 Q4에서 상세히 다뤘습니다 — 이것도 JPA 영속성 컨텍스트가 "관리하는 엔티티"와 "관리하지 않는(detached) 엔티티"를 어떻게 다르게 취급하는지와 관련된 문제입니다.

---

### Q8. 소프트 삭제(Soft Delete)

**참고 파일**
- `boot3/src/main/java/com/thejoa703/entity/Book.java`
- `boot3/src/main/java/com/thejoa703/entity/AppUser.java`
- `boot3/src/main/java/com/thejoa703/entity/Orders.java` (`hiddenByUser`)
- `boot3/src/main/java/com/thejoa703/service/UserService.java`

**도서 삭제 — 도입 배경**: 처음엔 하드 삭제였는데, `CART_ITEM`/`ORDER_ITEMS`가 `BOOK_ID`를 FK로 참조하고 있어서 한 번이라도 팔렸거나 장바구니에 담긴 도서를 삭제하려 하면 FK 제약 위반(`ORA-02292`)으로 실패하는 문제가 있었습니다.
```java
@Column(name = "DELETED", nullable = false, columnDefinition = "NUMBER(1) DEFAULT 0")
private boolean deleted = false;
```

**회원 탈퇴 — 같은 패턴을 재발견해서 적용**: 회원 탈퇴 기능을 점검하다가, `AppUser`도 정확히 같은 이유(Book/Sboard2/Cart/Orders가 FK로 참조하는 부모 행)로 하드 삭제가 위험하다는 걸 뒤늦게 발견했습니다.
```java
// UserService.java
@Transactional
public void deleteById(Long userId) {
    AppUser user = appUserRepository.findById(userId).orElseThrow(...);
    user.setDeleted(true); // 하드 삭제 대신 플래그만 갱신 (더티체킹으로 자동 UPDATE)
}
```
탈퇴 후 재로그인을 막기 위해, `login()`과 소셜로그인 양쪽에 `if (Boolean.TRUE.equals(user.getDeleted())) throw ...` 체크를 추가했습니다.

**같은 패턴을 주문에도 적용**: 결제완료/취소/실패 상태의 주문은 `Orders.hiddenByUser` 플래그로 "숨기기"만 처리해서 회계·이력 기록을 보존합니다.

**면접 답변 예시**: "소프트 삭제를 처음부터 일관되게 설계한 게 아니라, 도서에서 먼저 FK 제약 위반을 겪고 도입했고, 나중에 회원 탈퇴 기능을 점검하다가 똑같은 문제가 있다는 걸 재발견해서 같은 패턴으로 통일했습니다. 이 경험으로 '삭제'라는 기능을 만들 때는 그 데이터를 참조하는 다른 테이블부터 먼저 확인해야 한다는 걸 체득했습니다."

---

### Q8-1. 이미지 업로드 용량 제한, 확장자(MIME Type) 검증 및 이미지 최적화

**참고 파일**
- `boot3/src/main/java/com/thejoa703/util/FileStorageService.java`
- `boot3/src/main/java/com/thejoa703/controller/UserController.java`
- `boot3/src/test/java/com/thejoa703/Boot2ApplicationTests_2_Service.java`

**이전에는 미구현이었으나, 실제로 구현했습니다.** 처음에는 Spring 전역 설정(`max-file-size: 10MB`)과 UUID 접두사만 붙이는 정도였는데, 코드를 다시 점검하면서 확장자·MIME 검증이 전혀 없어서 **이론적으로 실행파일도 확장자만 바꾸면 업로드 가능한 상태**였다는 걸 발견하고 아래처럼 보강했습니다.

**용도별로 검증을 분리**: 프로필 사진/도서 표지(순수 이미지 전용)와 공지사항 첨부파일(PDF 등 문서 첨부 포함)의 요구사항이 달라서, 메서드 자체를 나눴습니다.
```java
// FileStorageService.java
public String uploadImage(MultipartFile file) {
    validateCommon(file, MAX_IMAGE_SIZE_BYTES, "5MB");            // 1) 용량 제한
    String extension = extractExtension(file.getOriginalFilename());
    if (!IMAGE_EXTENSIONS.contains(extension)) { throw ... }       // 2) 확장자 화이트리스트
    if (!IMAGE_CONTENT_TYPES.contains(contentType)) { throw ... }  // 3) Content-Type 검증
    if (ImageIO.read(file.getInputStream()) == null) { throw ... } // 4) 실제 이미지 디코딩 검증
    return store(file, extension);
}
```

**Content-Type만으로는 부족한 이유**: `Content-Type` 헤더는 클라이언트(브라우저)가 보내는 값이라 얼마든지 위조할 수 있습니다. 그래서 확장자·MIME 검증을 통과해도, `ImageIO.read()`로 실제 바이트가 이미지로 디코딩되는지 한 번 더 확인해서 확장자만 `.jpg`로 바꾼 실행파일 등을 걸러냅니다.

**Path Traversal 방지**: 원본 파일명을 저장에 전혀 사용하지 않고 `UUID + 검증된 확장자`로만 새 파일명을 만듭니다. 파일명에 `../`가 섞여도 애초에 그 문자열 자체를 안 쓰므로 uploads 폴더 바깥으로 저장될 수 없습니다.

**테스트에서 실제로 걸렸던 부작용**: 이 검증을 추가하고 나니, 기존 테스트가 `"test image content".getBytes()`처럼 가짜 텍스트를 이미지 파일인 척 넣고 있어서 `ImageIO` 검증에서 실패하는 걸 발견했습니다. 실제 1x1 PNG 바이너리(Base64)로 교체해서 해결했습니다.

**이미지 최적화(리사이징/압축) — 솔직한 답변**: **아직 미구현입니다.** 업로드된 원본 파일 그대로 저장하고 있어서, 사용자가 고해상도 이미지를 올리면 그대로 서버에 저장되고 그대로 응답됩니다.

**면접 답변 예시**: "처음엔 용량 제한만 있었는데, 코드를 다시 보다가 확장자·MIME 검증이 전혀 없다는 걸 발견하고 실제로 구현했습니다. 이미지 최적화(Thumbnailator 등으로 업로드 시점 리사이징)까지는 반영하지 못했는데, 이걸 추가하면 저장 용량과 응답 속도를 함께 개선할 수 있을 것 같습니다."

---

## 3. 프론트엔드 & API/UI (Q9 ~ Q14)

### Q9. RESTful API 설계

**참고 파일**: `boot3/src/main/java/com/thejoa703/controller/BookController.java`, `controller/UserController.java`

**설계 원칙**: 자원(Resource) 중심 URL + HTTP 메서드로 행위(Verb) 표현.

| 메서드 | 경로 | 의미 |
|---|---|---|
| POST | `/auth/email/send-code` | 이메일 인증번호 발송 |
| POST | `/auth/email/verify-code` | 이메일 인증번호 확인 |
| POST | `/auth/signup` | 회원가입 |
| PATCH | `/auth/{userId}/nickname` | 닉네임 수정 (본인만) |
| PATCH | `/auth/{userId}/profile-image` | 프로필 이미지 수정 (본인만) |
| DELETE | `/auth/me` | 회원 탈퇴 |
| PATCH | `/api/books/{id}/stock` | 재고만 별도 수정 (하위 리소스처럼 취급) |

**본인 확인이 필요한 자원은 URL에 `{userId}`를 명시하고, 서버가 그 값을 검증**: `/auth/{userId}/nickname`처럼 URL에 대상 사용자ID를 명시하되, 실제로는 `Authentication`에서 꺼낸 로그인 사용자ID와 비교해서 다르면 403을 반환합니다. URL 설계와 별개로 서버측 권한 검증이 항상 최종 방어선이라는 걸 실제 버그(IDOR)를 겪으며 체감했습니다.

**DELETE가 실제로는 소프트 삭제인 경우**: `DELETE /auth/me`, `DELETE /api/books/{id}`는 HTTP 메서드는 DELETE지만 실제 구현은 플래그 갱신(소프트 삭제)입니다. 클라이언트 입장에서는 "삭제 요청"이라는 의미가 동일하게 전달되므로, 내부 구현이 하드 삭제든 소프트 삭제든 API 계약(Contract)은 그대로 유지하는 게 맞다고 판단했습니다.

---

### Q10. React/Next.js 상태 관리 & 컴포넌트 구조

**참고 파일**
- `front3/store/configureStore.js`
- `front3/reducers/index.js`, `front3/sagas/index.js`
- `front3/reducers/authReducer.js`, `front3/sagas/authSaga.js`

**상태 관리: Redux Toolkit + Redux-Saga**

도메인별로 reducer/saga 파일을 분리했습니다: `auth`(인증/회원가입/탈퇴), `book`(도서), `notice`(공지), `cart`(장바구니), `order`(주문/결제). `reducers/index.js`의 `combineReducers`로 합치고, `sagas/index.js`의 `rootSaga`가 각 도메인 saga를 `fork`로 병렬 실행합니다.

**로그아웃과 회원탈퇴를 같은 패턴으로 구현**: 회원 탈퇴 기능을 나중에 추가하면서, 이미 있던 로그아웃 saga(`logoutRequest/Success/Failure`)와 동일한 구조로 `withdrawRequest/Success/Failure`를 만들었습니다. API 호출 → 로컬 토큰 정리 → 로그인 페이지로 이동까지 흐름이 똑같아서, 기존 패턴을 그대로 재사용할 수 있었습니다.
```js
// authSaga.js
export function* withdraw(){
    try{
        yield call(withdrawApi);
        localStorage.removeItem("accessToken");
        Cookies.remove("accessToken");
        yield put(withdrawSuccess());
        window.location.href = "/login";
    }catch(err){
        yield put(withdrawFailure(err.response?.data?.message || err.message));
    }
}
```

**왜 Redux-Saga인가**: API 호출 후 성공/실패에 따라 후속 액션(예: 소셜 provider별로 다른 로그아웃 URL로 리다이렉트)을 분기하는 로직이 많아서, 제너레이터 기반으로 흐름을 명시적으로 표현할 수 있는 saga가 적합하다고 판단했습니다.

**컴포넌트 구조**: `pages/`는 라우팅 단위 페이지, `components/`는 여러 페이지에서 재사용되는 UI. 레이아웃(`AppLayout`)이 헤더/메뉴를 담당하고 `{children}`으로 각 페이지를 감싸는 구조입니다.

---

### Q11. Axios 통신 & Interceptor 토큰 재발급(Silent Refresh)

**참고 파일**: `front3/api/axios.js`

**요청 인터셉터**: 매 요청마다 `localStorage`의 AccessToken을 `Authorization: Bearer {token}` 헤더에 자동으로 실어 보냅니다.

**응답 인터셉터(Silent Refresh 핵심)**:
```js
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;
      try {
        const { data } = await api.post("/auth/refresh");
        localStorage.setItem("accessToken", data.accessToken);
        original.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(original);
      } catch (refreshErr) {
        localStorage.removeItem("accessToken");
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);
```

**핵심 포인트**
1. `original._retry` 플래그로 무한 루프를 방지합니다.
2. RefreshToken은 HttpOnly 쿠키(`withCredentials: true`)로 관리해서 JS에서 직접 접근할 수 없게 했고, AccessToken만 `localStorage`에 둡니다.
3. 재발급마저 실패하면 로그인 페이지로 강제 이동시킵니다.

---

### Q12. 실시간 검색 `useCallback`/`useRef` 최적화

**참고 파일**: `front3/components/BookSearchBox.js`

**솔직한 답변**: 이 컴포넌트는 **`useRef`는 사용하지만 `useCallback`은 사용하지 않습니다.**
```js
const debounceRef = useRef(null);  // 디바운스 타이머 ID 저장용
const handleChange = (e) => {
  const value = e.target.value;
  setKeyword(value);
  if (debounceRef.current) clearTimeout(debounceRef.current);
  debounceRef.current = setTimeout(async () => {
    const res = await api.get('/api/books/search', { params: { keyword: value.trim() } });
    setResults(res.data || []);
  }, 250);
};
```
디바운스 타이머 ID는 리렌더링을 유발할 필요가 없는 값이라 `useState`가 아니라 `useRef`로 관리합니다. `useCallback`은 이 핸들러를 자식 컴포넌트에 props로 넘기지 않는 단순 구조라 적용하지 않았습니다.

---

### Q13. 반응형 UI & Mobile Drawer

**참고 파일**: `front3/components/AppLayout.js`

**반응형 브레이크포인트 설계**: Ant Design의 `Col` `xs/sm/md/lg` 반응형 속성을 사용합니다.

**실제로 겪었던 버그와 해결**: 처음엔 브레이크포인트를 `md`(768px) 기준으로 잡았는데, 태블릿 폭에서 검색창+가로메뉴를 한 줄에 담을 공간이 부족해서 헤더가 2줄로 보이는 문제가 있었습니다. 기준을 `lg`(992px)로 올리고, `Row`에 `wrap={false}`를 추가해서 이중으로 방어했습니다.

**Mobile Drawer**: `lg` 미만에서는 햄버거 버튼을 눌러 우측에서 슬라이드되는 `antd Drawer`가 열리고, 그 안에 검색창과 세로 메뉴를 함께 배치했습니다.

---

### Q14. UI 단 권한 분기 처리

**참고 파일**
- `front3/components/AppLayout.js`
- `front3/components/AccessDenied.js`
- `front3/pages/mypage.js`

**세 단계로 분기**

1) **메뉴/버튼 노출 여부** — `state.auth.user.role`, 로그인 여부에 따라 다른 메뉴 항목을 렌더링합니다.

2) **페이지 자체 접근 차단** — 관리자 전용 페이지는 권한이 없으면 폼 대신 `AccessDenied` 컴포넌트를 보여줍니다.

3) **위험한 동작에는 확인 절차 추가** — 회원 탈퇴처럼 되돌릴 수 없는 동작은 버튼 클릭만으로 바로 실행되지 않도록 `Popconfirm`으로 한 번 더 확인시킵니다.
```jsx
// mypage.js
<Popconfirm
    title="정말 탈퇴하시겠습니까?"
    description="탈퇴 후에는 로그인할 수 없으며, 되돌릴 수 없습니다."
    okButtonProps={{ danger: true }}
    onConfirm={() => dispatch(withdrawRequest())}
>
    <Button danger>회원 탈퇴</Button>
</Popconfirm>
```

**중요한 원칙**: 프론트 단 분기는 어디까지나 **UX 편의**이지 **진짜 보안 경계가 아닙니다.** 실제 권한 검증은 백엔드(`@PreAuthorize`, 본인 확인 로직)에서 이루어지므로, 프론트에서 우회해도 실제 API 호출은 서버에서 막힙니다. 실제로 닉네임/프로필이미지 수정 API에서 이 백엔드 검증이 빠져있던 걸 발견하고 나서(IDOR 취약점), "프론트 분기는 편의, 서버 검증은 필수"라는 원칙을 더 분명히 의식하게 됐습니다.

---

## 4. CS 및 백엔드/프론트엔드 웹 핵심 기본기 (Q15 ~ Q30)

### Q15. API 정의 및 역할

API(Application Programming Interface)는 서로 다른 소프트웨어가 정의된 규칙에 따라 요청과 응답을 주고받을 수 있게 해주는 약속입니다. 이 프로젝트에서는 프론트엔드(Next.js, `localhost:3000`)와 백엔드(Spring Boot, `localhost:8080`)가 서로 다른 서버에서 실행되는데, HTTP 기반 REST API가 이 둘을 연결하는 유일한 통로입니다.

### Q16. RESTful API 설계 원칙

1. **자원(Resource)은 명사, URL은 자원을 가리키기만** — `/auth/{userId}/nickname`(o)
2. **행위는 HTTP 메서드로 표현** — GET/POST/PATCH/DELETE
3. **상태 코드를 의미에 맞게 사용** — 200/201/400/401/**403(권한 없음)**/404. 실제로 닉네임 수정 API에서 본인이 아닌 다른 사용자를 수정하려는 시도를 403으로 명확히 구분해서 응답하도록 구현했습니다(`UserController.java`).
4. **무상태(Stateless)** — 서버가 세션 대신 JWT로 매 요청을 독립적으로 인증합니다.
5. **계층 구조** — `/api/books/{id}/stock`처럼 상위-하위 관계를 URL 경로로 표현.

### Q17. JWT 구조 (Header, Payload, Signature)

JWT는 `.`으로 구분된 3부분(`xxxxx.yyyyy.zzzzz`)으로 구성되고 각각 Base64Url로 인코딩됩니다.
1. **Header**: 서명 알고리즘과 토큰 타입. `HS256`을 사용합니다.
2. **Payload(Claims)**: `iss`, `sub`(사용자 ID), `iat`, `exp`, 커스텀 클레임(`role` 등).
3. **Signature**: Header+Payload를 서버만 아는 비밀키(`JWT_SECRET`)로 서명한 값.

**주의할 점**: Payload는 암호화가 아니라 인코딩만 되어 있어서 누구나 디코딩해서 읽을 수 있습니다. 이 프로젝트는 Payload에 사용자ID/role만 담고 비밀번호는 절대 담지 않습니다.

### Q18. SSR vs CSR 차이점

- **CSR**: 서버는 빈 HTML+JS 번들만 내려주고 브라우저가 JS를 실행해 화면을 그립니다. 초기 로딩은 느리지만 이후 페이지 전환은 빠릅니다.
- **SSR**: 서버가 완성된 HTML을 렌더링해서 내려줍니다. 첫 화면이 빨리 보이고 SEO에 유리합니다.
- 이 프로젝트는 Next.js를 쓰지만, 로그인 여부에 따라 내용이 크게 달라지는 개인화된 화면(장바구니, 마이페이지)이 많아서 대부분 CSR 방식(Redux-Saga로 클라이언트에서 데이터를 가져오는)을 택했습니다.

### Q19. Spring IoC / DI

**IoC(제어의 역전)**: 객체를 개발자가 직접 `new`로 생성·관리하는 대신, 스프링 컨테이너가 대신 관리하는 원칙.
**DI(의존성 주입)**: 필요한 의존 객체를 외부(스프링 컨테이너)로부터 주입받는 구현 방법.

이 프로젝트는 생성자 주입(Lombok `@RequiredArgsConstructor`)을 일관되게 사용합니다.
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final AppUserRepository appUserRepository;
    private final EmailVerificationStore emailVerificationStore; // 나중에 이메일 인증 기능 추가 시,
                                                                    // final 필드만 추가하면 생성자가
                                                                    // 자동으로 갱신되는 걸 실감했습니다.
}
```
**생성자 주입을 선택한 이유**: 필드 주입은 순환 참조를 런타임에야 발견하지만, 생성자 주입은 조기 발견 가능하고 `final`로 불변성을 보장하며 테스트에서 mock을 직접 넣기도 쉽습니다.

### Q20. Database JOIN 종류 및 차이

- **INNER JOIN**: 조인 조건이 일치하는 행만 반환.
- **LEFT (OUTER) JOIN**: 왼쪽 테이블의 모든 행을 반환, 매칭 안 되면 NULL.
- **RIGHT / FULL OUTER JOIN**: 반대 방향 / 양쪽 다 포함.

이 프로젝트는 도서 목록에서 재고 정보를 LEFT JOIN 합니다(`BookMapper.xml`). 재고가 아직 등록 안 된 신규 도서도 목록에 나와야 하므로 INNER JOIN을 쓰면 안 됩니다.

### Q21. Database Subquery 개념 및 종류

서브쿼리는 하나의 SQL문 안에 포함된 또 다른 SELECT문입니다. **스칼라 서브쿼리**(단일 값), **인라인 뷰**(FROM절), **중첩 서브쿼리**(WHERE절의 `IN`/`EXISTS`)로 나뉩니다. 이 프로젝트의 더미데이터 SQL에서도 관리자 계정을 이메일로 찾아 도서의 `APP_USER_ID`에 채우는 스칼라 서브쿼리를 사용합니다.
```sql
(SELECT APP_USER_ID FROM APP_USER WHERE EMAIL = 'admin_test@thejoa703.com' AND ROWNUM = 1)
```

### Q22. DML 개념 및 DDL/DCL과의 차이

- **DML**: 데이터 조작. `SELECT`, `INSERT`, `UPDATE`, `DELETE`.
- **DDL**: 스키마 정의. `CREATE TABLE`, `ALTER TABLE`. 이 프로젝트는 Hibernate `ddl-auto`가 엔티티 기반으로 DDL을 자동 생성합니다.
- **DCL**: 권한 제어. `GRANT`, `REVOKE`. 이 프로젝트는 애플리케이션 레벨(`@PreAuthorize`, 본인확인 로직)에서 권한을 통제합니다.

### Q23. Cookie vs Session 차이 및 작동 방식

- **Cookie**: 클라이언트에 저장되는 key-value 데이터. 서버가 `Set-Cookie`로 내려주면 이후 요청마다 자동으로 실림.
- **Session**: 서버가 상태를 저장하고, 클라이언트에는 세션 ID만 쿠키로 내려줌(Stateful).

이 프로젝트는 세션을 쓰지 않고 JWT를 씁니다. 다만 **RefreshToken은 쿠키로** 내려줍니다 — AccessToken(짧은 수명, JS가 헤더에 실어 보냄)과 RefreshToken(긴 수명, HttpOnly 쿠키로 JS 접근 차단)의 역할을 분리했습니다.

### Q24. JWT vs Cookie/Session 인증 방식 비교

| | Session | JWT |
|---|---|---|
| 상태 | Stateful | Stateless |
| 확장성 | 서버 여러 대면 세션 공유 필요 | 서버 간 상태 공유 불필요 |
| 즉시 무효화 | 서버에서 세션 삭제하면 즉시 반영 | 자체적으로 불가 — 블랙리스트 등 별도 장치 필요 |
| 페이로드 노출 | 서버만 봄 | 클라이언트도 디코딩 가능 |

이 프로젝트는 JWT를 택했고, "즉시 무효화가 어렵다"는 단점은 AccessToken 만료시간을 짧게(15분) 잡는 것으로 완화했습니다.

### Q25. CORS 개념 및 해결 경험

**CORS**: 브라우저가 "현재 페이지의 출처와 다른 출처로의 요청"을 기본적으로 차단하는 정책을 완화해주는 메커니즘.

이 프로젝트는 프론트(`localhost:3000`)와 백엔드(`localhost:8080`)의 포트가 달라서 CORS 문제가 발생했습니다.
```java
configuration.setAllowedOrigins(List.of("http://localhost:3000"));
configuration.setAllowCredentials(true);  // 쿠키(RefreshToken) 포함 요청 허용
```
**실제로 겪었던 함정**: `allowCredentials(true)`로 설정하면 `allowedOrigins`에 `"*"`를 쓸 수 없습니다(브라우저 스펙상 credentials 모드에서는 와일드카드 출처가 금지). 그래서 정확한 출처를 명시했습니다.

### Q26. OAuth 2.0 동작 원리

OAuth 2.0은 사용자가 제3자 서비스에 비밀번호를 직접 넘기지 않고도 위임 인증을 받을 수 있게 하는 프로토콜입니다.

이 프로젝트의 흐름 (`OAuth2SuccessHandler.java`):
1. "카카오로 로그인" 클릭 → 카카오 인증 서버로 리다이렉트
2. 사용자가 로그인/동의 → 카카오가 인가 코드와 함께 우리 서버로 리다이렉트
3. Spring Security OAuth2 클라이언트가 이 코드로 액세스 토큰을 요청하고 사용자 정보를 가져옴
4. `OAuth2SuccessHandler`가 이 정보로 DB에 회원이 있는지 확인 → **같은 이메일이 다른 방법(local 등)으로 이미 가입되어 있는지도 함께 확인** → 있으면 안내, 완전 신규면 가입 확인 절차로 → 최종적으로 우리 서비스 자체의 JWT를 발급

**중요한 점**: 카카오/구글의 토큰과 우리 서비스의 JWT는 별개입니다. OAuth2는 "이 사람이 그 계정 소유자임을 확인"하는 데만 쓰이고, 그 이후 인증은 자체 발급 JWT로 이루어집니다.

### Q27. React State vs Props & 전역 상태 필요성

- **State**: 컴포넌트 내부에서 관리하는 변할 수 있는 데이터.
- **Props**: 부모가 자식에게 내려주는 읽기 전용 데이터.

**전역 상태(Redux)가 필요한 이유**: `user`(로그인 정보), `cart`(장바구니 개수)처럼 서로 무관해 보이는 여러 컴포넌트가 동시에 같은 데이터를 필요로 하는 경우, props로 계속 전달(props drilling)하면 유지보수가 어렵습니다. `AppLayout`(헤더), `pages/mypage.js`(회원정보/탈퇴), `pages/cart/index.js`가 모두 `state.auth`를 동시에 참조하는 게 전형적인 사례입니다.

### Q28. React Virtual DOM 동작 원리

실제 DOM 조작은 브라우저의 레이아웃 재계산·리페인트를 유발해 비용이 큽니다. React는 메모리상의 가벼운 Virtual DOM 트리를 유지합니다.
1. 상태가 바뀌면 새 Virtual DOM 트리를 만듭니다.
2. 이전 트리와 비교(Diffing)해서 달라진 부분만 찾습니다.
3. 달라진 부분만 실제 DOM에 최소한으로 반영합니다(Reconciliation).

### Q29. 객체지향 5대 원칙(SOLID 원칙)이란 무엇인가요?

- **S (단일 책임)**: `UserService`(비즈니스 로직)와 `AppUserRepository`(데이터 접근)를 분리.
- **O (개방-폐쇄)**: `FileStorageService`를 `uploadImage()`/`uploadDocument()`로 나눠서, 새로운 업로드 용도가 생겨도 기존 메서드를 안 건드리고 새 메서드만 추가할 수 있게 함.
- **L (리스코프 치환)**: 자식 클래스는 부모 클래스를 대체할 수 있어야 합니다.
- **I (인터페이스 분리)**: `BookStockRepository`, `CartRepository` 등을 하나로 합치지 않고 도메인별로 작게 나눔.
- **D (의존관계 역전)**: `UserService`가 `AppUserRepository`(인터페이스, 실제 구현은 Spring Data JPA가 런타임 생성)에 의존하는 구조.

### Q30. 객체지향 프로그래밍(OOP)의 4대 특성은 무엇인가요?

- **캡슐화**: 엔티티들이 필드를 `private`으로 감추고 `@Getter`/`@Setter`나 서비스 메서드를 통해서만 접근.
- **상속**: `OrderStatus`(enum)로 상태를 표현하는 등, 상태 전이가 단순해서 상속 대신 enum 방식을 택함.
- **다형성**: `JpaRepository<AppUser, Long>` 인터페이스를 Spring Data JPA가 런타임에 프록시로 구현.
- **추상화**: `UserService.deleteById()`를 호출하는 컨트롤러 입장에서는 내부적으로 소프트 삭제인지 어떤 SQL이 실행되는지 몰라도 되는 것.

---

## 부록. 참고 파일 전체 목록

**백엔드 (`boot3/src/main/java/com/thejoa703/`)**
```
security/JwtProvider.java, security/JwtAuthenticationFilter.java, security/TokenStore.java
security/EmailVerificationStore.java
oauth2/OAuth2SuccessHandler.java, oauth2/CustomOAuth2User.java
config/SecurityConfig.java, config/RedisConfig.java, config/SchemaAutoFixRunner.java
controller/BookController.java, controller/UserController.java
service/BookService.java, service/PaymentService.java, service/OrderService.java
service/UserService.java, service/EmailService.java
entity/Book.java, entity/BookStock.java, entity/AppUser.java, entity/Orders.java
repository/BookStockRepository.java, repository/AppUserRepository.java
mapper/BookMapper.java + resources/mapper/BookMapper.xml
mapper/OrderItemMapper.java + resources/mapper/OrderItemMapper.xml
api/KakaoPayApiService.java, api/ApiKakaoBook.java, api/NlBookApiService.java
util/FileStorageService.java
exception/GlobalExceptionHandler.java
resources/application.yml
```

**프론트엔드 (`front3/`)**
```
api/axios.js
store/configureStore.js, reducers/index.js, sagas/index.js
reducers/authReducer.js, sagas/authSaga.js
components/AppLayout.js, components/BookSearchBox.js, components/AccessDenied.js
pages/signup.js, pages/mypage.js, pages/login.js
pages/oauth2/callback.js
```

## 부록 B. 이번 개정에서 새로 반영한 것 (지난 버전 대비)

1. **Q1**: 이메일 인증(회원가입), 계정 중복(다른 provider) 확인, 회원 탈퇴 소프트 삭제 및 로그인 차단 추가
2. **Q3**: Redis 활용처에 이메일 인증번호/인증완료 상태 저장 추가
3. **Q4, Q7**: `@MapsId` 연관관계 문제와 영속성 컨텍스트 1차 캐시 문제를 실제 겪은 사례로 상세히 반영
4. **Q6**: Gmail SMTP 예외 처리 미비(원인 진단이 어려웠던 실제 경험) 추가
5. **Q8**: 소프트 삭제를 도서뿐 아니라 회원 탈퇴에도 동일하게 적용한 이력 추가
6. **Q8-1**: "미구현" → **실제 구현 완료**로 전면 수정 (확장자/MIME/실제이미지검증/Path Traversal 방지)
7. **Q9, Q10, Q14**: 본인확인(IDOR 방지), 회원탈퇴 UI/saga를 실제 구현 사례로 반영
8. **Q16, Q19, Q26, Q29**: CS 기본기 답변에 실제 프로젝트 사례(403 활용, DI, OAuth 계정중복확인, OCP)를 자연스럽게 연결

---

*이 문서는 업로드된 프로젝트 코드를 직접 읽고 확인한 내용을 근거로 작성했습니다. "미구현"으로 표시한 항목은 실제로 코드에 없는 것을 확인한 것이며, 실제 면접에서는 이 문서의 "면접 답변 예시"를 참고하되 본인의 언어로 재구성해서 답하는 것을 권장합니다.*
