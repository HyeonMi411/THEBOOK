# 포트폴리오 종합 점검 및 평가 — BookStore (boot3 + front3)

> 이 문서는 실제 프로젝트 코드를 근거로 작성했습니다. 각 답변에는 참고한 파일의 정확한 경로를 표기했고, 구현되지 않은 부분은 "미구현"으로 명확히 밝히고 개선 방향을 제시했습니다. 면접에서 "이 부분은 실제로 구현하지 않았고, 시간이 있다면 이렇게 개선하고 싶다"고 정직하게 답하는 것이 유리합니다.

**프로젝트 구조**
- 백엔드: `boot3/src/main/java/com/thejoa703/` (Spring Boot 3, Java, Oracle)
- 프론트엔드: `front3/` (Next.js, Redux Toolkit + Redux-Saga)

---

## 1. 포트폴리오 종합 점검 및 평가

### 프로젝트 총평

**한 줄 요약**: 도서 쇼핑몰(회원가입/로그인/OAuth2/도서 CRUD/장바구니/카카오페이 결제/공지사항)을 Spring Boot 3 + Next.js로 구현한 풀스택 프로젝트. 데이터 접근 계층을 도메인 특성에 따라 JPA(Spring Data)와 MyBatis로 의도적으로 분리한 것이 구조적 특징입니다.

**잘 설계된 부분**
1. **데이터 접근 계층의 하이브리드 설계** — 단순 CRUD(회원/장바구니/주문)는 Spring Data JPA `Repository`로, JOIN·동적 검색·페이징이 복잡한 도메인(도서 검색, 공지사항)은 MyBatis `Mapper`로 분리했습니다. (`boot3/src/main/java/com/thejoa703/repository/`, `boot3/src/main/java/com/thejoa703/mapper/`)
2. **동시성 제어의 이중 방어** — 재고 차감에 비관적 락(`BookStockRepository.findByIdForUpdate`)과 낙관적 락(`BookStock.version`, `@Version`)을 함께 적용했습니다.
3. **소프트 삭제 도입 배경이 명확함** — 도서를 하드 삭제하면 이미 주문/장바구니에 담긴 이력의 FK가 깨지는 문제를 인지하고, `Book.deleted` 플래그로 전환했습니다. (`boot3/src/main/java/com/thejoa703/entity/Book.java`)
4. **Silent Refresh 패턴** — Axios 인터셉터로 AccessToken 만료(401) 시 자동으로 RefreshToken을 이용해 재발급받는 흐름이 구현되어 있습니다. (`front3/api/axios.js`)
5. **결제-재고 트랜잭션 순서** — "결제 준비" 단계에서는 재고를 확인만 하고, 실제 "결제 승인"이 성공한 시점에만 재고를 차감합니다. (`boot3/src/main/java/com/thejoa703/service/PaymentService.java`)

**향후 보완이 필요한 부분 (정직하게 밝히는 부분)**
1. Redis 캐시 조회 시 Redis 자체가 다운되는 상황에 대한 명시적 fallback(try-catch)이 없습니다 → Q3에서 상세 설명
2. 외부 API(카카오페이, 카카오/국립중앙도서관 도서검색) 호출 실패 시 재시도(Retry) 로직이 없습니다 → Q6에서 상세 설명
3. 이미지 업로드 시 확장자/MIME 타입 검증과 리사이징(최적화)이 없습니다 → Q8-1에서 상세 설명
4. 로그아웃이 AccessToken 블랙리스트 방식이 아니라 RefreshToken 삭제 방식입니다 → Q1에서 상세 설명
5. 실시간 검색에 `useCallback`은 쓰지 않고 `useRef` 기반 디바운스만 적용했습니다 → Q12에서 상세 설명
6. N+1 쿼리 최적화(`@EntityGraph`, `fetch join` 등)를 적용하지 않은 조회 지점이 있습니다 → Q5에서 상세 설명

### 향후 보완점 매핑 표

| 우선순위 | 항목 | 관련 파일 | 개선 방향 |
|---|---|---|---|
| 높음 | Redis 장애 Fallback | `service/BookService.java` | try-catch로 캐시 조회 실패 시 DB 직접 조회로 폴백 |
| 높음 | 외부 API Retry | `api/KakaoPayApiService.java`, `api/ApiKakaoBook.java` | Spring Retry(`@Retryable`) 또는 수동 재시도 루프 도입 |
| 중간 | 이미지 업로드 검증 | `util/FileStorageService.java` | 확장자 화이트리스트, MIME 검증, Thumbnailator 등으로 리사이징 |
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
- `boot3/src/main/java/com/thejoa703/controller/UserController.java` (`/auth/logout`)
- `boot3/src/main/resources/application.yml` (jwt 설정)

**인증 흐름**
1. 로그인 성공 시 `JwtProvider.createAccessToken()`으로 AccessToken(만료 15분, `access-token-exp-seconds: 900`), `createRefreshToken()`으로 RefreshToken(만료 14일, `refresh-token-exp-seconds: 1209600`)을 발급합니다.
2. RefreshToken은 `TokenStore`(Redis, `StringRedisTemplate`)에 `refresh:{userId}` 키로 저장하고, 클라이언트에는 HttpOnly 쿠키로 내려줍니다.
3. 매 요청마다 `JwtAuthenticationFilter`가 `Authorization: Bearer {AccessToken}` 헤더를 파싱해서 `SecurityContext`에 인증 정보를 세팅합니다.
4. AccessToken이 만료되면 프론트(`front3/api/axios.js`)가 401을 받아 `/auth/refresh`로 재발급을 요청하는 Silent Refresh 구조입니다 (Q11에서 상세).

**로그아웃 처리 — 솔직한 답변**
이 프로젝트의 로그아웃은 **"AccessToken 블랙리스트" 방식이 아니라 "RefreshToken 삭제" 방식**입니다.

```java
// UserController.java - /auth/logout
if (refreshToken != null && !refreshToken.isBlank()) {
    var claims = jwtProvider.parse(refreshToken).getBody();
    String userId = claims.getSubject();
    tokenStore.deleteRefreshToken(userId);   // Redis 에서 RefreshToken 삭제
}
```

즉, 로그아웃 시 서버는 **Redis에 저장된 RefreshToken만 지웁니다.** 이미 발급된 AccessToken은 자체 만료 시간(15분)이 될 때까지는 여전히 유효합니다 — JWT는 서버가 상태를 들고 있지 않는(stateless) 토큰이라, "이 토큰을 지금 당장 무효화"하려면 별도의 블랙리스트가 필요한데 이 프로젝트에는 없습니다.

**면접 답변 예시**: "AccessToken 만료시간을 15분으로 짧게 잡아서 탈취 시 피해 범위를 최소화하는 방향으로 설계했습니다. 다만 로그아웃 즉시 AccessToken을 완전히 무효화하려면 Redis에 '만료된 토큰'을 블랙리스트로 등록하고, `JwtAuthenticationFilter`에서 매 요청마다 블랙리스트 존재 여부를 확인하는 방식으로 확장할 수 있습니다. 만료시간(TTL)은 그 AccessToken의 남은 유효시간만큼만 주면 되므로 Redis 부담도 크지 않습니다. 시간 관계상 이번 프로젝트에는 반영하지 못했습니다."

또한 로그아웃 코드에는 방어적 설계가 하나 더 있습니다: RefreshToken 쿠키가 없거나 파싱에 실패해도 로그아웃 요청 자체는 항상 200으로 성공시킵니다 (`UserController.java` 233번줄 근처 주석 참고). 로그아웃의 목적은 "클라이언트 상태를 지우는 것"이라, 서버측 Redis 삭제 실패가 로그아웃 자체를 막으면 안 된다는 판단입니다.

---

### Q2. 카카오페이 결제 & 재고 관리

