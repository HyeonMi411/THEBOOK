# BookStore 프로젝트 포트폴리오 종합 점검 및 기술 Q&A

> 본 문서는 실제 프로젝트 코드(boot3 + front3)를 직접 확인하여 작성했습니다.
> 구현되어 있는 부분은 실제 코드를 근거로, 미구현된 부분은 "미구현"임을 명확히
> 밝히고 개선 방향을 제시했습니다. 면접 답변 시 이 구분을 유지하시는 것이
> 오히려 신뢰도를 높입니다 (없는 걸 있다고 하는 것보다, "이 부분은 아직 없지만

## 목차

1. [포트폴리오 종합 점검 및 평가](#1-포트폴리오-종합-점검-및-평가)
2. [백엔드 & 데이터베이스 (Q1~Q8-1)](#2-백엔드--데이터베이스-q1--q8-1)
3. [프론트엔드 & API/UI (Q9~Q14)](#3-프론트엔드--apiui-q9--q14)
4. [CS 및 웹 핵심 기본기 (Q15~Q30)](#4-cs-및-백엔드프론트엔드-웹-핵심-기본기-q15--q30)

---

> 이렇게 개선할 계획입니다"가 훨씬 좋은 인상을 줍니다).

---

## 1. 포트폴리오 종합 점검 및 평가

### 1-1. 프로젝트 총평

**기술 스택**: Spring Boot 3.3 + Java 17 + Spring Security + JWT + Redis + JPA(주)/MyBatis(보조 조회) + Oracle / Next.js 12 + Redux Toolkit + Redux-Saga + Ant Design 4

**강점**

| 항목 | 내용 |
|---|---|
| 인증 구조 | JWT(Access/Refresh 이원화) + Redis 기반 Refresh Token 관리 + 소셜로그인(카카오/구글/네이버) 3사 연동을 실제로 구현 |
| 결제 도메인 | 카카오페이 실연동(준비→승인 2단계), 재고차감을 결제 "승인" 시점으로 지연시켜 낙관적 락(@Version)으로 동시성 제어 — 단순 CRUD를 넘어선 실무형 도메인 로직 |
| 데이터 정합성 감각 | 개발 과정에서 실제로 겪은 문제들(영속성 컨텍스트 detached, 양방향 연관관계 미동기화로 인한 FK 위반, 트랜잭션 경계 문제)을 근본 원인까지 추적해서 해결한 이력이 있음 — 이 자체가 JPA 이해도를 보여주는 좋은 사례 |
| 계층 분리 | Controller-Service-Repository 계층이 일관되게 분리되어 있고, DTO 변환이 Service 계층에서 이루어짐 |
| 테스트 커버리지 | 프론트 reducer/saga 단위테스트 180여건 존재, 백엔드도 JUnit 통합테스트로 엔티티/서비스 레이어 검증 |

**개선이 필요한 부분** (아래 Q&A에서 상세히 다룸)

| 항목 | 현재 상태 | 관련 질문 |
|---|---|---|
| 로그아웃 시 Access Token 무효화 | Refresh Token만 Redis에서 삭제, Access Token은 자체 만료(15분)에만 의존 | Q1 |
| Redis 활용 범위 | Refresh Token 저장 용도로만 사용, 캐싱/멱등성 처리는 미적용 | Q3 |
| 동시성 락의 타임아웃 | `@Lock(PESSIMISTIC_WRITE)` 적용은 되어 있으나 명시적 타임아웃 설정은 없음 | Q4 |
| N+1 쿼리 가능성 | `@EntityGraph`/`JOIN FETCH` 명시적 사용 없음 — 주문상세 등 연관관계 많은 화면에서 잠재적 N+1 위험 | Q5 |
| 외부 API 장애 대응 | 카카오페이/카카오도서/국립중앙도서관 API 호출에 대해 예외 변환은 되어 있으나 Retry/Circuit Breaker 미적용 | Q6 |
| 파일 업로드 검증 | 용량 제한, MIME 타입 검증, 이미지 압축/리사이징 로직 없음 (현재는 원본을 그대로 저장) | Q8-1 |
| 검색 최적화 | 디바운스(`useRef`+`setTimeout`)는 있으나 `useCallback` 메모이제이션은 미적용 | Q12 |

이런 구멍들을 스스로 인지하고 "왜 지금은 이렇게 되어 있고, 어떻게 개선할 것인가"까지 설명할 수 있으면, 면접에서는 오히려 강점으로 작용합니다. 아래 Q&A에서 각각 구체적인 개선 방향을 함께 제시했습니다.

---

## 2. 백엔드 & 데이터베이스 (Q1 ~ Q8-1)

### Q1. 로그인 & Security/JWT (로그아웃 Blacklist 포함)

**참고 파일**: `boot3/src/main/java/com/thejoa703/security/JwtProvider.java`, `boot3/src/main/java/com/thejoa703/security/TokenStore.java`, `boot3/src/main/java/com/thejoa703/controller/UserController.java`, `boot3/src/main/java/com/thejoa703/config/SecurityConfig.java`

**현재 구현**
- 로그인 성공 시 **Access Token**(15분 만료)과 **Refresh Token**(예: 14일 만료, `HttpOnly` 쿠키)을 함께 발급합니다.
- Access Token은 매 API 요청마다 `Authorization: Bearer {token}` 헤더로 전달되고, Spring Security의 `JwtAuthenticationFilter`가 이를 검증해서 `SecurityContext`에 인증 정보를 채웁니다.
- Refresh Token은 `Redis`에 `refresh:{userId}` 키로 저장되고, Access Token 만료 시 프론트의 axios 인터셉터가 `/auth/refresh`를 호출해 새 Access Token을 재발급받습니다(Silent Refresh, Q11 참고).
- **로그아웃 시**: Redis에서 해당 사용자의 Refresh Token을 삭제하고, 브라우저의 Refresh Token 쿠키도 만료시킵니다.

```java
public void deleteRefreshToken(String userId) {
    String key = buildKey(userId); // "refresh:" + userId
    stringRedisTemplate.delete(key);
}
```

**Blacklist에 대한 솔직한 답변**: 현재는 **Access Token 자체를 무효화하는 블랙리스트는 구현되어 있지 않습니다.** 로그아웃해도 이미 발급된 Access Token은 자기 만료시간(15분)까지는 이론상 계속 유효합니다. 다만 실무적으로:
1. Access Token 만료시간을 짧게(15분) 가져가서 탈취 시 피해 윈도우를 최소화했고
2. Refresh Token은 로그아웃 즉시 Redis에서 삭제되므로, 그 Access Token이 만료된 이후에는 재발급이 불가능합니다.

**개선 방향(실제로 붙인다면)**: 로그아웃 시 Access Token의 `jti`(JWT ID) 클레임을 Redis에 `blacklist:{jti}`로 저장(TTL = 남은 만료시간)하고, `JwtAuthenticationFilter`에서 매 요청마다 이 블랙리스트에 있는지 확인하는 방식으로 확장 가능합니다. 다만 이렇게 하면 매 요청마다 Redis 조회가 추가되어 약간의 레이턴시가 생기므로, "즉시 무효화가 꼭 필요한 서비스인가"를 먼저 판단하는 게 맞다고 생각합니다(뱅킹/결제처럼 민감하면 필요, 일반 서비스는 짧은 만료시간만으로도 충분한 경우가 많음).

---

### Q2. 카카오페이 결제 & 재고 관리

**참고 파일**: `boot3/src/main/java/com/thejoa703/service/PaymentService.java`, `boot3/src/main/java/com/thejoa703/service/OrderService.java`, `boot3/src/main/java/com/thejoa703/api/KakaoPayApiService.java`, `boot3/src/main/java/com/thejoa703/entity/BookStock.java`

**결제 흐름 (3단계)**
1. **결제준비(ready)**: 주문(`Orders`, `PENDING` 상태) 생성 후, 카카오페이 API에 `cid`, `partner_order_id`, `item_name`, `quantity`, `total_amount`, `approval_url`/`cancel_url`/`fail_url`을 담아 요청 → `tid`(거래 고유번호)와 리다이렉트 URL을 받음
2. **사용자 결제 진행**: 카카오페이 결제창(외부 도메인)에서 실제 결제 진행
3. **결제승인(approve)**: 카카오가 `approval_url`로 `pg_token`을 담아 리다이렉트 → 프론트가 이 값을 백엔드에 전달 → 백엔드가 `tid`+`pg_token`으로 카카오에 승인 요청 → 성공하면 `Orders.orderStatus = PAID`로 변경

**재고 관리 — 핵심 설계 포인트**
```java
@Transactional
public OrderResponseDto approve(...) {
    // 카카오페이 승인 API 호출
    // 승인 성공 후 재고차감
    for (OrderItem item : order.getItems()) {
        BookStock stock = bookStockRepository.findByIdForUpdate(item.getBook().getId()) // 비관적 락
                .orElseThrow(...);
        if (stock.getStockQuantity() < item.getQuantity()) {
            throw new IllegalStateException("재고가 부족합니다.");
        }
        stock.setStockQuantity(stock.getStockQuantity() - item.getQuantity());
    }
    order.setOrderStatus(OrderStatus.PAID);
}
```

**재고차감 시점을 "결제 승인"으로 미룬 이유**: 만약 "결제준비" 시점에 재고를 미리 차감하면, 사용자가 결제창을 열어놓고 결제를 안 하거나 중간에 이탈했을 때 그 재고가 계속 묶여서 다른 사용자가 못 사는 문제가 생깁니다. 그래서 재고차감은 **실제로 돈이 오간 것이 확정되는 승인 시점**에만 일어나도록 설계했습니다.

**동시성 제어는 Q4에서 상세히 다룹니다.**

**개발 중 실제로 겪었던 버그(정직하게 공유할 만한 경험)**: 초기 구현에서 도서 가격이 `null`인 상태로 주문이 생성될 수 있는 허점이 있었는데, 이게 결제금액 0원으로 카카오에 전달되면서 카카오 서버가 원인 모를 내부 오류를 반환하는 문제가 있었습니다. 이후 **주문 생성 이전 단계에서 가격 검증을 선행**하도록 고쳐서, 문제가 훨씬 이해하기 쉬운 시점(주문생성)에서 명확한 한국어 메시지로 막도록 개선했습니다.

---

### Q3. Redis 활용처 (토큰, 캐싱, 멱등성 & 다운 시 Fallback)

**참고 파일**: `boot3/src/main/java/com/thejoa703/security/TokenStore.java`, `boot3/src/main/java/com/thejoa703/config/RedisConfig.java`

**현재 실제 활용처**: **Refresh Token 저장/조회/삭제** 이것 하나뿐입니다.

```java
@Component
@RequiredArgsConstructor
public class TokenStore {
    private final StringRedisTemplate stringRedisTemplate;
    public void saveRefreshToken(String userId, String token, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(buildKey(userId), token, ttlSeconds, TimeUnit.SECONDS);
    }
    // getRefreshToken, deleteRefreshToken ...
}
```

**캐싱/멱등성은 현재 미적용**입니다. 솔직히 말씀드리면:
- **캐싱**: 도서 목록/상세 조회처럼 자주 읽히고 자주 안 바뀌는 데이터에 `@Cacheable`을 붙이면 DB 부하를 줄일 수 있는데, 아직 적용하지 않았습니다.
- **멱등성**: 결제승인(`approve`) API가 사용자의 새로고침이나 중복 클릭으로 여러 번 호출될 수 있는데, 현재는 **DB 상태값으로만** 방어하고 있습니다(`if (order.getOrderStatus() == PAID) return 그대로;`). Redis 기반의 진짜 "멱등성 키"(예: `idempotency:{tid}`를 SETNX로 선점)는 아직 없습니다.

**Redis 다운 시 Fallback**: 현재 구조에서 Redis가 죽으면 **로그인 자체가 안 됩니다** (Refresh Token을 저장/조회 못 하므로). 이건 명확한 단일장애점(SPOF)입니다.

**개선 방향**:
1. 캐싱: `spring-boot-starter-cache` + `@Cacheable(value="books", key="#id")`을 도서 상세조회에 적용, `@CacheEvict`를 수정/삭제 시점에 함께 걸어서 캐시 무효화
2. 멱등성: 결제승인 시작 시 `SETNX idempotency:{tid} 1 EX 60`로 선점 → 이미 선점되어 있으면 "처리중"으로 즉시 응답
3. Redis 장애 대응: Redis 연결 실패 시 로그인 자체를 막는 대신, Refresh Token 재발급 기능만 일시적으로 비활성화하고 로그인/API 이용 자체(Access Token 검증)는 Redis 없이도 가능하게 하는 게 더 견고한 설계입니다 (Access Token 검증은 JWT 서명 검증만으로 가능해서 Redis 의존이 없기 때문).

---

### Q4. 동시성 제어 & 비관적 락(`@Lock`, `FOR UPDATE` & Timeout)

**참고 파일**: `boot3/src/main/java/com/thejoa703/entity/BookStock.java`, `boot3/src/main/java/com/thejoa703/repository/BookStockRepository.java`, `boot3/src/main/java/com/thejoa703/service/PaymentService.java`

**실제 구현 — 2단계 방어**

1. **낙관적 락(`@Version`)** — `BookStock` 엔티티 자체에 걸려있음
```java
@Version
private Long version; // 재고차감 동시성 제어용
```
이건 "누군가 먼저 수정하고 나면, 뒤에 커밋하려는 트랜잭션이 옛날 버전으로 UPDATE를 시도할 때 실패(`OptimisticLockException`)시키는" 범용 방어입니다.

2. **비관적 락(`SELECT ... FOR UPDATE`)** — 재고차감이 실제로 일어나는 결제승인 시점에만 명시적으로 사용
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM BookStock s WHERE s.bookId = :bookId")
Optional<BookStock> findByIdForUpdate(@Param("bookId") Long bookId);
```
이건 "해당 재고 행을 조회하는 순간부터 다른 트랜잭션이 그 행을 건드리지 못하게 잠그는" 방식입니다. 인기 도서 하나에 동시에 여러 명이 몰려서 결제를 시도하는 상황(재고 1권 남았는데 100명이 동시에 결제 클릭)에서, 이 락이 있어야 "두 명 모두 재고가 있다고 읽고 둘 다 차감에 성공"하는 초과판매(oversell)를 막을 수 있습니다.

**왜 낙관적 락 하나만으로는 부족한가**: 낙관적 락은 "쓰기 시점에 충돌을 감지"하는 방식이라, 충돌이 잦은 상황(인기상품 한정수량)에서는 재시도 로직이 반복적으로 실패하며 사용자 경험이 나빠집니다. 결제처럼 "이 순간 정말 재고가 있는지 확실히 확인하고 차감까지 한 번에 끝내야 하는" 임계 구간은 비관적 락으로 아예 순서를 강제하는 게 더 적합하다고 판단했습니다.

**Timeout에 대한 솔직한 답변**: 현재 `findByIdForUpdate`에 **명시적인 락 타임아웃 설정은 없습니다.** Oracle/Hibernate 기본 동작에 의존하고 있어서, 어떤 트랜잭션이 락을 오래 잡고 있으면 다른 요청들이 그만큼 대기하게 됩니다.

**개선 방향**: `@QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})`처럼 락 대기시간을 명시적으로 3초 정도로 제한하고, 타임아웃 발생 시 "잠시 후 다시 시도해주세요" 같은 사용자 친화적 메시지로 변환하는 예외처리를 추가하는 게 다음 개선 포인트입니다.

---

### Q5. DB 쿼리 최적화

**참고 파일**: `boot3/src/main/java/com/thejoa703/repository/BookRepository.java`, `boot3/src/main/java/com/thejoa703/repository/OrderItemRepository.java`(`findBestSellerBookIds` 네이티브 쿼리), `boot3/src/main/java/com/thejoa703/entity/Book.java`

**솔직한 현황**: 이 프로젝트는 **명시적인 쿼리 최적화(`@EntityGraph`, `JOIN FETCH`, 인덱스 튜닝)를 적용하지 않았습니다.** 대부분 Spring Data JPA의 파생 쿼리 메서드(`findByTitleContaining` 등)를 그대로 사용했습니다.

**실제로 존재하는 N+1 위험 지점**: 주문 상세 조회(`GET /api/orders/{id}`)에서 `Orders.items`(OrderItem 리스트)를 순회하며 각 `OrderItem.book`을 참조하는데, 이때 지연로딩(LAZY) 설정에 따라 주문상품 개수만큼 개별 `SELECT`가 추가로 나갈 수 있습니다.

**적용했던(혹은 적용 가능한) 최적화 포인트**
1. **부분적으로 이미 반영된 것**: `Book.stock`은 `@OneToOne`이라 기본 EAGER라서, 도서 조회 시 재고까지 한 번에 가져와집니다(즉시 로딩이 오히려 유리한 소규모 연관관계는 EAGER 유지).
2. **적용 가능한 개선**:
   - 주문 상세 조회에 `@EntityGraph(attributePaths = {"items", "items.book"})`를 걸어서 한 번의 쿼리(JOIN FETCH)로 가져오게 만들기
   - 국립중앙도서관 베스트셀러 집계(`findBestSellerBookIds`)처럼 통계성 쿼리는 이미 네이티브 쿼리로 DB에서 직접 GROUP BY 처리하도록 구현되어 있어서, 애플리케이션 레벨에서 전체 데이터를 끌어와 집계하는 비효율은 피했습니다.
   - 페이징 조회(`findByTitleContainingIgnoreCaseOrderByIdDesc`)에 `count` 쿼리와 `content` 쿼리가 분리되는 Spring Data `Page` 특성상, 카운트 쿼리 자체가 무거워질 수 있는데, 데이터량이 커지면 `Slice`로 전환해서 count 쿼리를 아예 생략하는 것도 방법입니다.

**정직하게 말씀드리면**: 이 프로젝트 규모(더미데이터 20~30건)에서는 성능 이슈가 체감되지 않지만, 데이터가 수만 건 이상으로 커지면 위 지점들이 실제 병목이 될 수 있는 부분이라 "인지하고 있고, 우선순위를 매겨 개선 계획이 있다"고 답하는 게 정확합니다.

---

### Q6. 외부 API 예외 처리 & Retry/Fallback

**참고 파일**: `boot3/src/main/java/com/thejoa703/exception/GlobalExceptionHandler.java`, `boot3/src/main/java/com/thejoa703/api/KakaoPayApiService.java`

**현재 구현 — 예외 변환까지는 되어 있음**

카카오페이(`KakaoPayApiService`), 카카오 도서검색(`ApiKakaoBook`), 국립중앙도서관(`NlBookApiService`) 3개 외부 API를 호출하는데, `GlobalExceptionHandler`에서 이런 예외들을 잡아 의미있는 응답으로 변환합니다.

```java
@ExceptionHandler(HttpClientErrorException.Unauthorized.class)
public ResponseEntity<Map<String, String>> handleExternalApiUnauthorized(...) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(createErrorBody("외부 API 인증에 실패했습니다. API 키를 확인해주세요."));
}

@ExceptionHandler({HttpClientErrorException.class, HttpServerErrorException.class})
public ResponseEntity<Map<String, String>> handleExternalApiHttpError(RestClientException ex) { ... }

@ExceptionHandler(ResourceAccessException.class)
public ResponseEntity<Map<String, String>> handleExternalApiNetworkError(...) { ... }
```

**Retry/Circuit Breaker는 미구현**입니다. 카카오 API가 순간적으로 타임아웃되거나 500을 반환해도, 지금은 **재시도 없이 바로 실패 처리**됩니다.

**실제로 겪은 디버깅 경험**: 개발 중 카카오페이가 원인을 알 수 없는 `error_code: -1`을 반환하는 문제가 있었는데, 원인을 추적해보니 **요청 본문 형식 자체가 문제**였습니다(2024년 API 개편으로 `application/x-www-form-urlencoded` → `application/json`으로 바뀐 걸 반영 못 했었음). 이 과정에서 "카카오 응답이 이상하면 우리가 보낸 요청값을 에러 메시지에 그대로 실어서 반환"하도록 개선해서, 다음에 비슷한 문제가 생기면 서버 로그를 뒤질 필요 없이 API 응답만 봐도 원인을 알 수 있게 만들었습니다. (이런 "관찰가능성(observability)을 높이는 방향의 개선 경험"은 실무 감각을 보여주는 좋은 사례입니다)

**개선 방향**: `spring-retry`의 `@Retryable(value = ResourceAccessException.class, maxAttempts = 3, backoff = @Backoff(delay = 500))`을 네트워크 일시 장애(타임아웃)에만 제한적으로 적용하고, 카카오 서버가 4xx(우리 요청 자체가 잘못됨)를 반환하는 경우는 재시도해도 의미가 없으므로 재시도 대상에서 제외하는 식으로 구분해서 적용하는 게 맞다고 생각합니다. Circuit Breaker(resilience4j)는 외부 API 장애가 우리 서비스 전체를 느리게 만드는 것을 막는 용도로, 트래픽이 많아지면 도입을 고려할 부분입니다.

---

### Q7. JPA & 영속성 컨텍스트 1차 캐시

**참고 파일**: `boot3/src/main/java/com/thejoa703/entity/BookStock.java`(`@MapsId` 사례), `boot3/src/main/java/com/thejoa703/entity/Orders.java`(양방향 연관관계 사례), `boot3/src/main/java/com/thejoa703/service/OrderService.java`(`@Transactional` 중복 오류 수정 이력), `boot3/src/test/java/com/thejoa703/Boot2ApplicationTests_5_PaymentEntity.java`(실제 재현 테스트)

**개념**: 영속성 컨텍스트(1차 캐시)는 하나의 트랜잭션(EntityManager) 생명주기 안에서, 같은 PK로 조회한 엔티티를 **항상 동일한 자바 객체 인스턴스로** 재사용하게 해주는 Hibernate의 캐시입니다. 이 덕분에 같은 트랜잭션 안에서 같은 엔티티를 여러 번 조회해도 실제 DB 쿼리는 한 번만 나갑니다.

**개발 중 이 개념 때문에 실제로 겪었던 3가지 버그(면접에서 이야기하기 아주 좋은 소재)**

1. **Detached Entity 문제**: `@OneToOne @MapsId`(자식이 부모 PK를 그대로 공유하는 관계, `BookStock`이 `Book`의 PK를 그대로 씀)에서, 부모 엔티티가 이전 트랜잭션에서 이미 조회되어 **준영속(detached)** 상태가 된 채로 자식을 저장하려 하면 `"detached entity passed to persist"` 예외가 발생했습니다. 원인은 "여러 개의 독립된 트랜잭션에 걸쳐 하나의 엔티티를 재사용"하려 한 것이었고, 트랜잭션 경계를 하나로 통일해서 해결했습니다.

2. **양방향 연관관계 미동기화**: `Orders.items`처럼 `List<OrderItem> items = new ArrayList<>()`로 필드를 초기화해두면, Hibernate는 이걸 "이미 로딩 완료된 컬렉션"으로 취급합니다. 그래서 자식(`OrderItem`)을 만들 때 `orderItem.setOrder(order)`로 자식 쪽에서만 연관관계를 설정하면, **부모의 `items` 리스트는 계속 빈 상태로 남아서** 나중에 `orders.delete()`를 호출했을 때 Hibernate가 "지울 자식이 없다"고 판단해 FK 제약조건 위반이 났습니다. `order.getItems().add(orderItem)`으로 양쪽을 모두 채워야 한다는 JPA의 기본 원칙을 실전에서 체감한 사례입니다.

3. **캐시된 인스턴스로 인한 "삭제됐는데도 조회되는" 문제**: `deleteAllInBatch()`(벌크 연산)로 DB에서는 실제로 삭제됐는데, 같은 트랜잭션 안에서 곧바로 `findById()`를 호출하면 Hibernate가 DB를 다시 조회하지 않고 **1차 캐시에 남아있던(이미 삭제된) 객체를 그대로** 반환해서 "삭제가 안 된 것처럼" 보이는 문제가 있었습니다. `entityManager.flush(); entityManager.clear();`로 1차 캐시를 강제로 비우고 재조회해서 실제 DB 상태를 검증하도록 테스트를 개선했습니다.

4. **(참고: 어노테이션 중복 컴파일 오류)** JPA 자체는 아니지만 관련해서, 기능을 점진적으로 확장하다가 같은 메서드 위에 `@Transactional`을 실수로 두 번 붙인 적이 있습니다(옛 버전 주석+어노테이션을 지우지 않고 그 위에 새 버전을 덧붙임). `@Transactional`은 `@Repeatable`이 아니라서 IDE가 "Duplicate annotation of non-repeatable type" 컴파일 에러로 즉시 잡아줬습니다. **점진적으로 기능을 확장할 때는 기존 코드를 완전히 지우고 새로 쓰는지, 옛 코드 위에 겹쳐 쓰는지를 항상 의식해야 한다**는 걸 다시 확인한 사례입니다.

**이 경험에서 얻은 결론**: JPA는 "객체지향적으로 편하게" 써지지만, 그 편리함의 이면에 있는 영속성 컨텍스트의 생명주기(트랜잭션 경계, 1차 캐시, 더티체킹)를 정확히 이해하지 못하면 오히려 디버깅이 훨씬 어려워진다는 걸 체감했습니다.

---

### Q8. 소프트 삭제(Soft Delete)

**참고 파일**: `boot3/src/main/java/com/thejoa703/entity/Orders.java`(`hiddenByUser` 필드, `columnDefinition` 설정 포함), `boot3/src/main/java/com/thejoa703/service/OrderService.java`(`deleteOrder`), `boot3/src/main/java/com/thejoa703/repository/OrdersRepository.java`, `boot3/src/main/java/com/thejoa703/controller/OrderController.java`(`DELETE /api/orders/{id}`)

**실제 적용 사례 — 주문(Orders) 삭제**

이 프로젝트는 상태에 따라 **하드 삭제와 소프트 삭제를 분기**해서 적용했습니다.

```java
@Column(name = "HIDDEN_BY_USER", nullable = false, columnDefinition = "NUMBER(1) DEFAULT 0")
private boolean hiddenByUser = false;
```

```java
public void deleteOrder(Long userId, Long orderId) {
    Orders order = ordersRepository.findById(orderId)...;
    if (order.getOrderStatus() == OrderStatus.PENDING) {
        // 결제 전 - 실제 거래 기록이 없으므로 진짜로 삭제(하드 삭제)
        orderItemRepository.deleteAll(order.getItems());
        ordersRepository.delete(order);
    } else {
        // 결제완료/취소/실패 - 회계·이력 보존을 위해 DB에는 남기고 "숨기기"만 처리(소프트 삭제)
        order.setHiddenByUser(true);
        ordersRepository.save(order);
    }
}
```

목록 조회 시에는 `findByUser_IdAndHiddenByUserFalseOrderByIdDesc`처럼 조건에 `hiddenByUser = false`를 포함시켜서, 숨긴 주문은 사용자 화면에 다시 안 나오게 합니다.

**소프트 삭제 컬럼을 실제로 추가하면서 겪은 스키마 마이그레이션 버그(면접에서 이야기하기 좋은 실전 경험)**: 이 `hiddenByUser` 필드를 처음 추가했을 때는 `columnDefinition` 없이 `@Column(nullable = false)`만 붙였는데, 이미 주문 데이터가 쌓여있는 `ORDERS` 테이블에 배포하니 `ORA-00904: 부적합한 식별자` 에러가 났습니다. 원인을 추적해보니, `ddl-auto: update`가 생성한 DDL이 `ALTER TABLE ORDERS ADD HIDDEN_BY_USER NUMBER(1) NOT NULL` 형태였는데, **Oracle은 기존 행이 있는 테이블에 기본값 없이 NOT NULL 컬럼을 추가하는 걸 거부합니다**(기존 행들이 채울 값이 없으므로). 문제는 Hibernate의 `update` 모드가 이 개별 DDL 실행 실패를 애플리케이션 기동 자체를 막지 않고 로그 경고로만 넘겨서, "서버는 정상적으로 떴는데 실제로는 컬럼이 없는" 상태가 되어 원인 파악이 까다로웠습니다. `@Column(columnDefinition = "NUMBER(1) DEFAULT 0")`으로 DDL 자체에 기본값을 명시해서, 기존 행에도 안전하게 `0`이 채워지며 컬럼이 추가되도록 고쳐서 해결했습니다. **"자동 스키마 생성(`ddl-auto`)에 의존할 때는, 이미 운영 데이터가 있는 테이블에 컬럼을 추가하는 경우를 항상 염두에 둬야 한다"**는 걸 체감한 사례입니다.

**왜 이렇게 나눴는가**: 결제전(PENDING) 주문은 실제로 돈이 오가거나 재고가 차감된 적이 없는 "빈 기록"이라 지워도 무방하지만, 결제완료 건은 실제 거래·재고차감 이력이라 **회계/감사(audit) 목적으로 DB에서 완전히 지우면 안 된다**고 판단했습니다. 이건 "사용자에게 보여지는 것"과 "실제로 존재하는 것"을 분리하는 소프트 삭제의 전형적인 사용 사례입니다.

**소프트 삭제의 트레이드오프**: 모든 조회 쿼리에 `hiddenByUser = false` 조건을 빠짐없이 넣어야 한다는 부담이 있습니다(하나라도 빠뜨리면 숨겨야 할 데이터가 노출됨). 이 프로젝트 규모에서는 조회 지점이 적어서 수동으로 관리했지만, 규모가 커지면 Hibernate의 `@SQLRestriction`(또는 `@Where`, 구버전) 어노테이션으로 엔티티 레벨에서 자동으로 필터링되게 하는 게 실수를 줄이는 방법입니다.

---

### Q8-1. 이미지 업로드 용량 제한, 확장자(MIME Type) 검증 및 이미지 최적화

**참고 파일**: `boot3/src/main/java/com/thejoa703/util/FileStorageService.java`

**솔직한 현황**: 현재 `FileStorageService`는 **어떤 검증도 하지 않고 파일을 그대로 저장**합니다.

```java
public String upload(MultipartFile file) {
    String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
    Path target = root.resolve(filename);
    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    return "uploads/" + filename;
}
```

- **용량 제한**: 없음 (다만 Spring Boot의 `spring.servlet.multipart.max-file-size` 전역 설정으로 아주 큰 파일은 애초에 컨트롤러 진입 전에 막힐 수 있으나, 도메인 레벨에서 명시적으로 제한하고 있지는 않음)
- **확장자/MIME 검증**: 없음 — 실행파일이나 스크립트를 이미지 확장자로 위장해서 올려도 그대로 저장됩니다. **명백한 보안 취약점**입니다.
- **이미지 최적화(리사이징/압축)**: 없음 — 업로드된 원본 그대로 저장/서빙되어, 큰 이미지를 올리면 그대로 큰 용량으로 응답됩니다.

**개선 방향(실제로 구현한다면 이렇게 접근하겠다고 답변)**
```java
public String upload(MultipartFile file) {
    // 1) 용량 제한
    if (file.getSize() > 5 * 1024 * 1024) {
        throw new IllegalArgumentException("파일 크기는 5MB를 초과할 수 없습니다.");
    }
    // 2) MIME 타입 화이트리스트 검증 (확장자만 보면 위조 가능하므로 실제 콘텐츠 타입 확인)
    String contentType = file.getContentType();
    if (!List.of("image/jpeg", "image/png", "image/webp").contains(contentType)) {
        throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
    }
    // 3) 이미지 리사이징(예: Thumbnailator 라이브러리로 최대 800px로 축소 + 품질 80%로 압축)
    Thumbnails.of(file.getInputStream())
            .size(800, 800)
            .outputQuality(0.8)
            .toFile(target.toFile());
}
```
추가로, 파일 시그니처(매직 넘버, 예: JPEG는 `FF D8 FF`로 시작)까지 검사하면 `Content-Type` 헤더 자체를 조작하는 공격까지 방어할 수 있습니다. 실서비스라면 이 정도까지 고려하는 게 맞다고 생각합니다.


## 3. 프론트엔드 & API/UI (Q9 ~ Q14)

### Q9. RESTful API 설계

**참고 파일**: `boot3/src/main/java/com/thejoa703/controller/CartController.java`, `OrderController.java`, `PaymentController.java`, `BookController.java`

**실제 적용된 원칙**

| 원칙 | 적용 사례 |
|---|---|
| 리소스는 명사, 행위는 HTTP 메서드로 표현 | `POST /api/cart`(담기), `GET /api/cart`(조회), `PATCH /api/cart/{itemId}`(수량수정), `DELETE /api/cart/{itemId}`(삭제) |
| 계층 구조를 URL 경로에 반영 | `/api/payments/kakao/ready`, `/api/payments/kakao/approve` — "결제 중에서도 카카오페이 방식"이라는 계층을 경로로 표현 |
| 상태 변경이 없는 조회는 GET, 생성/변경은 POST·PATCH, 삭제는 DELETE | 전체 API에서 일관되게 지킴 |
| 응답 상태코드의 의미있는 구분 | 생성 성공 200(body 있음), 상태변경류 성공 204(body 없음, 예: 로그아웃·장바구니 삭제·주문취소), 검증 실패 400, 인증 실패 401, 권한 없음 403, 외부 API 실패 502 |

**의도적으로 다르게 처리한 부분**: `DELETE /api/cart/{itemId}`는 204(No Content)를 반환하는데, `DELETE /api/books/{id}`는 삭제한 도서의 `id`를 body에 담아 반환합니다. 같은 DELETE라도 "프론트가 응답을 실제로 활용하는지"에 따라 다르게 설계했습니다 — RESTful 원칙을 기계적으로 따르기보다, 실제 클라이언트 요구에 맞춰 실용적으로 판단했습니다.

**개선 여지**: 에러 응답 형식이 `{"error": "메시지"}`로 다소 단순합니다. 필드별 검증 에러가 여러 개 발생하는 경우(예: 회원가입 폼) `{"errors": [{"field": "email", "message": "..."}]}`처럼 구조화하면 프론트에서 필드별로 에러를 표시하기 더 편해집니다.

---

### Q10. React/Next.js 상태 관리 & 컴포넌트 구조

**참고 파일**: `front3/reducers/cartReducer.js`, `front3/sagas/cartSaga.js`, `front3/components/AppLayout.js`(로그인 상태 복원 로직)

**상태 관리 — Redux Toolkit + Redux-Saga**

- **전역 상태**(Redux): 인증 정보(`authReducer`), 장바구니(`cartReducer`), 주문/결제(`orderReducer`), 도서(`bookReducer`), 공지사항(`noticeReducer`), 게시글(`postReducer`) — 여러 페이지에서 공유되거나, 페이지 이동 후에도 유지돼야 하는 데이터
- **로컬 상태**(`useState`): 폼 입력값, 모달 열림/닫힘, 페이지 내부에서만 쓰이는 UI 상태
- **비동기 흐름**(Redux-Saga): API 호출 → 성공/실패 액션 디스패치까지의 흐름을 `generator` 함수로 명시적으로 관리. `Request/Success/Failure` 3단계 액션 패턴을 전체 도메인에 일관되게 적용

**컴포넌트 구조**: 페이지(`pages/`)는 라우팅과 데이터 페칭(`useEffect` + `dispatch`)만 담당하고, 실제 UI는 `components/`의 재사용 컴포넌트(`BookCoverImage`, `Pagination`, `AppLayout` 등)로 분리했습니다. `AppLayout`이 모든 페이지를 감싸는 공통 레이아웃 역할을 하며, 로그인 상태 복원(아래 참고)도 여기서 처리합니다.

**실제로 겪었던 상태관리 버그(좋은 경험담)**: 로그인 상태가 Redux 메모리에만 있고 `localStorage`에는 Access Token만 저장되는 구조라서, **카카오페이 결제창(외부 도메인)에 갔다가 돌아오는** 것처럼 브라우저가 완전히 새로 페이지를 로드하면 Redux 스토어가 초기화되어 로그인 정보(`user`)가 사라지는 문제가 있었습니다. `AppLayout`에서 "토큰은 있는데 `user`가 없으면 `/auth/me`로 다시 불러온다"는 복원 로직을 추가해서 해결했습니다. 이건 **"전역 상태(메모리)와 영속 저장소(localStorage)의 생명주기가 다르다"**는 걸 실전에서 체감한 사례입니다.

---

### Q11. Axios 통신 & Interceptor 토큰 재발급(Silent Refresh)

**참고 파일**: `front3/api/axios.js`, `boot3/src/main/java/com/thejoa703/controller/UserController.java`(`/auth/logout`, `/auth/refresh`)

**실제 구현**

```js
// 요청 인터셉터 - 모든 요청에 Access Token 자동 첨부
api.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem("accessToken");
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
});

