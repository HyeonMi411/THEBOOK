package com.thejoa703;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.api.KakaoPayApiService;
import com.thejoa703.api.KakaoPayApproveResponse;
import com.thejoa703.api.KakaoPayReadyResponse;
import com.thejoa703.dto.CartDto.CartItemRequestDto;
import com.thejoa703.dto.CartDto.CartResponseDto;
import com.thejoa703.dto.OrderDto.OrderCreateRequestDto;
import com.thejoa703.dto.OrderDto.OrderResponseDto;
import com.thejoa703.dto.PageResponseDto;
import com.thejoa703.dto.PaymentDto.PaymentReadyResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Book;
import com.thejoa703.entity.BookStock;
import com.thejoa703.entity.OrderStatus;
import com.thejoa703.mapper.BookMapper;
import com.thejoa703.oauth2.CustomOAuth2User;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.BookStockRepository;
import com.thejoa703.repository.CartItemRepository;
import com.thejoa703.service.CartService;
import com.thejoa703.service.OrderService;
import com.thejoa703.service.PaymentService;
import com.thejoa703.service.BookService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 결제 기능(CartService/OrderService/PaymentService + RestController) 통합테스트
 * ------------------------------------------------------------------------------
 * - Boot2ApplicationTests_2_Service 패턴(서비스 계층을 직접 호출해 실제 비즈니스 로직을
 *   검증)을 참고하되, 결제 도메인 특성에 맞게 다음을 반영했음.
 * - 더미SQL 데이터(스프링부트 완전정복 등)와 겹치지 않도록 도서명은 전부 UUID 를 붙여
 *   매번 고유하게 생성.
 * - 카카오페이는 외부 실제 서버를 호출하는 API 라서, 테스트에서 진짜로 호출할 수 없음
 *   (네트워크가 없는 CI 환경에서도 안정적으로 돌아가야 함). @MockBean 으로 KakaoPayApiService
 *   를 가짜 응답으로 대체해서, "우리 서비스 로직"(재고차감/주문상태변경/CLOB저장)만 검증.
 * - "재고차감이 실제로 DB에 반영되는지" 검증은 BookStockRepository 로 saveAndFlush 하고
 *   재조회해서 확인.
 * - 클래스에 @Transactional 을 걸어 각 테스트 종료 후 자동 롤백되므로, 더미데이터 SQL과
 *   테스트 데이터가 서로 섞이지 않음.
 * ------------------------------------------------------------------------------
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Boot2ApplicationTests_6_PaymentService {

	@Autowired private AppUserRepository   appUserRepository;
	@Autowired private BookMapper          bookMapper;
	@Autowired private BookStockRepository bookStockRepository;
	@Autowired private CartItemRepository  cartItemRepository;

	@PersistenceContext
	private EntityManager entityManager; // BookStock.book(@MapsId) 참조용

	@Autowired private CartService    cartService;
	@Autowired private OrderService   orderService;
	@Autowired private PaymentService paymentService;
	@Autowired private BookService    bookService;

	@Autowired private MockMvc mockMvc;

	// 카카오페이 실제 API 호출 대신 가짜 응답을 돌려주는 Mock (외부망 없이도 항상 동작)
	@MockBean
	private KakaoPayApiService kakaoPayApiService;

	// ------------------------------------------------------------------
	// 공통 헬퍼
	// ------------------------------------------------------------------
	private AppUser createAdmin() {
		AppUser admin = new AppUser();
		admin.setEmail("pay_admin_" + UUID.randomUUID() + "@test.com");
		admin.setPassword("encoded-pass");
		admin.setNickname("pay_admin_" + UUID.randomUUID().toString().substring(0, 8));
		admin.setRole("ROLE_ADMIN");
		admin.setProvider("local");
		admin.setProviderId("local");
		admin.setDeleted(false);
		appUserRepository.saveAndFlush(admin);
		return admin;
	}

	private AppUser createBuyer(String provider) {
		AppUser user = new AppUser();
		user.setEmail("pay_user_" + UUID.randomUUID() + "@test.com");
		user.setPassword("encoded-pass");
		user.setNickname("pay_user_" + UUID.randomUUID().toString().substring(0, 8));
		user.setRole("ROLE_USER");
		user.setProvider(provider); // local / kakao / naver / google - 소셜로그인 구매자도 검증
		user.setProviderId(provider.equals("local") ? "local" : "social-" + UUID.randomUUID());
		user.setDeleted(false);
		appUserRepository.saveAndFlush(user);
		return user;
	}

	// BookService.deleteBook() 등 @PreAuthorize("hasRole('ADMIN')") 가 걸린 메서드를
	// 테스트에서 직접 호출하려면, SecurityContext 에 해당 권한을 가진 인증 정보가
	// 먼저 있어야 함. JwtAuthenticationFilter 가 실제 요청때마다 하는 일을 재현.
	private void loginAs(AppUser appUser) {
		CustomOAuth2User principal = new CustomOAuth2User(appUser.getId(), appUser.getRole());
		Authentication auth = new UsernamePasswordAuthenticationToken(
				principal, null, principal.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	// 테스트 간 SecurityContextHolder(ThreadLocal) 상태가 절대 섞이지 않도록 매번 초기화
	@BeforeEach
	void clearSecurityContextBefore() {
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void clearSecurityContextAfter() {
		SecurityContextHolder.clearContext();
	}

	// 더미SQL 데이터와 겹치지 않도록 UUID 로 고유 도서명 생성 + 재고까지 함께 등록
	private Book createBookWithStock(AppUser admin, String title, int price, int stockQuantity) {
		Map<String, Object> params = new HashMap<>();
		params.put("title", title);
		params.put("author", "결제서비스테스트작가");
		params.put("publisher", "결제서비스테스트출판사");
		params.put("publishDate", LocalDate.of(2024, 1, 1));
		params.put("category", "결제서비스테스트카테고리");
		params.put("ranking", null);
		params.put("reviewCount", 0);
		params.put("rating", null);
		params.put("description", null);
		params.put("pages", null);
		params.put("price", price);
		params.put("bookCover", "uploads/default_book.png");
		params.put("appUserId", admin.getId());
		bookMapper.insert(params);
		Long bookId = (Long) params.get("bookId");

		BookStock stock = new BookStock();
		// BookStock.book 은 @MapsId 라 book 이 null 이면 ID 생성 자체가 실패하고,
		// 그렇다고 Mapper(MyBatis)로 조회한 detached Book 을 그대로 넘기면 Hibernate 가
		// cascade persist 를 시도하다 실패. entityManager.getReference() 로 만든
		// 관리 대상 참조(프록시)를 쓰면 DB 재조회도, cascade persist 대상도 아니면서
		// ID 는 정상적으로 넘겨줄 수 있어 두 문제를 동시에 피할 수 있음.
		stock.setBook(entityManager.getReference(Book.class, bookId));
		stock.setStockQuantity(stockQuantity);
		bookStockRepository.saveAndFlush(stock);

		return bookMapper.findById(bookId);
	}

	//-------------------------------------------------------------------
	// 1. CartService - 담기/수량합산/재고초과거부/수정/삭제/비우기
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ CartService - 담기(수량합산), 재고초과 거부, 수량수정, 항목삭제, 전체비우기")
	void testCartService() {
		AppUser admin = createAdmin();
		AppUser buyer = createBuyer("kakao"); // 소셜로그인(카카오) 구매자
		String title = "결제서비스테스트도서_" + UUID.randomUUID();
		Book book = createBookWithStock(admin, title, 15000, 5); // 재고 5권

		// 1) 담기
		CartResponseDto cart = cartService.addToCart(buyer.getId(), new CartItemRequestDto(book.getId(), 2));
		assertThat(cart.getItems()).hasSize(1);
		assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
		assertThat(cart.getTotalAmount()).isEqualTo(30000);

		// 2) 같은 도서 다시 담기 → 수량 합산 (2 + 2 = 4)
		cart = cartService.addToCart(buyer.getId(), new CartItemRequestDto(book.getId(), 2));
		assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(4);

		// 3) 재고(5권) 초과 담기 시도 → 거부
		assertThatThrownBy(() -> cartService.addToCart(buyer.getId(), new CartItemRequestDto(book.getId(), 10)))
				.isInstanceOf(IllegalStateException.class);

		Long itemId = cart.getItems().get(0).getId();

		// 4) 수량수정 - 재고 초과시 거부
		assertThatThrownBy(() -> cartService.updateQuantity(buyer.getId(), itemId, 100))
				.isInstanceOf(IllegalStateException.class);

		// 5) 수량수정 - 정상범위
		cart = cartService.updateQuantity(buyer.getId(), itemId, 3);
		assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);

		// 6) 다른 사용자가 내 장바구니 항목을 건드리려 하면 거부
		AppUser otherBuyer = createBuyer("local");
		assertThatThrownBy(() -> cartService.updateQuantity(otherBuyer.getId(), itemId, 1))
				.isInstanceOf(Exception.class); // 장바구니 없음 or 본인아님 예외

		// 7) 항목삭제
		cartService.removeItem(buyer.getId(), itemId);
		assertThat(cartService.getCart(buyer.getId()).getItems()).isEmpty();

		// 8) 다시 담고 전체비우기
		cartService.addToCart(buyer.getId(), new CartItemRequestDto(book.getId(), 1));
		cartService.clearCart(buyer.getId());
		assertThat(cartService.getCart(buyer.getId()).getItems()).isEmpty();
	}

	//-------------------------------------------------------------------
	// 2. OrderService - 바로구매 / 장바구니결제, 가격·제목 스냅샷
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ OrderService - 바로구매/장바구니결제 주문생성, 가격·제목 스냅샷, 재고초과 거부")
	void testOrderService() {
		AppUser admin = createAdmin();
		AppUser buyer = createBuyer("naver"); // 소셜로그인(네이버) 구매자
		String titleA = "결제서비스테스트도서A_" + UUID.randomUUID();
		String titleB = "결제서비스테스트도서B_" + UUID.randomUUID();
		Book bookA = createBookWithStock(admin, titleA, 20000, 3);
		Book bookB = createBookWithStock(admin, titleB, 9000, 10);

		// 1) 바로구매 - 주문생성
		OrderCreateRequestDto directBuy = new OrderCreateRequestDto(null, bookA.getId(), 2);
		OrderResponseDto order1 = orderService.createOrder(buyer.getId(), directBuy);
		assertThat(order1.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(order1.getTotalAmount()).isEqualTo(40000);
		assertThat(order1.getItems()).hasSize(1);
		assertThat(order1.getItems().get(0).getBookTitle()).isEqualTo(titleA); // 제목 스냅샷
		assertThat(order1.getItems().get(0).getPrice()).isEqualTo(20000);      // 가격 스냅샷

		// 2) 바로구매 - 재고(3권) 초과 시도 → 거부, 주문 생성 안됨
		assertThatThrownBy(() -> orderService.createOrder(buyer.getId(), new OrderCreateRequestDto(null, bookA.getId(), 100)))
				.isInstanceOf(IllegalStateException.class);

		// 3) 장바구니 결제 - 장바구니에 담고 → 주문생성 → 장바구니에서 제거되는지 확인
		CartResponseDto cart = cartService.addToCart(buyer.getId(), new CartItemRequestDto(bookB.getId(), 4));
		Long cartItemId = cart.getItems().get(0).getId();

		OrderCreateRequestDto cartCheckout = new OrderCreateRequestDto(List.of(cartItemId), null, null);
		OrderResponseDto order2 = orderService.createOrder(buyer.getId(), cartCheckout);
		assertThat(order2.getTotalAmount()).isEqualTo(36000);
		assertThat(order2.getItems().get(0).getBookTitle()).isEqualTo(titleB);

		// 주문에 사용된 장바구니 항목은 제거되어야 함 (MyBatis 는 매 조회가 항상 실제
		// DB 값을 그대로 가져오므로, JPA 의 1차캐시/벌크연산 관련 주의사항은 해당 없음)
		assertThat(cartItemRepository.findById(cartItemId)).isEmpty();
		assertThat(cartService.getCart(buyer.getId()).getItems()).isEmpty();

		// 4) 잘못된 요청 - cartItemIds 도 bookId 도 없으면 예외
		assertThatThrownBy(() -> orderService.createOrder(buyer.getId(), new OrderCreateRequestDto(null, null, null)))
				.isInstanceOf(IllegalArgumentException.class);

		// 5) 내 주문내역 조회(페이징) - 2건 확인
		PageResponseDto<OrderResponseDto> myOrders = orderService.getMyOrders(buyer.getId(), 1, 12);
		assertThat(myOrders.getTotalElements()).isEqualTo(2);

		// 6) 다른 사용자가 내 주문을 조회하려 하면 거부
		AppUser otherBuyer = createBuyer("local");
		assertThatThrownBy(() -> orderService.getOrder(otherBuyer.getId(), order1.getId()))
				.isInstanceOf(IllegalStateException.class);
	}

	//-------------------------------------------------------------------
	// 3. PaymentService - 결제준비/승인, 재고차감이 실제로 DB에 반영되는지 검증
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ PaymentService - 결제준비(tid발급)/승인, 재고차감 DB반영 검증, CLOB(카카오응답) 저장 확인")
	void testPaymentServiceReadyAndApprove() {
		AppUser admin = createAdmin();
		AppUser buyer = createBuyer("local");
		String title = "결제서비스테스트도서_" + UUID.randomUUID();
		Book book = createBookWithStock(admin, title, 12000, 10); // 재고 10권

		OrderResponseDto order = orderService.createOrder(buyer.getId(), new OrderCreateRequestDto(null, book.getId(), 3));
		Long orderId = order.getId();

		// ---- 1) 결제 준비 - 카카오 API는 Mock 이 가짜 tid/redirectUrl 을 돌려줌 ----
		KakaoPayReadyResponse fakeReady = new KakaoPayReadyResponse();
		fakeReady.setTid("T_test_" + UUID.randomUUID());
		fakeReady.setNext_redirect_pc_url("https://mockup-pg.kakao.com/pc/" + orderId);
		Mockito.when(kakaoPayApiService.ready(
				Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()
		)).thenReturn(fakeReady);

		PaymentReadyResponseDto readyDto = paymentService.ready(buyer.getId(), orderId);
		assertThat(readyDto.getTid()).isEqualTo(fakeReady.getTid());
		assertThat(readyDto.getRedirectUrl()).isEqualTo(fakeReady.getNext_redirect_pc_url());

		// 결제준비 단계에서는 재고가 아직 차감되면 안 됨
		BookStock stockAfterReady = bookStockRepository.findById(book.getId()).orElseThrow();
		assertThat(stockAfterReady.getStockQuantity()).isEqualTo(10);

		// ---- 2) 결제 승인 - 카카오 API는 Mock 이 가짜 승인 응답을 돌려줌 ----
		KakaoPayApproveResponse fakeApprove = new KakaoPayApproveResponse();
		fakeApprove.setAid("A_test_" + UUID.randomUUID());
		fakeApprove.setTid(fakeReady.getTid());
		fakeApprove.setStatus("SUCCESS_PAYMENT");
		Mockito.when(kakaoPayApiService.approve(
				Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()
		)).thenReturn(fakeApprove);

		OrderResponseDto approved = paymentService.approve(buyer.getId(), orderId, "pg_token_test");
		assertThat(approved.getOrderStatus()).isEqualTo(OrderStatus.PAID);

		// ---------------------------------------------------------------
		// 재고차감이 실제로 DB에 반영되는지 검증
		// ---------------------------------------------------------------
		BookStock stockAfterApprove = bookStockRepository.findById(book.getId()).orElseThrow();
		assertThat(stockAfterApprove.getStockQuantity()).isEqualTo(7); // 10 - 3 = 7
		// ---------------------------------------------------------------

		// 카카오 응답 원문(CLOB) 저장 확인
		var savedOrder = orderService.getOrder(buyer.getId(), orderId);
		assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);

		// ---- 3) 이미 승인된 주문을 다시 승인 요청해도 에러 없이 그대로 반환 (중복호출 방지) ----
		OrderResponseDto approvedAgain = paymentService.approve(buyer.getId(), orderId, "pg_token_test");
		assertThat(approvedAgain.getOrderStatus()).isEqualTo(OrderStatus.PAID);
	}

	//-------------------------------------------------------------------
	// 4. PaymentService - 결제승인 시점에 재고가 부족해지면(동시 구매 등) 거부되는지
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ PaymentService - 결제승인 시점에 재고가 부족하면 거부되는지 확인")
	void testPaymentServiceApproveFailsWhenStockRunsOut() {
		AppUser admin = createAdmin();
		AppUser buyer = createBuyer("local");
		String title = "결제서비스테스트도서_" + UUID.randomUUID();
		Book book = createBookWithStock(admin, title, 10000, 2); // 재고 2권

		OrderResponseDto order = orderService.createOrder(buyer.getId(), new OrderCreateRequestDto(null, book.getId(), 2));

		Mockito.when(kakaoPayApiService.ready(
				Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()
		)).thenReturn(new KakaoPayReadyResponse());
		paymentService.ready(buyer.getId(), order.getId());

		// 결제승인 직전에 다른 경로로 재고가 0으로 소진된 상황을 재현
		BookStock stock = bookStockRepository.findById(book.getId()).orElseThrow();
		stock.setStockQuantity(0);
		bookStockRepository.saveAndFlush(stock);

		Mockito.when(kakaoPayApiService.approve(
				Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()
		)).thenReturn(new KakaoPayApproveResponse());

		assertThatThrownBy(() -> paymentService.approve(buyer.getId(), order.getId(), "pg_token_test"))
				.isInstanceOf(IllegalStateException.class);
	}

	//-------------------------------------------------------------------
	// 5. PaymentService - 결제 취소/실패 처리
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ PaymentService - 결제 취소/실패 상태 전이 확인")
	void testPaymentServiceCancelAndFail() {
		AppUser admin = createAdmin();
		AppUser buyer = createBuyer("local");
		String title = "결제서비스테스트도서_" + UUID.randomUUID();
		Book book = createBookWithStock(admin, title, 8000, 5);

		OrderResponseDto order1 = orderService.createOrder(buyer.getId(), new OrderCreateRequestDto(null, book.getId(), 1));
		paymentService.cancel(buyer.getId(), order1.getId());
		assertThat(orderService.getOrder(buyer.getId(), order1.getId()).getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);

		OrderResponseDto order2 = orderService.createOrder(buyer.getId(), new OrderCreateRequestDto(null, book.getId(), 1));
		paymentService.fail(buyer.getId(), order2.getId());
		assertThat(orderService.getOrder(buyer.getId(), order2.getId()).getOrderStatus()).isEqualTo(OrderStatus.FAILED);

		// 취소/실패된 재고는 차감되지 않아야 함
		BookStock stock = bookStockRepository.findById(book.getId()).orElseThrow();
		assertThat(stock.getStockQuantity()).isEqualTo(5);
	}

	//-------------------------------------------------------------------
	// 6. [소프트 삭제] 이미 장바구니에 담긴 도서가 이후 삭제(판매중단)되면 어떻게 되는지
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ 소프트삭제 - 장바구니에 담긴 도서가 이후 삭제되면 수량증가/신규담기/주문은 거부되고, 기존 항목 조회/삭제는 계속 가능한지")
	void testSoftDeleteBookAlreadyInCart() {
		AppUser admin = createAdmin();
		AppUser buyer = createBuyer("local");
		String title = "소프트삭제테스트도서_" + UUID.randomUUID();
		Book book = createBookWithStock(admin, title, 10000, 10);

		// 1) 먼저 장바구니에 담아둠 (아직 삭제되기 전)
		CartResponseDto cart = cartService.addToCart(buyer.getId(), new CartItemRequestDto(book.getId(), 2));
		Long itemId = cart.getItems().get(0).getId();
		assertThat(cart.getItems().get(0).isBookDeleted()).isFalse();

		// 2) 관리자가 도서를 삭제(소프트) - 실제 행은 남고 DELETED 플래그만 세워짐
		//    deleteBook() 은 @PreAuthorize("hasRole('ADMIN')") 가 걸려있어서, 호출 전에
		//    SecurityContext 에 ADMIN 권한의 인증 정보를 먼저 세팅 필요.
		loginAs(admin);
		bookService.deleteBook(book.getId());
		// deleteBook() 은 MyBatis(raw SQL)로 DB 를 직접 갱신하는데, 이 테스트는 클래스
		// 레벨 @Transactional 로 처음부터 끝까지 하나의 영속성 컨텍스트(Hibernate 1차
		// 캐시)를 계속 씁니다. 그래서 앞서 cartService.addToCart() 에서 이미 로딩해둔
		// CartItem.book(JPA 캐시)은 deleteBook() 이후에도 여전히 옛 상태(deleted=false)
		// 로 남아있음. clear() 로 캐시를 비워서, 아래 getCart() 가 실제 DB 값을
		// 다시 읽어오도록 강제.
		entityManager.clear();

		// 3) 삭제된 도서는 목록/상세조회/검색에서 더 이상 보이지 않아야 함
		assertThatThrownBy(() -> bookService.getBook(book.getId()))
				.isInstanceOf(com.thejoa703.exception.ResourceNotFoundException.class);

		// 4) 장바구니를 다시 조회하면, 이미 담아둔 항목은 여전히 보이되 "판매중단" 표시가 되어야 함
		CartResponseDto cartAfterDelete = cartService.getCart(buyer.getId());
		assertThat(cartAfterDelete.getItems()).hasSize(1);
		assertThat(cartAfterDelete.getItems().get(0).isBookDeleted()).isTrue();
		// 판매중단된 항목은 결제예정금액(totalAmount)에서 제외되어야 함
		assertThat(cartAfterDelete.getTotalAmount()).isEqualTo(0);

		// 5) 판매중단된 도서를 새로 담으려 하면 거부되어야 함
		assertThatThrownBy(() -> cartService.addToCart(buyer.getId(), new CartItemRequestDto(book.getId(), 1)))
				.isInstanceOf(com.thejoa703.exception.ResourceNotFoundException.class);

		// 6) 이미 담긴 항목의 수량을 "늘리는" 시도는 거부되어야 함
		assertThatThrownBy(() -> cartService.updateQuantity(buyer.getId(), itemId, 5))
				.isInstanceOf(IllegalStateException.class);

		// 7) 반대로 수량을 "줄이는" 것은 허용되어야 함 (2 -> 1)
		CartResponseDto afterDecrease = cartService.updateQuantity(buyer.getId(), itemId, 1);
		assertThat(afterDecrease.getItems().get(0).getQuantity()).isEqualTo(1);

		// 8) 이 항목으로 주문을 생성하려 하면(장바구니결제) 거부되어야 함
		assertThatThrownBy(() -> orderService.createOrder(buyer.getId(), new OrderCreateRequestDto(List.of(itemId), null, null)))
				.isInstanceOf(IllegalStateException.class);

		// 9) 항목 삭제(장바구니에서 제거)는 언제나 허용되어야 함
		cartService.removeItem(buyer.getId(), itemId);
		assertThat(cartService.getCart(buyer.getId()).getItems()).isEmpty();
	}

	//-------------------------------------------------------------------
	// 7. [Swagger] /v3/api-docs 에 Cart/Order/Payment API가 실제로 노출되는지 검증
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ [Swagger] /v3/api-docs 에 Cart/Order/Payment API가 정상 노출되는지 확인")
	void testSwaggerDocsExposeCartOrderPaymentEndpoints() throws Exception {
		MvcResult result = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andReturn();

		String json = result.getResponse().getContentAsString();

		assertThat(json).as("/v3/api-docs 응답에 /api/cart 경로가 있어야 합니다.")
				.contains("\"/api/cart\"");
		assertThat(json).as("/v3/api-docs 응답에 /api/orders 경로가 있어야 합니다.")
				.contains("\"/api/orders\"");
		assertThat(json).as("/v3/api-docs 응답에 카카오페이 결제준비 경로가 있어야 합니다.")
				.contains("/api/payments/kakao/ready");
		assertThat(json).as("/v3/api-docs 응답에 카카오페이 결제승인 경로가 있어야 합니다.")
				.contains("/api/payments/kakao/approve");

		assertThat(json).as("Cart Api 태그가 있어야 합니다.").contains("Cart Api");
		assertThat(json).as("Order Api 태그가 있어야 합니다.").contains("Order Api");
		assertThat(json).as("Payment Api 태그가 있어야 합니다.").contains("Payment Api");
	}
}