**참고 파일**
- `boot3/src/main/java/com/thejoa703/service/PaymentService.java`
- `boot3/src/main/java/com/thejoa703/api/KakaoPayApiService.java`
- `boot3/src/main/java/com/thejoa703/repository/BookStockRepository.java`
- `boot3/src/main/java/com/thejoa703/entity/BookStock.java`

**결제 3단계 흐름**
1. **결제 준비(ready)**: 주문 항목의 재고를 "확인만" 합니다 (차감하지 않음). 카카오페이 API를 호출해 `tid`(결제 고유번호)와 결제창 URL을 받아 주문에 저장합니다.
2. **사용자 결제 진행**: 카카오페이 결제창에서 실제 결제(외부 서비스).
3. **결제 승인(approve)**: 카카오페이 승인 API 호출이 성공한 시점에만 재고를 실제로 차감합니다.

```java
// PaymentService.java - approve()
for (OrderItem item : order.getItems()) {
    BookStock stock = bookStockRepository.findByIdForUpdate(item.getBook().getId())  // 비관적 락
            .orElseThrow(...);
    if (stock.getStockQuantity() < item.getQuantity()) {
        throw new IllegalStateException(...);  // 재고부족
    }
    stock.setStockQuantity(stock.getStockQuantity() - item.getQuantity());
    bookStockRepository.saveAndFlush(stock);   // 낙관적 락(@Version) 충돌은 여기서 즉시 감지
}
order.setOrderStatus(OrderStatus.PAID);
```

**왜 "준비 시점"이 아니라 "승인 시점"에 차감하는가**: 결제 준비만 하고 실제로 결제창에서 이탈하는 사용자가 많은데, 준비 시점에 미리 재고를 차감하면 "결제 안 한 사람 때문에 재고가 묶이는" 문제가 생깁니다. 승인이 실제로 완료된 시점에만 차감해서 이 문제를 피했습니다.

**재고 관리의 동시성 제어**는 Q4에서 상세히 다룹니다.

---

### Q3. Redis 활용처 (토큰, 캐싱, 멱등성 & 다운 시 Fallback)

**참고 파일**
- `boot3/src/main/java/com/thejoa703/config/RedisConfig.java`
- `boot3/src/main/java/com/thejoa703/security/TokenStore.java`
- `boot3/src/main/java/com/thejoa703/service/BookService.java` (`getBestsellers`, `evictBestsellerCache`)

이 프로젝트에서 Redis는 **두 가지 용도**로 쓰입니다.

**1) RefreshToken 저장소** (`StringRedisTemplate`)
```java
// TokenStore.java
public void saveRefreshToken(String userId, String token, long ttlSeconds) {
    stringRedisTemplate.opsForValue().set("refresh:" + userId, token, ttlSeconds, TimeUnit.SECONDS);
}
```

**2) 베스트셀러(판매량 TOP 10) 캐싱** (`RedisTemplate<String, Object>`, JSON 직렬화)
```java
// BookService.java - getBestsellers()
List<BestsellerBookDto> cached = (List) redisTemplate.opsForValue().get(BESTSELLER_CACHE_KEY);
if (cached != null) { return cached; }              // 캐시 히트
List<Map<String, Object>> rows = orderItemMapper.findBestSellerBookIds(10);  // 캐시 미스 → DB 집계
... 
redisTemplate.opsForValue().set(BESTSELLER_CACHE_KEY, result, 600, TimeUnit.SECONDS);  // TTL 10분
```
캐시 무효화는 결제 승인이 성공할 때마다 명시적으로 호출합니다 (`PaymentService.approve()` → `bookService.evictBestsellerCache()`). TTL(10분)도 함께 걸어둬서, 무효화 호출을 놓치더라도 최대 10분 뒤에는 최신 데이터로 자동 갱신됩니다.

**멱등성(Idempotency) — 솔직한 답변**: 이 프로젝트에서 Redis를 이용한 명시적인 멱등성 키(idempotency key) 처리는 구현하지 않았습니다. 다만 결제 승인 로직(`PaymentService.approve()`) 자체에 `order.getOrderStatus() == OrderStatus.PAID` 체크가 있어서, 이미 승인된 주문에 대해 승인 API가 중복 호출되어도 카카오페이 재승인을 시도하지 않고 그대로 기존 결과를 반환하는 정도의 방어는 되어 있습니다. Redis를 활용한 요청 단위 멱등성 키 방식은 아닙니다.

**Redis 다운 시 Fallback — 솔직한 답변**: **현재는 명시적인 fallback이 구현되어 있지 않습니다.** `getBestsellers()`가 `redisTemplate.opsForValue().get()`을 호출하는 시점에 Redis 연결이 끊겨 있으면 예외가 그대로 전파되어 베스트셀러 조회 자체가 실패합니다.

**면접 답변 예시**: "현재는 Redis 장애 시 별도 처리가 없어서, Redis가 다운되면 베스트셀러 조회 API 자체가 실패합니다. 개선한다면 캐시 조회를 try-catch로 감싸서, Redis 예외 발생 시 DB에서 직접 집계하도록 폴백시키고, 로그로 장애를 남기는 방식으로 바꾸고 싶습니다. RefreshToken 저장(`TokenStore`)도 마찬가지로, Redis가 다운되면 로그인 자체가 막힐 수 있어서 이 부분도 개선 여지가 있습니다."

---

### Q4. 동시성 제어 & 비관적 락(`@Lock`, `FOR UPDATE` & Timeout)

**참고 파일**
- `boot3/src/main/java/com/thejoa703/repository/BookStockRepository.java`
- `boot3/src/main/java/com/thejoa703/entity/BookStock.java`
- `boot3/src/main/java/com/thejoa703/service/PaymentService.java`

**이중 방어 구조**

1) **비관적 락(Pessimistic Lock)** — 결제 승인 시점, 재고 행을 SELECT하면서 즉시 잠급니다.
```java
// BookStockRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM BookStock s WHERE s.bookId = :bookId")
Optional<BookStock> findByIdForUpdate(@Param("bookId") Long bookId);
```
이건 JPA `@Lock`을 통해 Hibernate가 Oracle의 `SELECT ... FOR UPDATE`를 생성하도록 하는 방식입니다. 같은 도서를 동시에 여러 명이 구매하려 할 때, 먼저 락을 잡은 트랜잭션이 재고를 확인·차감·커밋할 때까지 다른 트랜잭션은 대기합니다.

2) **낙관적 락(Optimistic Lock)** — `BookStock.version` 필드에 `@Version`을 붙여, 실제 UPDATE 시점에 버전이 바뀌었으면 `OptimisticLockException`이 발생하도록 이중으로 방어합니다.
```java
@Version
@Column(name = "VERSION", nullable = false)
private Long version;
```
```java
try {
    bookStockRepository.saveAndFlush(stock);  // 여기서 버전 충돌시 즉시 예외
} catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
    throw new IllegalStateException("재고 갱신 충돌이 발생했습니다. 다시 시도해주세요.");
}
```

**Timeout — 솔직한 답변**: 비관적 락에 별도의 락 대기 타임아웃(`@QueryHints`로 `javax.persistence.lock.timeout` 설정 등)을 명시적으로 지정하지는 않았습니다. Oracle/HikariCP의 기본 타임아웃 설정을 그대로 따르고 있습니다. 실무에서는 락 경합이 심한 상품(한정판 등)에서 무한 대기를 막기 위해 `@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))` 같은 명시적 타임아웃을 추가하는 것이 좋습니다.

**왜 두 락을 동시에 쓰는가**: 비관적 락만으로도 충분해 보이지만, 비관적 락이 걸리는 구간(결제 승인)과 다른 경로(관리자가 직접 재고를 수정하는 `BookService.updateStock()`)가 분리되어 있어서, 관리자 수정 경로에서도 최소한의 충돌 감지(낙관적 락)를 걸어둔 것입니다.