// 응답 인터셉터 - 401이면 Refresh Token으로 조용히 재발급 후 원래 요청 재시도
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true; // 무한루프 방지 플래그
      try {
        const { data } = await api.post("/auth/refresh"); // HttpOnly 쿠키 자동전송
        localStorage.setItem("accessToken", data.accessToken);
        original.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(original); // 원래 요청 재시도
      } catch (refreshErr) {
        localStorage.removeItem("accessToken");
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);
```

**Silent Refresh가 하는 일**: 사용자가 API 호출 도중 Access Token이 만료돼서 401을 받으면, 사용자 눈에는 아무 일도 없었던 것처럼(로그인 화면으로 튕기지 않고) 백그라운드에서 Refresh Token으로 새 Access Token을 발급받아 원래 요청을 자동으로 재시도합니다. `_retry` 플래그로 "재발급 요청 자체도 401이 나서 무한루프에 빠지는 것"을 방지합니다.

**개발 중 실제로 겪은 버그**: 백엔드의 로그아웃 API가 `refreshToken` 쿠키가 없을 때 예외를 던지도록 되어 있어서, 로그아웃 버튼을 눌러도 항상 400 에러가 나며 로그아웃 자체가 실패하는 문제가 있었습니다. 브라우저 콘솔에 단계별 로그를 심어서(`[logout] 1) ...`, `[logout] 2) ...`) 정확히 어느 단계에서 멈추는지 추적한 끝에, 백엔드가 `null`인 토큰을 그대로 파싱하려다 실패하는 게 원인임을 찾아 방어 코드를 추가했습니다. **"프론트가 안 되는 것처럼 보여도, 실제 원인은 백엔드 API 응답 자체"**였던 사례로, 프론트/백엔드 경계를 넘나드는 디버깅 경험이었습니다.

---

### Q12. 실시간 검색 `useCallback`/`useRef` 최적화

**참고 파일**: `front3/components/BookSearchBox.js`

**실제 구현 — `useRef` 기반 디바운스**

```js
const debounceRef = useRef(null);