---

### Q5. DB 쿼리 최적화

**참고 파일**
- `boot3/src/main/resources/mapper/BookMapper.xml`
- `boot3/src/main/resources/mapper/Sboard2Mapper.xml`
- `boot3/src/main/resources/mapper/OrderItemMapper.xml`
- `boot3/src/main/java/com/thejoa703/service/OrderService.java`

**적용한 최적화**

1) **N+1을 피하기 위한 명시적 LEFT JOIN** — 도서 목록 조회 시 작성자(AppUser)와 재고(BookStock) 정보가 항상 함께 필요한데, 이를 각각 별도 쿼리로 조회하지 않고 MyBatis `resultMap`의 `association`으로 한 번의 SELECT에 담습니다.
```xml
<!-- BookMapper.xml -->
LEFT JOIN APP_USER u ON b.APP_USER_ID = u.APP_USER_ID
LEFT JOIN BOOK_STOCK s ON b.BOOK_ID = s.BOOK_ID
```
공지사항(Sboard2Mapper.xml)도 작성자 정보를 같은 방식으로 LEFT JOIN 합니다.

2) **집계는 DB에서 GROUP BY로 처리, 애플리케이션 레벨 합산 지양** — 베스트셀러 랭킹은 애플리케이션에서 주문 목록을 다 가져와 합산하지 않고, DB에 GROUP BY + ORDER BY + FETCH FIRST로 위임합니다.
```xml
<!-- OrderItemMapper.xml -->
SELECT oi.BOOK_ID, SUM(oi.QUANTITY) AS TOTAL_QTY
FROM ORDER_ITEMS oi JOIN ORDERS o ON oi.ORDER_ID = o.ID
WHERE o.ORDER_STATUS = 'PAID'
GROUP BY oi.BOOK_ID ORDER BY TOTAL_QTY DESC FETCH FIRST 10 ROWS ONLY
```

3) **페이징에 Oracle 네이티브 문법 사용** — `OFFSET ... ROWS FETCH NEXT ... ROWS ONLY`를 직접 사용해서, 애플리케이션에서 전체 목록을 가져온 뒤 자르는 방식(메모리 낭비)을 피했습니다.

**미해결 N+1 — 솔직한 답변**: 주문 목록을 페이징 조회하는 `OrderService.getMyOrders()`에는 아직 N+1 패턴이 남아있습니다.
```java
// OrderService.java
Page<Orders> result = ordersRepository.findByUser_IdAndHiddenByUserFalseOrderByIdDesc(userId, pageable);
result.getContent().forEach(o -> o.setItems(orderItemRepository.findByOrder_Id(o.getId())));
// 주문이 10건이면, 주문목록 조회(1번) + 각 주문의 상품목록 조회(10번) = 총 11번 쿼리
```
**면접 답변 예시**: "주문 목록 페이지에서 페이지당 항목 수가 적어서(기본 12건) 실사용에 큰 문제는 없었지만, 이상적으로는 `@EntityGraph(attributePaths = "items")`를 리포지토리 메서드에 붙이거나, `JOIN FETCH`를 쓰는 JPQL 쿼리로 한 번에 가져오도록 개선해야 합니다. 다만 페이징(`Pageable`)과 `fetch join`을 동시에 쓰면 Hibernate가 메모리에서 페이징하며 경고를 내는 이슈가 있어서, `@BatchSize`로 IN 절 배치 조회를 쓰는 방법도 고려할 수 있습니다."

---

### Q6. 외부 API 예외 처리 & Retry/Fallback

**참고 파일**
- `boot3/src/main/java/com/thejoa703/api/KakaoPayApiService.java`
- `boot3/src/main/java/com/thejoa703/api/ApiKakaoBook.java` (카카오 도서검색)
- `boot3/src/main/java/com/thejoa703/api/NlBookApiService.java` (국립중앙도서관)

이 프로젝트가 호출하는 외부 API는 3가지입니다: 카카오페이 결제, 카카오 도서검색, 국립중앙도서관 도서검색.

**현재 구현된 예외 처리**
```java
// KakaoPayApiService.java
} catch (RestClientException ex) {
    log.warn("카카오페이 API 호출 실패: {}", ex.getMessage());
    throw new IllegalStateException("결제 서비스 연결에 실패했습니다.", ex);
}
```
```java
// ApiKakaoBook.java / NlBookApiService.java
} catch (Exception e) {
    log.warn("카카오 도서검색 API 호출/파싱 중 오류: {}", e.getMessage());
    // 예외를 삼키고 빈 결과 반환 (검색 화면 자체가 죽지 않도록)
}
```

**Retry — 솔직한 답변**: **재시도 로직은 구현되어 있지 않습니다.** `@Retryable`(Spring Retry)이나 수동 재시도 루프가 없어서, 외부 API가 일시적 네트워크 오류로 실패하면 그 즉시 사용자에게 실패로 응답합니다.

**Fallback 차이**: 카카오페이는 결제라는 특성상 실패 시 명확한 예외를 던져서 프론트가 사용자에게 결제 실패를 알리도록 했고(재시도보다 명확한 실패 통지가 중요), 도서검색 API(카카오/국립중앙도서관)는 실패해도 검색 결과를 빈 배열로 돌려줘서 화면 자체가 깨지지 않도록 했습니다 — 이건 "결제처럼 중요한 것은 실패를 명확히, 검색처럼 부가기능은 관대하게"라는 판단입니다.

**면접 답변 예시**: "카카오페이 결제 API처럼 금전이 오가는 호출은 재시도가 오히려 중복결제 위험을 만들 수 있어서 신중해야 하고, 재시도한다면 멱등키(idempotency key)를 함께 설계해야 합니다. 도서검색 API처럼 조회성 호출은 Spring Retry의 `@Retryable(maxAttempts=3, backoff=@Backoff(delay=500))` 같은 방식으로 일시적 네트워크 오류를 자동 복구하도록 개선하고 싶습니다. 이번 프로젝트에서는 시간 관계상 로그만 남기고 즉시 실패/빈 결과로 처리했습니다."

---

### Q7. JPA & 영속성 컨텍스트 1차 캐시

**참고 파일**
- `boot3/src/main/java/com/thejoa703/entity/BookStock.java`
- `boot3/src/main/java/com/thejoa703/service/BookService.java`
- `boot3/src/test/java/com/thejoa703/Boot2ApplicationTests_6_PaymentService.java`

이 프로젝트는 하이브리드 구조(JPA + MyBatis)라서, **실제로 1차 캐시 때문에 발생한 버그를 겪고 고친 경험**이 있습니다. 면접에서 이 사례를 그대로 답변하면 좋습니다.

**실제로 겪은 문제**: `Book`은 MyBatis(`BookMapper`)로 관리하고, `BookStock`은 JPA(`BookStockRepository`)로 관리합니다. `BookStock.book`은 `@OneToOne @MapsId`로 `Book`을 참조하는데, 관리자가 재고를 처음 등록할 때 MyBatis로 조회한 `Book` 객체(JPA 입장에서는 "detached" 상태)를 그대로 연관관계에 넘기면 Hibernate가 이걸 cascade persist 하려다 `PersistentObjectException`(detached entity passed to persist)이 발생했습니다. 반대로 ID값만 직접 넣으면 `@MapsId`의 ID 생성 전략(`ForeignGenerator`)이 연관 엔티티(`book`)를 필요로 해서 또 다른 예외(`attempted to assign id from null one-to-one property`)가 났습니다.

**해결**: `EntityManager.getReference()`로 "이미 존재한다고 가정하는 관리 대상 참조(프록시)"를 만들어 연관관계에 넘겼습니다. DB를 다시 조회하지도 않고, detached도 아니라서 두 문제를 동시에 피할 수 있었습니다.
```java
// BookService.java - updateStock()
newStock.setBook(entityManager.getReference(Book.class, bookId));
```