const handleChange = (e) => {
  const value = e.target.value;
  if (debounceRef.current) clearTimeout(debounceRef.current);

  debounceRef.current = setTimeout(async () => {
    const res = await api.get('/api/books/search', { params: { keyword: value.trim() } });
    setResults(res.data || []);
  }, 250);
};
```

**`useRef`를 쓴 이유**: 디바운스 타이머 ID를 저장해야 하는데, 이 값은 **화면을 다시 그릴(re-render) 필요가 없는 값**입니다. `useState`로 관리하면 타이머 ID가 바뀔 때마다 불필요한 리렌더링이 발생하지만, `useRef`는 값이 바뀌어도 리렌더링을 유발하지 않으면서 컴포넌트 생명주기 동안 값을 유지할 수 있어서 디바운스/스로틀 구현에 적합합니다.

**`useCallback`에 대한 솔직한 답변**: 현재 `handleChange` 함수 자체는 `useCallback`으로 메모이제이션되어 있지 않습니다. 이 검색창 컴포넌트가 부모로부터 `props`를 받지 않고 독립적으로 동작하는 구조라 당장 리렌더링 이슈는 크지 않지만, 만약 이 함수를 `props`로 자식 컴포넌트에 넘긴다면 부모가 리렌더링될 때마다 `handleChange`가 새로 생성되어 자식이 불필요하게 리렌더링될 수 있습니다. `useCallback(fn, [])`로 감싸서 함수 참조를 고정하면, `React.memo`로 감싼 자식 컴포넌트의 불필요한 리렌더링을 막을 수 있습니다 — 이건 이 프로젝트에 당장 필요하진 않았지만, 컴포넌트 트리가 깊어질수록 필요해지는 최적화입니다.

**추가로 적용한 것**: 앞뒤 공백/중복 공백 정리(`keyword.trim().replaceAll("\\s+", " ")`)를 프론트와 백엔드 양쪽에 두어, 사용자가 실수로 공백을 여러 번 입력해도 검색이 실패하지 않도록 했습니다.

---

### Q13. 반응형 UI & Mobile Drawer

**참고 파일**: `front3/components/AppLayout.js`

**구조**: Ant Design의 `Row`/`Col` 반응형 breakpoint(`xs`/`sm`/`md`/`lg`)를 이용해, 화면 폭에 따라 **가로 메뉴(Menu, PC용)**와 **햄버거 버튼 + Drawer(모바일/태블릿용)**를 전환합니다.

```jsx
<Col flex="auto" xs={0} sm={0} md={0} lg={18}>
  <Menu theme="dark" mode="horizontal" items={menuItems} />
</Col>
<Col flex="none" xs={2} lg={0}>
  <Button icon={<MenuOutlined />} onClick={() => setDrawerOpen(true)} />
</Col>
```

**실제로 겪은 반응형 버그**: 처음에는 가로 메뉴 전환 시점을 `md`(768px)로 설정했는데, 정확히 그 지점부터 로고+검색창+메뉴 7개 항목을 한 줄에 다 담기엔 폭이 부족해서, `Row`의 기본 동작(`flex-wrap: wrap`)에 의해 **자동으로 줄바꿈되어 헤더가 2줄(이중)로 보이는** 문제가 있었습니다. 태블릿 폭까지는 계속 햄버거 메뉴를 쓰도록 전환 시점을 `lg`(992px)로 늦추고, `Row`에 `wrap={false}`를 추가로 걸어서 어떤 화면 폭에서도 줄바꿈 자체가 안 일어나도록 이중으로 방어했습니다. **"반응형은 breakpoint 하나만 잘 잡는 게 아니라, 그 경계 지점에서 실제 콘텐츠가 들어갈 공간이 충분한지까지 검증해야 한다"**는 걸 체감한 사례입니다.

---

### Q14. UI 단 권한 분기 처리

**참고 파일**: `front3/pages/books/[id].js`(`isAdmin` 분기), `boot3/src/main/java/com/thejoa703/service/BookService.java`(`@PreAuthorize`), `boot3/src/main/java/com/thejoa703/oauth2/OAuth2SuccessHandler.java`(소셜 가입확인 흐름)

**패턴**: 로그인한 사용자 정보(`user.role`)를 기준으로 조건부 렌더링합니다.

```jsx
const isAdmin = user?.role === "ROLE_ADMIN";
...
{isAdmin && (
  <button onClick={handleDelete}>삭제</button>
)}
```

**중요한 원칙**: **UI 단의 권한 분기는 어디까지나 "사용자 경험을 위한 것"이지, 실제 보안 경계가 아닙니다.** 관리자가 아닌 사용자에게 삭제 버튼을 안 보여주는 건 실수로 클릭하는 걸 막아주는 정도의 의미이고, 실제 권한 검증은 반드시 백엔드(`@PreAuthorize("hasRole('ADMIN')")`)에서 한 번 더 이루어져야 합니다. 이 프로젝트에서도 도서등록/공지작성/재고수정 등 관리자 전용 기능은 프론트의 버튼 숨김과는 별개로, 백엔드 컨트롤러/서비스 레벨에서 `@PreAuthorize`로 이중 검증하고 있습니다. 만약 프론트만 믿고 백엔드 검증을 생략하면, 개발자도구로 API를 직접 호출해서 권한 없이도 관리자 기능을 실행할 수 있는 심각한 보안 허점이 생깁니다.

**소셜로그인 사용자의 권한 분기 특이사항**: 카카오/구글/네이버로 처음 가입하는 사용자는 인증만 통과하면 바로 회원가입되는 게 아니라, **"가입확인(닉네임 확인) 화면"을 한 번 거치도록** 별도 임시토큰(10분 만료) 방식을 추가했습니다. 이렇게 하면 소셜 인증과 실제 회원가입(권한 부여) 시점을 분리해서, 사용자가 원치 않는 가입이 실수로 이루어지는 걸 방지할 수 있습니다.


## 4. CS 및 백엔드/프론트엔드 웹 핵심 기본기 (Q15 ~ Q30)

### Q15. API 정의 및 역할

**API(Application Programming Interface)**는 서로 다른 소프트웨어(또는 소프트웨어의 구성요소)가 **상호작용할 수 있도록 정의된 약속(인터페이스)**입니다. 웹 개발 맥락에서는 보통 클라이언트(프론트엔드)와 서버(백엔드)가, 혹은 서버와 서버끼리(외부 API 연동) 데이터를 주고받는 규격을 의미합니다.

**역할**
1. **관심사 분리**: 프론트엔드는 "어떻게 보여줄지", 백엔드는 "어떻게 처리하고 저장할지"에만 집중할 수 있게 해줍니다.
2. **재사용성**: 하나의 API를 웹, 모바일 앱, 다른 서비스가 동시에 사용할 수 있습니다(이 프로젝트도 같은 REST API를 Next.js 프론트가 소비하지만, 원칙적으로 모바일 앱이 붙어도 그대로 재사용 가능).
3. **캡슐화**: 내부 구현(어떤 DB를 쓰는지, 어떤 언어로 짰는지)을 감추고, 정해진 규격(요청/응답 형식)만 노출합니다.

이 프로젝트에서는 `/api/books`, `/api/cart`, `/api/orders`, `/api/payments/kakao` 처럼 도메인별로 REST API를 설계해서, 프론트(Next.js)가 이 API들을 axios로 호출하는 구조입니다.

---

### Q16. RESTful API 설계 원칙

REST(Representational State Transfer)는 **자원(Resource)을 URI로 표현하고, 그 자원에 대한 행위를 HTTP 메서드로 표현**하는 아키텍처 스타일입니다.

**핵심 원칙**
1. **자원 중심 URI**: URI에는 명사만 사용하고, 동사(행위)는 HTTP 메서드로 표현합니다. (`GET /api/books/{id}` O, `GET /api/getBook?id=1` X)
2. **HTTP 메서드의 의미 준수**: `GET`(조회, 부작용 없음), `POST`(생성), `PUT`(전체교체), `PATCH`(부분수정), `DELETE`(삭제)
3. **무상태성(Stateless)**: 서버는 클라이언트의 이전 요청 상태를 기억하지 않습니다. 매 요청에 필요한 정보(예: JWT 토큰)를 클라이언트가 전부 포함해서 보내야 합니다.
4. **계층 구조 표현**: `/api/orders/{orderId}/items`처럼 자원 간의 소유·포함 관계를 URI 경로로 표현
5. **일관된 상태코드 사용**: 200(성공), 201(생성됨), 204(성공, 본문 없음), 400(잘못된 요청), 401(인증 필요), 403(권한 없음), 404(자원 없음), 500(서버 오류)

이 프로젝트에서는 무상태성 원칙에 따라 JWT를 매 요청 헤더에 담아 인증하고, 사용자 세션 정보를 서버 메모리에 두지 않습니다(Refresh Token만 Redis에 별도 저장하는 것도 "서버가 클라이언트 인증 상태를 기억"하는 게 아니라 "재발급을 위한 별도 저장소"라는 점에서 무상태 원칙과 배치되지 않습니다).

---

### Q17. JWT 구조 (Header, Payload, Signature)

**참고 파일**: `boot3/src/main/java/com/thejoa703/security/JwtProvider.java`

JWT(JSON Web Token)는 `.`으로 구분된 3부분으로 구성되며, 각 부분은 Base64Url로 인코딩됩니다.

```
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiIxMjMiLCJyb2xlIjoiQURNSU4ifQ . SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
      Header                        Payload                              Signature