**테스트에서 겪은 1차 캐시 문제**: 통합 테스트(`Boot2ApplicationTests_6_PaymentService.java`)는 클래스 레벨 `@Transactional`이라 테스트 시작부터 끝까지 하나의 영속성 컨텍스트를 씁니다. 그 안에서 `CartItem.book`(JPA 연관관계로 로딩된 `Book`)이 한 번 캐시되면, 이후 MyBatis(raw SQL)로 `Book.deleted`를 갱신해도 캐시된 `Book` 객체는 그 변경을 전혀 모릅니다.
```java
bookService.deleteBook(book.getId());  // MyBatis 로 DB 만 직접 갱신
entityManager.clear();                  // 1차 캐시를 비워야 이후 조회가 최신값을 읽음
```

**면접 답변 예시**: "영속성 컨텍스트(1차 캐시)는 같은 트랜잭션 안에서 동일 엔티티를 반복 조회할 때 SQL을 안 날리고 캐시된 객체를 재사용해주는 성능 이점이 있지만, 이 프로젝트처럼 MyBatis(캐시를 거치지 않는 raw SQL)와 JPA를 함께 쓰는 구조에서는 '캐시된 객체와 실제 DB 상태가 어긋나는' 문제가 생길 수 있다는 걸 실제로 겪었습니다. 해결책은 `EntityManager.clear()`/`refresh()`로 명시적으로 동기화하거나, 애초에 한 도메인은 하나의 데이터 접근 기술로 일관되게 관리하는 것입니다."

---

### Q8. 소프트 삭제(Soft Delete)

**참고 파일**
- `boot3/src/main/java/com/thejoa703/entity/Book.java`
- `boot3/src/main/java/com/thejoa703/entity/Orders.java` (`hiddenByUser`)
- `boot3/src/main/resources/mapper/BookMapper.xml`
- `boot3/src/main/java/com/thejoa703/service/CartService.java`, `OrderService.java`

**도입 배경**: 처음엔 `BookMapper.delete()`가 `DELETE FROM BOOK`으로 하드 삭제였는데, `CART_ITEM`/`ORDER_ITEMS`가 `BOOK_ID`를 FK로 참조하고 있어서 **한 번이라도 팔렸거나 누군가의 장바구니에 담긴 도서를 관리자가 삭제하려 하면 FK 제약 위반(ORA-02292)으로 삭제 자체가 실패**하는 문제가 있었습니다.

**구현**
```java
// Book.java
@Column(name = "DELETED", nullable = false, columnDefinition = "NUMBER(1) DEFAULT 0")
private boolean deleted = false;
```
```xml
<!-- BookMapper.xml -->
<update id="updateDeleted">
    UPDATE BOOK SET DELETED = #{deleted} WHERE BOOK_ID = #{bookId}
</update>
```
목록/검색/카테고리/제목중복확인 전부 `WHERE DELETED = 0` 조건이 붙어서, 삭제된 도서는 화면에 보이지 않지만 DB에는 그대로 남아 기존 주문 이력과의 FK는 유지됩니다.

**이미 장바구니에 담긴 사용자에 대한 처리**: 소프트 삭제된 도서가 이미 장바구니에 담겨 있으면, 장바구니 화면에는 계속 보이되 "판매중단" 표시를 하고 결제예정금액에서 자동 제외합니다. 수량을 늘리거나 새로 담는 것은 막되, 수량을 줄이거나 삭제하는 것은 항상 허용합니다.

**같은 패턴을 주문에도 적용**: 결제완료/취소/실패 상태의 주문은 실제로 삭제(DB에서 DELETE)하지 않고 `Orders.hiddenByUser` 플래그로 "숨기기"만 처리해서 회계·이력 기록을 보존합니다. 반면 아직 결제 전(PENDING) 주문은 실제 DELETE합니다 — 결제 이력이 없는 임시 데이터이기 때문입니다.

**면접 답변 예시**: "소프트 삭제를 처음부터 설계한 게 아니라, 실제로 FK 제약 위반 에러를 겪고 나서 도입했습니다. 이 경험으로 '삭제'라는 기능을 설계할 때는 그 데이터를 참조하는 다른 테이블이 있는지부터 먼저 파악해야 한다는 걸 배웠습니다."

---

### Q8-1. 이미지 업로드 용량 제한, 확장자(MIME Type) 검증 및 이미지 최적화

**참고 파일**
- `boot3/src/main/java/com/thejoa703/util/FileStorageService.java`
- `boot3/src/main/resources/application.yml` (multipart 설정)

**용량 제한 — Spring 전역 설정으로 구현됨**
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB
```

**확장자/MIME 타입 검증, 이미지 최적화 — 솔직한 답변**: **현재 미구현입니다.**
```java
// FileStorageService.java - 전체 검증 로직
public String upload(MultipartFile file) {
    if (!Files.exists(root)) { Files.createDirectories(root); }
    String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    return "uploads/" + filename;
}
```
파일명 충돌 방지(UUID 접두사)만 되어 있고, **확장자 화이트리스트 검사, `Content-Type`(MIME) 검사, 실제 이미지인지 매직바이트 검증, 리사이징/압축은 전혀 없습니다.** 이론적으로는 이미지가 아닌 실행파일도 확장자만 바꿔서 업로드할 수 있는 상태입니다.

**면접 답변 예시**: "현재는 파일 크기 제한(Spring 전역 설정)만 있고, 확장자·MIME 검증과 이미지 최적화는 구현하지 못했습니다. 개선한다면: (1) `file.getContentType()`으로 1차 검증 후 `ImageIO.read()`로 실제 이미지 여부를 다시 검증(Content-Type 헤더는 클라이언트가 조작 가능하므로), (2) 허용 확장자 화이트리스트(jpg/png/webp)로 제한, (3) Thumbnailator 같은 라이브러리로 업로드 시점에 리사이징·압축해서 저장 용량과 응답 속도를 개선하고 싶습니다."

---

## 3. 프론트엔드 & API/UI (Q9 ~ Q14)

### Q9. RESTful API 설계

**참고 파일**: `boot3/src/main/java/com/thejoa703/controller/BookController.java`

**설계 원칙**: 자원(Resource) 중심 URL + HTTP 메서드로 행위(Verb) 표현.

| 메서드 | 경로 | 의미 |
|---|---|---|
| GET | `/api/books` | 목록 조회(페이징) |
| GET | `/api/books/{id}` | 단건 조회 |
| GET | `/api/books/search` | 검색 |
| GET | `/api/books/bestsellers` | 베스트셀러 조회 |
| POST | `/api/books` | 등록 |
| PATCH | `/api/books/{id}` | 부분 수정 |
| PATCH | `/api/books/{id}/stock` | 재고만 별도 수정 (하위 리소스처럼 취급) |
| DELETE | `/api/books/{id}` | 삭제(소프트) |

**PUT 대신 PATCH를 선택한 이유**: 도서 수정 폼에서 항상 모든 필드를 다 채워 보내는 게 아니라, 바뀐 필드만 보낼 수 있게 하고 싶어서 "리소스 전체 교체"(PUT)가 아니라 "부분 수정"(PATCH) 의미를 명확히 썼습니다. 서비스 레이어(`BookService.updateBook()`)도 `null`이 아닌 필드만 반영하는 방식으로 맞췄습니다.

**재고 수정을 별도 엔드포인트(`/{id}/stock`)로 분리한 이유**: 도서 정보 수정과 재고 수정은 권한/빈도/트랜잭션 성격이 달라서, 명확히 구분되는 하위 자원으로 표현했습니다.

---

### Q10. React/Next.js 상태 관리 & 컴포넌트 구조

**참고 파일**
- `front3/store/configureStore.js`
- `front3/reducers/index.js`, `front3/sagas/index.js`
- `front3/reducers/`, `front3/sagas/`, `front3/components/`, `front3/pages/`

**상태 관리: Redux Toolkit + Redux-Saga**

도메인별로 reducer/saga 파일을 분리했습니다: `auth`(인증), `book`(도서), `notice`(공지, Sboard2), `cart`(장바구니), `order`(주문/결제). `reducers/index.js`의 `combineReducers`로 합치고, `sagas/index.js`의 `rootSaga`가 각 도메인 saga를 `fork`로 병렬 실행합니다.

```js
// reducers/index.js
const rootReducer = combineReducers({
    auth: authReducer, book: bookReducer, notice: noticeReducer,
    cart: cartReducer, order: orderReducer,
});
```

**왜 Redux-Saga인가**: 이 프로젝트는 API 호출(비동기) 후 성공/실패에 따라 후속 액션(예: 로그아웃 성공 후 소셜 provider별로 다른 URL로 리다이렉트)을 분기하는 로직이 많아서, `Promise` 체이닝보다 제너레이터 기반으로 흐름을 명시적으로 표현할 수 있는 saga가 적합하다고 판단했습니다.

**컴포넌트 구조**: `pages/`는 라우팅 단위 페이지, `components/`는 여러 페이지에서 재사용되는 UI(`AppLayout`, `BookSearchBox`, `BookList`, `AccessDenied` 등). 레이아웃(`AppLayout`)이 헤더/메뉴/Drawer를 담당하고 `{children}`으로 각 페이지를 감싸는 구조입니다.

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
      original._retry = true;  // 무한루프 방지 플래그
      try {
        const { data } = await api.post("/auth/refresh");  // 쿠키의 RefreshToken으로 재발급
        localStorage.setItem("accessToken", data.accessToken);
        original.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(original);   // 실패했던 원래 요청을 새 토큰으로 재시도
      } catch (refreshErr) {
        localStorage.removeItem("accessToken");
        window.location.href = "/login";  // 재발급도 실패하면 로그인 페이지로
      }
    }
    return Promise.reject(error);
  }
);
```