```

1. **Header**: 토큰의 타입(`"typ": "JWT"`)과 서명 알고리즘(`"alg": "HS256"`)을 담습니다.
2. **Payload**: 실제 담고 싶은 정보(Claim)를 담습니다. `sub`(주체, 보통 사용자 ID), `role`, `iat`(발급시각), `exp`(만료시각) 등. **암호화된 게 아니라 단순 인코딩이라 누구나 디코딩해서 내용을 볼 수 있으므로, 비밀번호처럼 민감한 정보는 절대 넣으면 안 됩니다.**
3. **Signature**: Header와 Payload를 합친 값을 서버만 아는 비밀키(secret)로 서명한 값입니다. 이 서명 덕분에 클라이언트가 Payload 내용을 변조해도(예: role을 ADMIN으로 바꾸는 것), 서버가 재검증할 때 서명이 일치하지 않아 위변조를 즉시 탐지할 수 있습니다.

**이 프로젝트의 적용**: `JwtProvider`에서 `subject`에 사용자 ID를, 커스텀 claim에 `role`, `nickname` 등을 담아 Access Token을 생성하고, 검증 시 `Jwts.parserBuilder().setSigningKey(key)...`로 서명을 확인합니다.

---

### Q18. SSR vs CSR 차이점

| | SSR (Server-Side Rendering) | CSR (Client-Side Rendering) |
|---|---|---|
| 렌더링 위치 | 서버에서 HTML을 완성해서 내려줌 | 브라우저가 JS를 실행해서 HTML을 그림 |
| 최초 로딩 | 서버 응답 시점에 이미 화면 내용이 보임 (체감속도 빠름) | 빈 HTML + JS 다운로드 후에야 화면이 그려짐 (초기 로딩 느릴 수 있음) |
| SEO | 검색엔진이 완성된 HTML을 바로 읽을 수 있어 유리 | 크롤러가 JS를 실행 안 하면 빈 페이지로 인식될 수 있음(개선되고 있지만 여전히 SSR이 유리) |
| 페이지 전환 | 매 페이지마다 서버 왕복 필요(전통적 방식) | 한 번 로드된 이후엔 필요한 데이터만 받아서 부드러운 전환 가능 |
| 서버 부하 | 렌더링을 서버가 담당해서 부하가 더 큼 | 렌더링 부담이 클라이언트(사용자 브라우저)로 분산 |

**Next.js가 이 경계를 흐리는 이유**: Next.js는 `getServerSideProps`(요청마다 서버에서 렌더링), `getStaticProps`(빌드 시 미리 렌더링), 그리고 순수 클라이언트 렌더링을 페이지 단위로 선택할 수 있게 해줍니다. 이 프로젝트는 대부분의 페이지가 클라이언트 사이드에서 Redux-Saga로 데이터를 받아와 그리는 **CSR 방식**을 기본으로 사용하고 있고, 일부(`mypage.js`)에서 `getServerSideProps`를 활용해 서버에서 초기 사용자 정보를 가져오는 하이브리드 방식을 부분적으로 씁니다.

---

### Q19. Spring IoC / DI

**참고 파일**: `boot3/src/main/java/com/thejoa703/service/OrderService.java`(생성자 주입 예시)

**IoC(제어의 역전, Inversion of Control)**: 전통적인 프로그래밍에서는 객체가 필요한 다른 객체를 **자기가 직접 생성**합니다(`new`). IoC는 이 흐름을 뒤집어서, 객체의 생성과 생명주기 관리를 **프레임워크(Spring 컨테이너)**가 대신 담당하게 합니다. 개발자는 "무엇이 필요한지"만 선언하고, "그것을 언제 어떻게 만들지"는 컨테이너에게 맡깁니다.

**DI(의존성 주입, Dependency Injection)**: IoC를 구현하는 대표적인 방법입니다. 객체가 필요로 하는 의존 객체(Dependency)를, 그 객체 스스로 생성하지 않고 **외부(Spring 컨테이너)에서 주입**받는 방식입니다.

```java
@Service
@RequiredArgsConstructor  // Lombok - final 필드에 대한 생성자를 자동 생성 → 생성자 주입
public class OrderService {
    private final OrdersRepository ordersRepository;
    private final BookRepository bookRepository;
    // ordersRepository, bookRepository 를 OrderService 가 직접 new 하지 않고,
    // Spring 이 생성자를 통해 이미 만들어둔 빈(Bean)을 주입해줌
}
```

**왜 이렇게 하는가**: 
1. **결합도 감소**: `OrderService`는 `OrdersRepository`가 "어떤 구현체인지" 몰라도 되고, 인터페이스에만 의존하면 됩니다.
2. **테스트 용이성**: 테스트할 때 실제 구현체 대신 가짜(Mock) 객체를 주입할 수 있습니다(이 프로젝트에서도 `@MockBean`으로 `KakaoPayApiService`를 가짜로 대체해서, 실제 카카오 서버를 호출하지 않고 결제 로직만 검증했습니다).
3. **생명주기 관리 위임**: 싱글톤으로 관리해야 할 객체를 개발자가 직접 챙기지 않아도 Spring이 알아서 하나만 만들어 재사용합니다.

**생성자 주입을 선호하는 이유**: 필드 주입(`@Autowired` 필드)보다 생성자 주입이 권장되는데, 그 이유는 (1) `final` 키워드로 불변성을 보장할 수 있고 (2) 순환 참조가 있으면 애플리케이션 시작 시점에 바로 에러가 나서 조기에 발견 가능하며 (3) 테스트 시 Mock을 주입하기 쉽기 때문입니다.

---

### Q20. Database JOIN 종류 및 차이

**참고 파일**: `boot3/src/main/java/com/thejoa703/entity/Book.java`, `BookStock.java`(연관관계 예시)

| 종류 | 설명 |
|---|---|
| **INNER JOIN** | 두 테이블 모두에 조건이 일치하는 행만 반환 (교집합) |
| **LEFT (OUTER) JOIN** | 왼쪽 테이블의 모든 행을 반환하고, 오른쪽에 일치하는 게 없으면 NULL로 채움 |
| **RIGHT (OUTER) JOIN** | LEFT JOIN의 반대 — 오른쪽 테이블 기준으로 전부 반환 |
| **FULL OUTER JOIN** | 양쪽 테이블의 모든 행을 반환하고, 일치 안 하는 쪽은 NULL (Oracle은 지원, MySQL은 미지원이라 UNION으로 흉내냄) |
| **CROSS JOIN** | 조건 없이 두 테이블의 모든 조합(카티전 곱)을 반환 |
| **SELF JOIN** | 같은 테이블을 자기 자신과 조인 (예: 조직도에서 상사-부하 관계 조회) |

**이 프로젝트의 실제 예시**: `Book`과 `AppUser`(등록한 관리자)는 `@ManyToOne`이라 도서 조회 시 내부적으로 INNER JOIN(또는 EAGER 설정에 따라 즉시 로딩)이 일어납니다. 반대로 `BookItem`이 없는 도서를 포함해서 "전체 도서 + 있으면 재고 정보"를 함께 보고 싶다면 LEFT JOIN이 필요한데, `Book.stock`이 `@OneToOne`으로 필수 연관관계가 아니라면 이런 케이스가 발생할 수 있습니다.

---

### Q21. Database Subquery 개념 및 종류

**서브쿼리(Subquery)**는 하나의 SQL 문 안에 포함된 또 다른 SELECT 문입니다. 메인 쿼리가 서브쿼리의 결과를 활용해서 최종 결과를 만듭니다.

**종류**
1. **단일 행 서브쿼리**: 결과가 1개 행. `WHERE price = (SELECT MAX(price) FROM book)`
2. **다중 행 서브쿼리**: 결과가 여러 행. `IN`, `ANY`, `ALL`과 함께 사용. `WHERE book_id IN (SELECT book_id FROM order_items WHERE quantity > 5)`
3. **상관 서브쿼리(Correlated Subquery)**: 서브쿼리가 메인쿼리의 컬럼을 참조해서, 메인쿼리의 각 행마다 서브쿼리가 다시 실행됨. 예: "각 카테고리별 평균가보다 비싼 도서 찾기"
4. **스칼라 서브쿼리**: SELECT 절 안에 들어가서 값 하나를 반환하는 서브쿼리
5. **FROM 절 서브쿼리(인라인 뷰)**: 서브쿼리의 결과를 마치 하나의 테이블처럼 사용

**이 프로젝트의 실제 예시**: 베스트셀러 집계(`findBestSellerBookIds`)는 네이티브 쿼리로 `ORDER_ITEMS`와 `ORDERS`를 조인하고 `PAID` 상태만 필터링해서 `GROUP BY`로 판매량을 집계하는데, 여기에 "이 도서가 특정 기간 내에 판매됐는지"를 확인하는 서브쿼리를 추가하면 "최근 30일 베스트셀러"처럼 더 정교한 조건을 걸 수 있습니다.

---

### Q22. DML 개념 및 DDL/DCL과의 차이

**참고 파일**: `boot3/src/main/resources/application.yml`(`ddl-auto: update` 설정), `boot3/src/main/java/com/thejoa703/entity/Orders.java`(`columnDefinition` 실전 이슈)

SQL 명령어는 목적에 따라 세 가지로 분류됩니다.

| 구분 | 전체 이름 | 목적 | 대표 명령어 |
|---|---|---|---|
| **DDL** | Data Definition Language | 데이터베이스 구조(스키마) 자체를 정의 | `CREATE`, `ALTER`, `DROP`, `TRUNCATE` |
| **DML** | Data Manipulation Language | 실제 데이터를 조작 | `SELECT`, `INSERT`, `UPDATE`, `DELETE` |
| **DCL** | Data Control Language | 권한을 제어 | `GRANT`, `REVOKE` |

(참고로 `COMMIT`/`ROLLBACK`은 트랜잭션을 제어하는 **TCL**(Transaction Control Language)로 별도 분류하기도 합니다.)

**중요한 차이점**: DDL 명령어(`CREATE`, `ALTER`, `DROP`)는 실행 즉시 자동으로 커밋되는(암묵적 커밋) 경우가 많아서, `ROLLBACK`으로 되돌릴 수 없는 경우가 대부분입니다. 반면 DML(`INSERT`, `UPDATE`, `DELETE`)은 명시적으로 커밋하기 전까지는 롤백이 가능합니다. 이 차이 때문에 운영 환경에서 `TRUNCATE`(DDL)나 `DROP TABLE`은 특히 신중하게 다뤄야 합니다.

**이 프로젝트의 적용**: JPA의 `ddl-auto: update` 설정은 애플리케이션 기동 시 엔티티 변경사항을 자동으로 `ALTER TABLE`(DDL)로 반영해줍니다. 이건 개발 편의성을 위한 것이고, 실제 운영환경에서는 DDL을 자동으로 실행하게 두지 않고 `Flyway`/`Liquibase` 같은 마이그레이션 도구로 버전 관리하며 신중하게 적용하는 게 일반적입니다.


### Q23. Cookie vs Session 차이 및 작동 방식

**참고 파일**: `boot3/src/main/java/com/thejoa703/controller/UserController.java`(Refresh Token 쿠키 설정 부분)

**공통점**: 둘 다 HTTP의 무상태성(Stateless)을 보완해서 "사용자를 식별/기억"하기 위한 기술입니다.

**차이점**

| | Cookie | Session |
|---|---|---|
| 저장 위치 | 클라이언트(브라우저) | 서버 |
| 클라이언트가 갖는 것 | 실제 데이터 자체 | 세션 ID(식별자)만 |
| 보안 | 클라이언트에 데이터가 그대로 있어 상대적으로 취약 (`HttpOnly`, `Secure`로 보완) | 실제 데이터는 서버에 있어 상대적으로 안전 |
| 서버 부하 | 없음(서버가 아무것도 저장 안 함) | 사용자 수만큼 서버 메모리(또는 별도 저장소)를 씀 |
| 확장성 | 유리(서버가 상태를 안 가지므로 서버를 늘려도 문제없음) | 여러 서버로 확장 시 세션 공유 문제 발생(Sticky Session, 세션 클러스터링, 또는 Redis 같은 외부 세션 스토어 필요) |

**작동 방식**: 서버가 응답에 `Set-Cookie` 헤더를 실어 보내면, 브라우저가 이후 같은 도메인으로 요청할 때마다 자동으로 `Cookie` 헤더에 담아 전송합니다. Session은 이 쿠키에 "세션 ID"만 담고, 실제 사용자 데이터는 서버의 세션 저장소에서 그 ID로 조회합니다.

**이 프로젝트의 적용**: Refresh Token을 `HttpOnly` 쿠키로 저장합니다(`httpOnly(true)`로 자바스크립트에서 접근 불가능하게 만들어 XSS 공격으로 토큰이 탈취되는 걸 방어). 다만 이건 전통적인 "세션 기반 인증"은 아니고, 쿠키 안에 JWT 토큰 자체를 담아 서버가 별도 세션 저장소 없이(사실은 Redis에 Refresh Token 저장/검증용으로만 씀) 무상태로 인증을 처리하는 하이브리드 방식입니다.

---

### Q24. JWT vs Cookie/Session 인증 방식 비교

| | Session 기반 | JWT 기반 |
|---|---|---|
| 상태 관리 | Stateful(서버가 세션 저장) | Stateless(서버는 토큰 검증만, 별도 저장 불필요) |
| 확장성 | 서버 여러 대로 늘리면 세션 공유 이슈 | 서버가 어디든 같은 비밀키로 서명 검증만 하면 되므로 수평 확장에 유리 |
| 즉시 무효화(로그아웃) | 서버에서 세션을 지우면 즉시 무효화됨 | 토큰 자체는 만료 전까지 유효 — 즉시 무효화하려면 별도 블랙리스트 필요(Q1 참고) |
| 모바일/다른 클라이언트 지원 | 쿠키 기반이라 모바일 네이티브 앱 등에서 다루기 불편 | 헤더에 담아 보내면 되므로 플랫폼 무관하게 쓰기 쉬움 |
| 토큰 크기 | 세션 ID만 있어서 작음 | Payload에 정보가 들어있어 상대적으로 큼 |

**이 프로젝트가 JWT를 선택한 이유**: 소셜로그인(카카오/구글/네이버) 3사를 함께 지원하면서 인증 로직을 서버 상태와 분리해서 관리하고 싶었고, 향후 모바일 앱이 붙거나 서버를 여러 대로 확장해야 할 때 세션 클러스터링 문제 없이 대응할 수 있다는 점이 컸습니다. 다만 그 대가로 "로그아웃 즉시 무효화"가 완벽하지 않다는 트레이드오프를 안고 있고, 이건 Access Token 만료시간을 짧게(15분) 가져가는 방식으로 절충했습니다.

---

### Q25. CORS 개념 및 해결 경험

**참고 파일**: `boot3/src/main/java/com/thejoa703/config/SecurityConfig.java`(`corsConfigurationSource`), `boot3/src/main/java/com/thejoa703/controller/UserController.java`(쿠키 `Secure` 속성 이슈)

**CORS(Cross-Origin Resource Sharing)**: 브라우저의 기본 보안 정책인 **동일 출처 정책(Same-Origin Policy)**은 "프로토콜+도메인+포트가 모두 같은" 출처(origin)끼리만 자유롭게 요청을 주고받게 허용합니다. 다른 출처로의 요청(Cross-Origin)은 기본적으로 차단되는데, 서버가 명시적으로 "이 출처는 허용한다"고 응답 헤더(`Access-Control-Allow-Origin` 등)를 내려주면 브라우저가 그 요청을 허용합니다. CORS는 이걸 가능하게 하는 **브라우저와 서버 간의 규약**입니다.

**이 프로젝트의 실제 상황**: 프론트(`http://localhost:3000`)와 백엔드(`http://localhost:8080`)가 **포트가 달라서** 서로 다른 출처로 취급됩니다. 그래서 백엔드에서 CORS를 명시적으로 허용해야 합니다.

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:3000"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true); // 쿠키(Refresh Token)를 주고받으려면 필수
    ...
}
```

**`allowCredentials(true)`가 특히 중요했던 이유**: 이 프로젝트는 Refresh Token을 쿠키로 주고받는데, **쿠키를 포함한 Cross-Origin 요청은 `Access-Control-Allow-Credentials: true`가 없으면 브라우저가 아예 쿠키를 전송하지 않습니다.** 이걸 놓치면 로그인은 되는데 Refresh Token만 계속 안 실리는, 원인을 찾기 까다로운 문제가 생깁니다.

**실제로 겪었던 관련 문제**: Refresh Token 쿠키에 `secure(true)`(HTTPS 전용)를 고정해뒀던 게 원인이 되어, `http://localhost`(HTTPS가 아닌) 환경에서 쿠키 자체가 브라우저에 저장/전송되지 않는 문제를 겪었습니다. CORS 설정 자체는 문제없었지만, 쿠키의 `Secure` 속성이 로컬 개발환경과 안 맞았던 사례로, "요청이 도메인을 못 넘는 이유가 CORS만은 아니다"라는 걸 체감했습니다. 서버가 실행되는 호스트명을 확인해서 로컬(`localhost`, `127.0.0.1`)이면 `secure(false)`로 분기하도록 고쳤습니다.

---

### Q26. OAuth 2.0 동작 원리

**참고 파일**: `boot3/src/main/java/com/thejoa703/oauth2/OAuth2SuccessHandler.java`, `UserInfoKakao.java`, `UserInfoGoogle.java`, `UserInfoNaver.java`, `boot3/src/main/resources/application-oauth.yml`

**OAuth 2.0**은 사용자가 자신의 비밀번호를 제3의 서비스(우리 서비스)에 직접 알려주지 않고도, 다른 서비스(카카오/구글/네이버)에 등록된 자신의 신원을 이용해 로그인할 수 있게 해주는 인증/인가 프로토콜입니다.

**Authorization Code Grant 방식의 흐름 (이 프로젝트가 사용하는 방식)**
1. 사용자가 "카카오로 로그인" 클릭 → 우리 서버가 카카오의 인증 페이지로 리다이렉트
2. 사용자가 카카오 계정으로 로그인 + 권한 동의
3. 카카오가 **Authorization Code**(1회용 임시 코드)를 우리 서버의 Redirect URI로 전달
4. 우리 서버가 이 코드와 우리 앱의 Client ID/Secret을 카카오 서버에 보내 **Access Token**으로 교환
5. 우리 서버가 이 Access Token으로 카카오의 사용자 정보 API를 호출해서 이메일/닉네임 등을 받아옴
6. 받아온 정보로 우리 서비스 자체의 회원가입/로그인 처리 후, **우리 서비스만의 JWT**를 발급해서 프론트에 전달