**핵심 포인트**
1. `original._retry` 플래그로 "이미 한 번 재시도한 요청은 또 재시도하지 않도록" 막아서 무한 루프를 방지합니다.
2. RefreshToken 자체는 HttpOnly 쿠키(`withCredentials: true`)로 관리해서 JS에서 직접 접근할 수 없게 했고, AccessToken만 `localStorage`에 둡니다 (XSS로부터 RefreshToken은 보호되지만 AccessToken은 노출 가능 — 그래서 AccessToken 만료시간을 15분으로 짧게 잡은 것과 연결됩니다, Q1 참고).
3. 재발급마저 실패하면(RefreshToken도 만료) 로그인 페이지로 강제 이동시킵니다.

---

### Q12. 실시간 검색 `useCallback`/`useRef` 최적화

**참고 파일**: `front3/components/BookSearchBox.js`

**솔직한 답변**: 이 컴포넌트는 **`useRef`는 사용하지만 `useCallback`은 사용하지 않습니다.**

```js
const debounceRef = useRef(null);  // 디바운스 타이머 ID 저장용

const handleChange = (e) => {
  const value = e.target.value;
  setKeyword(value);
  if (debounceRef.current) clearTimeout(debounceRef.current);  // 이전 타이머 취소
  ...
  debounceRef.current = setTimeout(async () => {
    const res = await api.get('/api/books/search', { params: { keyword: value.trim() } });
    setResults(res.data || []);
  }, 250);  // 250ms 디바운스
};
```

**`useRef`를 쓴 이유**: 디바운스 타이머 ID(`setTimeout`의 반환값)는 리렌더링을 유발할 필요가 없는 값이라, `useState`가 아니라 `useRef`로 보관합니다. `useState`로 관리했다면 타이머 ID가 바뀔 때마다 불필요한 리렌더링이 발생했을 것입니다.

**`useCallback`을 안 쓴 이유(솔직히)**: `handleChange` 함수를 `useCallback`으로 메모이제이션하지 않았습니다. 이 컴포넌트는 자식 컴포넌트에 이 핸들러를 props로 넘기지 않고, `input`에 직접 붙이는 단순 구조라 `useCallback`으로 얻는 이득(자식 컴포넌트의 불필요한 리렌더링 방지)이 크지 않다고 판단했습니다.

**면접 답변 예시**: "디바운스 타이머는 `useRef`로 관리해서 불필요한 리렌더링을 막았습니다. `useCallback`은 이 컴포넌트가 `React.memo`로 감싼 자식에게 핸들러를 넘기는 구조가 아니라서 적용하지 않았는데, 만약 검색 결과 드롭다운을 별도 메모이제이션된 컴포넌트로 분리한다면 그때는 `useCallback`으로 핸들러를 감싸는 게 의미가 있을 것 같습니다."

---

### Q13. 반응형 UI & Mobile Drawer

**참고 파일**: `front3/components/AppLayout.js`

**반응형 브레이크포인트 설계**: Ant Design의 `Col` `xs/sm/md/lg` 반응형 속성을 사용합니다.

```jsx
<Col flex="auto" xs={0} sm={0} md={0} lg={9}>  {/* 검색창: lg 이상에서만 노출 */}
  <BookSearchBox />
</Col>
<Col flex="auto" xs={0} sm={0} md={0} lg={18}> {/* 가로메뉴: lg 이상에서만 노출 */}
  <Menu ... />
</Col>
<Col flex="none" xs={2} lg={0}>  {/* 햄버거 버튼: lg 미만에서만 노출 */}
  <Button icon={<MenuOutlined />} onClick={() => setDrawerOpen(true)} />
</Col>
```

**실제로 겪었던 버그와 해결**: 처음엔 브레이크포인트를 `md`(768px) 기준으로 잡았는데, 태블릿 폭에서 로고+검색창+가로메뉴를 한 줄에 담을 공간이 부족해서 `Row`가 자동으로 줄바꿈되며 헤더가 2줄(이중)로 보이는 문제가 있었습니다.

**해결책 2가지를 함께 적용**:
1. 기준을 `md`에서 `lg`(992px)로 올려서, 태블릿 폭에서는 검색창/가로메뉴 자체를 숨기고 계속 햄버거(Drawer) 메뉴를 쓰도록 변경.
2. `Row`에 `wrap={false}`를 추가해서, 혹시 폭이 애매하게 부족해도 강제로 한 줄을 유지하도록 방어.

```jsx
<Header style={{ display: "flex", overflow: "hidden" }}>
  <Row align="middle" justify="space-between" wrap={false} ...>
```
`overflow: hidden`도 함께 걸어서, 혹시 내용이 헤더의 고정 높이를 넘어 삐져나오면서 겹쳐 보이는 것까지 이중으로 방지했습니다.

**Mobile Drawer**: `lg` 미만(모바일+태블릿)에서는 햄버거 버튼을 눌러 우측에서 슬라이드되는 `antd Drawer`가 열리고, 그 안에 검색창과 세로 메뉴를 함께 배치했습니다.

---

### Q14. UI 단 권한 분기 처리

**참고 파일**
- `front3/components/AppLayout.js`
- `front3/components/AccessDenied.js`
- `front3/components/BookList.js`, `front3/pages/books/new.js`, `front3/pages/books/[id].js` 등

**두 단계로 분기**

1) **메뉴/버튼 노출 여부** — `state.auth.user.role`을 확인해서, 로그인 여부와 관리자 여부에 따라 다른 메뉴 항목을 렌더링합니다.
```js
// AppLayout.js
const menuItems = [
  ...(user && user.nickname
    ? [ /* 로그인 상태 메뉴: 도서/공지/장바구니/주문내역/마이페이지/로그아웃 */ ]
    : [ /* 비로그인 메뉴: 도서/공지/로그인/회원가입 */ ]
  ),
];
```
```js
// BookList.js
const isAdmin = user?.role === "ROLE_ADMIN";
// isAdmin 일 때만 "도서 등록" 버튼, 각 카드의 "수정/삭제" 버튼 노출
```