**핵심 포인트**: 4단계에서 알 수 있듯, **Authorization Code가 곧바로 우리에게 전달되는 게 아니라, 코드를 다시 한번 Access Token으로 "교환"하는 과정을 거칩니다.** 이 2단계 절차(Code 발급 → Token 교환) 덕분에, Access Token이 브라우저를 거치지 않고 서버 간 통신(Back-channel)으로만 오가서 URL에 토큰이 노출되는 위험을 줄입니다.

**이 프로젝트에서 소셜로그인에 대해 추가로 설계한 부분**: OAuth 인증이 성공했다고 바로 우리 서비스 회원으로 등록하지 않고, 신규 사용자는 **"닉네임 확인" 중간 단계**(별도의 짧은 만료(10분)의 임시 토큰)를 거치도록 했습니다. 이는 OAuth 표준 흐름 자체는 아니고, "소셜 인증 통과 = 즉시 회원가입"이 아니라 "소셜 인증 통과 후 최소한의 확인을 거쳐 회원가입 완료"로 만드는 이 프로젝트만의 UX 설계입니다.

---

### Q27. React State vs Props & 전역 상태 필요성

**참고 파일**: `front3/reducers/cartReducer.js`, `front3/components/AppLayout.js`, `front3/pages/books/[id].js`