2) **페이지 자체 접근 차단** — 도서 등록/수정처럼 관리자 전용 페이지는, 컴포넌트 렌더링 시점에 권한을 확인해서 권한이 없으면 폼 대신 `AccessDenied` 컴포넌트를 보여줍니다.
```js
// pages/books/new.js (개념적 예시)
if (!user) return <AccessDenied needLogin />;
if (user.role !== 'ROLE_ADMIN') return <AccessDenied message="이 기능은 관리자만 이용할 수 있습니다." />;
```

**중요한 원칙**: 이 프론트엔드 단 분기는 어디까지나 **UX 편의**(허용 안 된 화면/버튼을 아예 안 보여줘서 혼란을 줄임)이지, **진짜 보안 경계가 아닙니다.** 실제 권한 검증은 백엔드(`@PreAuthorize("hasRole('ADMIN')")`)에서 이루어지므로, 프론트에서 권한 체크를 우회해도(개발자도구로 버튼을 강제로 노출시키는 등) 실제 API 호출은 서버에서 403으로 막힙니다. 이 이중 구조(UI는 편의, API는 보안)를 면접에서 명확히 설명하는 게 좋습니다.

---

## 4. CS 및 백엔드/프론트엔드 웹 핵심 기본기 (Q15 ~ Q30)

### Q15. API 정의 및 역할

API(Application Programming Interface)는 서로 다른 소프트웨어(또는 소프트웨어와 사용자) 사이에서 정의된 규칙에 따라 요청과 응답을 주고받을 수 있게 해주는 약속입니다. 이 프로젝트에서는 프론트엔드(Next.js)와 백엔드(Spring Boot)가 서로 다른 서버/포트(`localhost:3000` ↔ `localhost:8080`)에서 실행되는데, HTTP 기반 REST API(`/api/books`, `/api/orders` 등)가 이 둘을 연결하는 유일한 통로입니다. 프론트는 화면과 상호작용만, 백엔드는 데이터와 비즈니스 로직만 책임지도록 관심사를 분리하는 역할을 합니다.

### Q16. RESTful API 설계 원칙

1. **자원(Resource)은 명사, URL은 자원을 가리키기만** — `/api/books`(o), `/api/getBooks`(x)
2. **행위는 HTTP 메서드로 표현** — GET(조회)/POST(생성)/PUT·PATCH(수정)/DELETE(삭제)
3. **상태 코드를 의미에 맞게 사용** — 200(성공)/201(생성됨)/400(잘못된 요청)/401(인증 필요)/403(권한 없음)/404(없음)
4. **무상태(Stateless)** — 서버가 클라이언트의 이전 요청 상태를 기억하지 않고, 매 요청에 필요한 정보(토큰 등)를 전부 포함해서 보냄. 이 프로젝트도 `SessionCreationPolicy.STATELESS`로 세션을 안 쓰고 JWT로 매 요청을 독립적으로 인증합니다 (`SecurityConfig.java`).
5. **계층 구조** — `/api/books/{id}/stock`처럼 상위-하위 관계를 URL 경로로 표현 (Q9 참고).

### Q17. JWT 구조 (Header, Payload, Signature)

JWT는 `.`으로 구분된 3부분(`xxxxx.yyyyy.zzzzz`)으로 구성되고, 각각 Base64Url로 인코딩됩니다.

1. **Header**: 서명 알고리즘과 토큰 타입. 이 프로젝트는 `SignatureAlgorithm.HS256`(HMAC-SHA256)을 사용합니다 (`JwtProvider.java`).
2. **Payload(Claims)**: 실제 담는 정보. `iss`(발급자), `sub`(주체, 이 프로젝트에서는 사용자 ID), `iat`(발급시각), `exp`(만료시각), 그리고 커스텀 클레임(`role` 등)이 담깁니다.
3. **Signature**: Header+Payload를 서버만 아는 비밀키(`JWT_SECRET`)로 서명한 값. 이 서명이 있어서 클라이언트나 중간자가 Payload를 조작해도 서명이 안 맞아 위조를 탐지할 수 있습니다.

**주의할 점**: Payload는 암호화가 아니라 인코딩만 되어 있어서, 디코딩하면 누구나 내용을 읽을 수 있습니다(비밀번호 같은 민감정보를 담으면 안 됨). 이 프로젝트도 Payload에는 사용자ID/role만 담고 비밀번호는 절대 담지 않습니다.

### Q18. SSR vs CSR 차이점

- **CSR(Client-Side Rendering)**: 서버는 빈 HTML+JS 번들만 내려주고, 브라우저가 JS를 실행해서 화면을 그립니다. 초기 로딩은 느리지만(빈 화면→JS 다운로드→렌더링), 이후 페이지 전환은 빠릅니다. SEO에 불리합니다(검색엔진 크롤러가 빈 HTML만 볼 수 있음).
- **SSR(Server-Side Rendering)**: 서버가 이미 완성된 HTML을 렌더링해서 내려줍니다. 첫 화면이 더 빨리 보이고 SEO에 유리하지만, 매 요청마다 서버가 렌더링 부담을 집니다.
- 이 프로젝트는 Next.js를 쓰지만, 대부분 페이지가 `useEffect` + Redux-Saga로 클라이언트에서 데이터를 가져오는 CSR 방식입니다(도서 목록/상세 등). 로그인 여부에 따라 내용이 크게 달라지는 개인화된 화면(장바구니, 마이페이지)이 많아서 SSR의 이점(SEO, 초기 로딩)보다 CSR의 단순함을 택했습니다.

### Q19. Spring IoC / DI

**IoC(Inversion of Control, 제어의 역전)**: 객체를 개발자가 직접 `new`로 생성하고 관리하는 대신, 스프링 컨테이너가 객체(빈)의 생성과 생명주기를 대신 관리하는 원칙입니다.

**DI(Dependency Injection, 의존성 주입)**: IoC를 구현하는 구체적인 방법 중 하나로, 객체가 필요로 하는 의존 객체를 직접 생성하지 않고 외부(스프링 컨테이너)로부터 주입받습니다.

이 프로젝트는 생성자 주입(Lombok `@RequiredArgsConstructor`)을 일관되게 사용합니다.
```java
// BookService.java
@Service
@RequiredArgsConstructor
public class BookService {
    private final BookMapper bookMapper;
    private final BookStockRepository bookStockRepository;
    // final 필드 + @RequiredArgsConstructor 가 생성자를 자동생성 → 스프링이 주입
}
```
**생성자 주입을 선택한 이유**: 필드 주입(`@Autowired` 필드)은 순환 참조를 런타임에야 발견하지만, 생성자 주입은 컴파일 시점(또는 스프링 컨텍스트 초기화 시점)에 조기 발견할 수 있고, `final`로 불변성을 보장하며, 테스트에서 mock을 직접 생성자에 넣기도 쉽습니다.

### Q20. Database JOIN 종류 및 차이

- **INNER JOIN**: 두 테이블에서 조인 조건이 일치하는 행만 반환.
- **LEFT (OUTER) JOIN**: 왼쪽 테이블의 모든 행을 반환하고, 오른쪽에 매칭되는 게 없으면 NULL로 채움.
- **RIGHT (OUTER) JOIN**: LEFT의 반대.
- **FULL OUTER JOIN**: 양쪽 다 매칭 안 되는 행까지 포함.

이 프로젝트는 도서 목록에서 재고(BOOK_STOCK) 정보를 LEFT JOIN 합니다 (`BookMapper.xml`). **재고가 아직 등록 안 된 신규 도서도 목록에 나와야 하기 때문**에 INNER JOIN을 쓰면 재고 없는 도서가 통째로 빠지는 문제가 생겨서 LEFT JOIN을 선택했습니다.