**State**: 컴포넌트 **내부**에서 관리되는, 변할 수 있는 데이터입니다. `useState`로 선언하고, 값이 바뀌면 그 컴포넌트(와 자식들)가 리렌더링됩니다. 컴포넌트가 "스스로 기억하는 것"입니다.

**Props**: 부모 컴포넌트가 자식 컴포넌트에게 **전달**하는 읽기 전용 데이터입니다. 자식은 props를 직접 변경할 수 없고(단방향 데이터 흐름), 값이 필요하면 부모가 넘겨준 콜백 함수를 호출해서 부모의 상태를 변경해달라고 요청해야 합니다.

**전역 상태(Redux 등)가 필요한 이유**: React의 기본 데이터 흐름은 부모→자식으로만 props를 통해 내려가는 단방향 구조입니다. 만약 서로 멀리 떨어진 두 컴포넌트(예: 헤더의 장바구니 아이콘과, 도서 상세페이지의 "담기" 버튼)가 같은 데이터(장바구니 내용)를 공유해야 한다면, props만으로는 최상위 공통 조상까지 값을 끌어올렸다가 다시 여러 단계를 거쳐 내려보내야 합니다(이른바 "Prop Drilling"). 이는 컴포넌트 트리가 깊어질수록 유지보수가 어려워집니다. Redux 같은 전역 상태 관리는 이 문제를 "어디서든 직접 접근 가능한 저장소"를 두어 해결합니다.

**이 프로젝트의 실제 사례**: 장바구니(`cartReducer`)가 정확히 이 케이스입니다. `AppLayout`(헤더의 장바구니 뱃지)과 `pages/cart/index.js`(장바구니 화면)와 `pages/books/[id].js`(담기 버튼)가 서로 부모-자식 관계가 아닌 전혀 다른 컴포넌트인데, 전부 같은 `state.cart` 데이터를 참조해야 해서 Redux 전역 상태로 관리했습니다.

---

### Q28. React Virtual DOM 동작 원리

**참고 파일**: `front3/pages/mypage/orders/index.js`(`key={order.id}` 리스트 렌더링 예시)

**Virtual DOM**은 실제 브라우저의 DOM을 그대로 본뜬 **메모리 상의 가벼운 JS 객체 트리**입니다.

**동작 순서**
1. 상태(State)가 변경되면, React는 새로운 Virtual DOM 트리를 만듭니다.
2. 이전 Virtual DOM 트리와 새 Virtual DOM 트리를 비교(**Diffing**)합니다.
3. 차이가 발견된 부분만 골라내서, 실제 DOM에는 **그 차이 나는 부분만** 반영(**Reconciliation**)합니다.

**왜 이렇게 하는가 — 실제 DOM 조작이 비싼 이유**: 실제 DOM을 직접 조작하면 브라우저가 레이아웃 재계산(Reflow)과 다시 그리기(Repaint)를 수행해야 하는데, 이게 상대적으로 비용이 큰 작업입니다. 만약 상태가 바뀔 때마다 화면 전체를 다시 그린다면 매우 비효율적입니다. Virtual DOM은 메모리상의 가벼운 객체 비교로 "진짜 바뀐 부분"만 정확히 찾아내서, 실제 DOM 조작을 최소화하는 전략입니다.

**Diffing 알고리즘의 핵심 전제**: React는 일반적인 트리 비교(O(n³))가 아니라, 두 가지 휴리스틱(경험적 규칙)을 통해 O(n)으로 성능을 낮춥니다.
1. 서로 다른 타입의 엘리먼트는 완전히 다른 트리를 만든다고 가정하고, 이전 트리를 버리고 새로 만듭니다.
2. 같은 레벨의 리스트를 렌더링할 때는 `key` prop으로 각 항목의 정체성을 유지시켜, 리스트 순서가 바뀌어도 불필요한 재생성 없이 위치만 옮깁니다. (이 프로젝트에서도 `orders.map((order) => <div key={order.id}>...)`처럼 고유 ID를 `key`로 사용해서 리스트 렌더링을 최적화하고 있습니다)

---

### Q29. 객체지향 5대 원칙(SOLID 원칙)이란 무엇인가요?

**참고 파일**: `boot3/src/main/java/com/thejoa703/oauth2/UserInfoOAuth2.java`(추상화), `UserInfoKakao.java`/`UserInfoGoogle.java`/`UserInfoNaver.java`(개방-폐쇄 원칙 구현체)

| 원칙 | 이름 | 핵심 내용 |
|---|---|---|
| **S** | 단일 책임 원칙 (Single Responsibility Principle) | 클래스는 단 하나의 책임(변경 이유)만 가져야 합니다. |
| **O** | 개방-폐쇄 원칙 (Open-Closed Principle) | 확장에는 열려있고, 변경에는 닫혀있어야 합니다 — 기존 코드를 수정하지 않고 새 기능을 추가할 수 있어야 합니다. |
| **L** | 리스코프 치환 원칙 (Liskov Substitution Principle) | 자식 클래스는 부모 클래스가 쓰이는 곳에 대체해서 넣어도 프로그램이 정상 동작해야 합니다. |
| **I** | 인터페이스 분리 원칙 (Interface Segregation Principle) | 클라이언트가 사용하지 않는 메서드에 의존하도록 강제하면 안 됩니다 — 하나의 거대한 인터페이스보다 여러 개의 작은 인터페이스가 낫습니다. |
| **D** | 의존관계 역전 원칙 (Dependency Inversion Principle) | 상위 모듈은 하위 모듈의 구체적인 구현이 아니라, 추상화(인터페이스)에 의존해야 합니다. |

**이 프로젝트에서의 적용 예시**
- **단일 책임**: `Controller`(요청/응답), `Service`(비즈니스 로직), `Repository`(데이터 접근)로 계층을 분리해서, 예를 들어 "결제 승인 로직이 바뀌었다"면 `PaymentService`만 수정하면 되고 `PaymentController`는 건드릴 필요가 없습니다.
- **의존관계 역전**: `OrderService`가 `OrdersRepository`라는 **인터페이스**에 의존하고, Spring Data JPA가 런타임에 실제 구현체를 만들어 주입합니다. `OrderService`는 그 구현체가 JPA인지 다른 무엇인지 몰라도 됩니다(Q19의 DI와 직결).
- **개방-폐쇄**: 카카오/구글/네이버 3사 로그인을 `UserInfoOAuth2`라는 공통 개념(추상화) 아래 `UserInfoKakao`, `UserInfoGoogle`, `UserInfoNaver`로 각각 구현해서, 나중에 네 번째 소셜로그인(예: 애플)이 추가돼도 기존 코드를 수정하지 않고 새 구현체만 추가하면 됩니다.

---

### Q30. 객체지향 프로그래밍(OOP)의 4대 특성은 무엇인가요?

**참고 파일**: `boot3/src/main/java/com/thejoa703/entity/Book.java`(캡슐화), `boot3/src/main/java/com/thejoa703/oauth2/UserInfoOAuth2.java`(다형성), `boot3/src/main/java/com/thejoa703/exception/ResourceNotFoundException.java`(상속)

| 특성 | 핵심 내용 |
|---|---|
| **캡슐화 (Encapsulation)** | 데이터(필드)와 그 데이터를 다루는 메서드를 하나로 묶고, 외부에는 필요한 것만 공개(`public`)하고 나머지는 감춥니다(`private`). |
| **상속 (Inheritance)** | 기존 클래스(부모)의 속성과 기능을 물려받아 새로운 클래스(자식)를 만듭니다. 코드 재사용과 계층 구조 표현에 사용됩니다. |
| **다형성 (Polymorphism)** | 같은 이름의 메서드가 객체의 실제 타입에 따라 다르게 동작합니다(오버라이딩), 혹은 같은 이름의 메서드가 매개변수에 따라 다르게 동작합니다(오버로딩). |
| **추상화 (Abstraction)** | 복잡한 내부 구현은 감추고, 필요한 핵심 기능만 간단한 인터페이스로 노출합니다. |

**이 프로젝트에서의 적용 예시**
- **캡슐화**: 엔티티(`Book`, `Orders` 등)의 필드는 `private`로 감추고, `@Getter`/`@Setter`(Lombok)로만 접근하게 했습니다. 더 나아가 `OrderService.checkPurchasable()`처럼 "가격/재고 검증"이라는 로직을 서비스 내부에 캡슐화해서, 외부(Controller)는 그 세부 검증 로직을 몰라도 됩니다.
- **다형성**: `UserInfoOAuth2` 타입의 변수에 실제로는 `UserInfoKakao`/`UserInfoGoogle`/`UserInfoNaver` 중 어떤 걸 담아도, `userInfo.getEmail()`, `userInfo.getNickname()`을 호출하면 각 구현체에 맞게 카카오/구글/네이버의 서로 다른 응답 구조에서 값을 알맞게 꺼내옵니다. 호출하는 쪽(`OAuth2SuccessHandler`)은 어떤 구현체인지 신경 쓸 필요가 없습니다.
- **추상화**: `BookRepository`, `CartRepository` 같은 Spring Data JPA 인터페이스는 "이 데이터를 어떻게 조회할지"의 세부 구현(SQL 생성, 커넥션 관리)을 완전히 감추고, `findByTitleContainingIgnoreCase(keyword)`처럼 의도가 명확한 메서드 이름만 노출합니다.
- **상속**: 이 프로젝트는 엔티티 간 상속 구조(`@Inheritance`)는 사용하지 않았지만, 예외 클래스 계층(`ResourceNotFoundException extends RuntimeException`)에서 상속을 활용해 "우리 도메인에서 의미있는 예외 타입"을 만들어 `GlobalExceptionHandler`에서 구분해서 처리합니다.