### Q21. Database Subquery 개념 및 종류

서브쿼리는 하나의 SQL문 안에 포함된 또 다른 SELECT문입니다.
- **스칼라 서브쿼리**: SELECT절에서 단일 값 하나를 반환.
- **인라인 뷰**: FROM절에서 하나의 임시 테이블처럼 사용.
- **중첩(nested) 서브쿼리**: WHERE절에서 `IN`, `EXISTS`, 비교연산자와 함께 사용.

이 프로젝트에서는 베스트셀러 집계(`OrderItemMapper.xml`)에서 서브쿼리 대신 JOIN + GROUP BY로 직접 처리했는데, 만약 "각 카테고리별 최고 판매 도서"처럼 그룹별 최댓값을 뽑아야 했다면 `ROW_NUMBER() OVER (PARTITION BY ...)` 같은 윈도우 함수나 인라인 뷰가 필요했을 것입니다.

### Q22. DML 개념 및 DDL/DCL과의 차이

- **DML(Data Manipulation Language)**: 데이터 조작. `SELECT`, `INSERT`, `UPDATE`, `DELETE`. 이 프로젝트의 대부분 Mapper XML/JPA 쿼리가 여기 해당.
- **DDL(Data Definition Language)**: 스키마(구조) 정의. `CREATE TABLE`, `ALTER TABLE`, `DROP TABLE`. 이 프로젝트는 Hibernate `ddl-auto`가 엔티티 기반으로 DDL을 자동 생성합니다 (`application.yml`).
- **DCL(Data Control Language)**: 권한 제어. `GRANT`, `REVOKE`. 이 프로젝트는 애플리케이션 레벨(Spring Security `@PreAuthorize`)에서 권한을 통제하고, DB 사용자 계정 자체의 권한 부여는 별도로 다루지 않습니다.

### Q23. Cookie vs Session 차이 및 작동 방식

- **Cookie**: 클라이언트(브라우저)에 저장되는 key-value 데이터. 서버가 `Set-Cookie` 헤더로 내려주면 브라우저가 저장했다가, 이후 같은 도메인 요청마다 자동으로 실어 보냅니다.
- **Session**: 서버가 클라이언트별 상태를 서버 메모리(또는 별도 저장소)에 저장하고, 클라이언트에는 그 세션을 식별할 `session ID`만 쿠키로 내려줍니다. 실제 데이터는 서버에 있습니다(Stateful).

이 프로젝트는 세션을 쓰지 않고(`SessionCreationPolicy.STATELESS`) JWT를 씁니다. 다만 **RefreshToken은 쿠키로** 내려줍니다 — AccessToken(짧은 수명, JS가 헤더에 실어 보냄)과 RefreshToken(긴 수명, HttpOnly 쿠키로 JS 접근 차단)의 역할을 분리한 것입니다.

### Q24. JWT vs Cookie/Session 인증 방식 비교

| | Session | JWT |
|---|---|---|
| 상태 | Stateful (서버가 세션 저장) | Stateless (토큰 자체에 정보 포함) |
| 확장성 | 서버가 여러 대면 세션 공유(Sticky session/Redis 등) 필요 | 서버 간 상태 공유 불필요 → 수평 확장 쉬움 |
| 즉시 무효화 | 서버에서 세션 삭제하면 즉시 반영 | 자체적으로는 불가(만료까지 유효) — 블랙리스트 등 별도 장치 필요 |
| 페이로드 노출 | 서버만 봄 | 클라이언트도 디코딩해서 내용 확인 가능(암호화 아님) |

이 프로젝트가 JWT를 택한 이유는 REST API를 프론트/백엔드가 분리된 구조에서 세션 쿠키 기반 인증보다 명확하게 관리할 수 있고, 향후 서버를 여러 대로 확장해도 세션 동기화 문제가 없기 때문입니다. 대신 "즉시 무효화가 어렵다"는 단점은 Q1에서 언급한 대로 AccessToken 만료시간을 짧게(15분) 잡는 것으로 완화했습니다.

### Q25. CORS 개념 및 해결 경험

**CORS(Cross-Origin Resource Sharing)**: 브라우저가 "현재 페이지의 출처(origin)와 다른 출처로의 요청"을 기본적으로 차단하는 보안 정책(SOP, Same-Origin Policy)을 완화해주는 메커니즘. 서버가 응답 헤더로 "이 출처는 허용한다"고 명시해야 브라우저가 응답을 JS에 넘겨줍니다.

이 프로젝트는 프론트(`localhost:3000`)와 백엔드(`localhost:8080`)의 포트가 달라서 CORS 문제가 발생했습니다. `SecurityConfig.java`에서 다음과 같이 해결했습니다.

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:3000"));  // 허용 출처를 명시적으로 지정
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);  // 쿠키(RefreshToken) 포함 요청 허용
    configuration.setMaxAge(3600L);
    ...
}
```

**실제로 겪었던 함정**: `allowCredentials(true)`(쿠키를 주고받으려면 필수)로 설정하면, `allowedOrigins`에 `"*"`(모든 출처 허용)를 쓸 수 없습니다(브라우저 스펙상 credentials 모드에서는 와일드카드 출처가 금지되어 있음). 그래서 정확한 출처(`http://localhost:3000`)를 명시했습니다.

### Q26. OAuth 2.0 동작 원리

OAuth 2.0은 사용자가 제3자 서비스(카카오/구글/네이버)에 자신의 비밀번호를 직접 넘기지 않고도, 그 서비스가 우리 애플리케이션에게 "이 사용자다"라고 인증해줄 수 있게 하는 위임 인증 프로토콜입니다.

이 프로젝트의 흐름 (`OAuth2SuccessHandler.java`, `CustomOAuth2User.java`):
1. 사용자가 "카카오로 로그인" 클릭 → 카카오 인증 서버로 리다이렉트
2. 사용자가 카카오 계정으로 로그인/동의 → 카카오가 우리 서버(`/login/oauth2/code/kakao` 등)로 인가 코드(authorization code)와 함께 리다이렉트
3. Spring Security의 OAuth2 클라이언트가 이 코드로 카카오에 액세스 토큰을 요청하고, 그 토큰으로 사용자 정보(이메일, 닉네임 등)를 가져옴
4. `OAuth2SuccessHandler`가 이 정보로 우리 DB에 회원이 있는지 확인 → 있으면 로그인, 없으면 신규 가입 처리 후 우리 서비스 자체의 JWT를 발급해서 프론트로 전달

**중요한 점**: 카카오/구글의 토큰과 우리 서비스의 JWT는 별개입니다. OAuth2는 "이 사람이 카카오 계정 소유자임을 확인"하는 데만 쓰이고, 그 이후 우리 서비스 내에서의 인증은 자체 발급 JWT로 이루어집니다.

### Q27. React State vs Props & 전역 상태 필요성

- **State**: 컴포넌트 내부에서 관리하는, 변할 수 있는 데이터. 그 컴포넌트(와 자식)만 직접 조작 가능.
- **Props**: 부모가 자식에게 내려주는 읽기 전용 데이터. 자식이 직접 바꿀 수 없고, 값이 바뀌길 원하면 부모가 내려준 콜백 함수(props로 전달된 함수)를 호출해야 함.

**전역 상태(Redux)가 필요한 이유**: `user`(로그인 정보), `cart`(장바구니 개수)처럼 **서로 무관해 보이는 여러 컴포넌트가 동시에 같은 데이터를 필요로 하는 경우**, props로 계속 아래로 전달(props drilling)하면 중간 컴포넌트들이 실제로 안 쓰는 데이터까지 계속 넘겨줘야 해서 유지보수가 어렵습니다. 이 프로젝트에서 `AppLayout`(헤더의 장바구니 뱃지), `pages/cart/index.js`(장바구니 페이지), `pages/order/checkout.js`(결제 페이지)가 모두 `state.cart`를 동시에 참조하는데, 이런 경우가 전역 상태 관리가 필요한 전형적인 사례입니다.

### Q28. React Virtual DOM 동작 원리

실제 DOM 조작은 브라우저의 레이아웃 재계산·리페인트를 유발해서 비용이 큽니다. React는 실제 DOM과 별개로 메모리상의 가벼운 JS 객체 트리(Virtual DOM)를 유지합니다.

1. 상태(state)가 바뀌면, React는 새 Virtual DOM 트리를 만듭니다.
2. 이전 Virtual DOM 트리와 새 트리를 비교(Diffing)해서, 실제로 달라진 부분만 찾아냅니다.
3. 달라진 부분만 실제 DOM에 최소한으로 반영합니다(Reconciliation).

이 방식으로 매번 전체 DOM을 다시 그리지 않고, 변경된 최소 범위만 갱신해서 성능을 확보합니다. 이 프로젝트에서도 예를 들어 장바구니 수량을 바꾸면 그 항목의 수량 텍스트만 갱신되고, 나머지 도서 목록 전체가 다시 그려지지 않습니다.

### Q29. 객체지향 5대 원칙(SOLID 원칙)이란 무엇인가요?

- **S (단일 책임 원칙, SRP)**: 클래스는 하나의 책임만 가져야 합니다. 이 프로젝트의 `BookService`(비즈니스 로직)와 `BookMapper`(데이터 접근)를 분리한 것이 이 원칙의 적용입니다.
- **O (개방-폐쇄 원칙, OCP)**: 확장에는 열려있고 변경에는 닫혀있어야 합니다. 예를 들어 `KakaoPayApiService`처럼 특정 PG사에 의존하는 구현을 인터페이스로 추상화해뒀다면, 새 결제수단(토스페이 등)을 추가할 때 기존 코드를 안 건드리고 새 구현체만 추가할 수 있었을 것입니다(이 프로젝트는 카카오페이 하나만 지원해서 이 추상화까지는 하지 않았습니다).
- **L (리스코프 치환 원칙, LSP)**: 자식 클래스는 부모 클래스를 대체할 수 있어야 합니다.
- **I (인터페이스 분리 원칙, ISP)**: 클라이언트가 쓰지 않는 메서드에 의존하도록 강제하면 안 됩니다. `BookStockRepository`, `CartRepository` 등을 하나의 거대한 Repository로 합치지 않고 도메인별로 작게 나눈 것이 이 원칙과 맞닿아 있습니다.
- **D (의존관계 역전 원칙, DIP)**: 고수준 모듈이 저수준 모듈의 구체적 구현이 아니라 추상화(인터페이스)에 의존해야 합니다. `BookService`가 `BookStockRepository`(인터페이스, 실제 구현은 Spring Data JPA가 런타임에 생성)에 의존하는 구조 자체가 DIP의 예시입니다.

### Q30. 객체지향 프로그래밍(OOP)의 4대 특성은 무엇인가요?

- **캡슐화(Encapsulation)**: 데이터(필드)와 그 데이터를 다루는 로직(메서드)을 하나로 묶고, 내부 구현을 외부로부터 숨김. 이 프로젝트의 엔티티들이 필드를 `private`으로 감추고 Lombok `@Getter`/`@Setter`나 서비스 메서드를 통해서만 접근하게 한 것.
- **상속(Inheritance)**: 기존 클래스의 속성/동작을 물려받아 재사용. 예를 들어 `OrderStatus`(enum)로 상태를 표현하는 대신, `Order` 하위에 `PendingOrder`, `PaidOrder`처럼 상속 구조를 만들 수도 있었지만, 이 프로젝트는 상태 전이가 단순해서 enum + 상태 필드 방식을 택했습니다.
- **다형성(Polymorphism)**: 같은 인터페이스를 여러 구현체가 서로 다르게 구현. `JpaRepository<Book, Long>` 인터페이스를 Spring Data JPA가 런타임에 프록시로 구현해주는 것, 혹은 `List<CartItemResponseDto>`를 다루는 코드가 실제 구현 클래스가 `ArrayList`든 뭐든 신경 쓰지 않는 것이 다형성의 실무적 활용입니다.
- **추상화(Abstraction)**: 복잡한 구현은 감추고 필요한 인터페이스만 노출. `BookService.deleteBook()`을 호출하는 컨트롤러 입장에서는 내부적으로 소프트 삭제인지, 어떤 SQL이 실행되는지 몰라도 되는 것이 추상화입니다.

---

## 부록 A. 참고 파일 전체 목록

**백엔드 (`boot3/src/main/java/com/thejoa703/`)**
```
security/JwtProvider.java
security/JwtAuthenticationFilter.java
security/TokenStore.java
security/JwtProperties.java
oauth2/OAuth2SuccessHandler.java
oauth2/CustomOAuth2User.java
oauth2/UserInfoOAuth2.java
config/SecurityConfig.java
config/RedisConfig.java
config/SchemaAutoFixRunner.java
controller/BookController.java
controller/UserController.java
service/BookService.java
service/PaymentService.java
service/OrderService.java
service/CartService.java
service/UserService.java
service/Sboard2Service.java
entity/Book.java
entity/BookStock.java
entity/Orders.java
entity/OrderItem.java
entity/Cart.java, entity/CartItem.java
entity/AppUser.java
repository/BookStockRepository.java
repository/AppUserRepository.java
repository/OrdersRepository.java, repository/OrderItemRepository.java
repository/CartRepository.java, repository/CartItemRepository.java
mapper/BookMapper.java + resources/mapper/BookMapper.xml
mapper/Sboard2Mapper.java + resources/mapper/Sboard2Mapper.xml
mapper/OrderItemMapper.java + resources/mapper/OrderItemMapper.xml
api/KakaoPayApiService.java
api/ApiKakaoBook.java
api/NlBookApiService.java
util/FileStorageService.java
resources/application.yml
```

**프론트엔드 (`front3/`)**
```
api/axios.js
store/configureStore.js
reducers/index.js, sagas/index.js
reducers/authReducer.js, sagas/authSaga.js
reducers/bookReducer.js, sagas/bookSaga.js
reducers/cartReducer.js, sagas/cartSaga.js
reducers/orderReducer.js, sagas/orderSaga.js
reducers/noticeReducer.js, sagas/noticeSaga.js
components/AppLayout.js
components/BookSearchBox.js
components/BookList.js
components/AccessDenied.js
pages/books/new.js, pages/books/[id].js
```

## 부록 B. 조사 중 발견한 부수적 이슈

문서 작성을 위해 프로젝트 전체를 훑는 과정에서, 이번 Q&A와는 별개로 정리하면 좋을 항목을 하나 발견했습니다.

- **`front3/pages/index-ver1.zip`** — `pages` 디렉토리 안에 압축 파일이 그대로 들어있습니다. Next.js는 `pages/` 아래의 `.js` 파일만 라우트로 인식하므로 빌드에는 영향이 없지만, 이전 버전을 백업해두려다 남은 잔재로 보입니다. 배포 산출물에 불필요한 파일이 섞여 있는 상태라 정리를 권장합니다. 원하시면 이 파일을 지운 최종 zip을 다시 만들어드릴 수 있습니다.

---

*이 문서는 업로드된 프로젝트 코드를 직접 읽고 확인한 내용을 근거로 작성했습니다. "미구현"으로 표시한 항목은 실제로 코드에 없는 것을 확인한 것이며, 실제 면접에서는 이 문서의 "면접 답변 예시"를 참고하되 본인의 언어로 재구성해서 답하는 것을 권장합니다.*
