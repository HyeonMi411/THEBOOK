package com.thejoa703;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Book;
import com.thejoa703.entity.BookStock;
import com.thejoa703.entity.Cart;
import com.thejoa703.entity.CartItem;
import com.thejoa703.entity.OrderItem;
import com.thejoa703.entity.OrderStatus;
import com.thejoa703.entity.Orders;
import com.thejoa703.mapper.BookStockMapper;
import com.thejoa703.mapper.CartItemMapper;
import com.thejoa703.mapper.CartMapper;
import com.thejoa703.mapper.OrderItemMapper;
import com.thejoa703.mapper.OrdersMapper;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.BookRepository;
import com.thejoa703.repository.BookStockRepository;
import com.thejoa703.repository.CartItemRepository;
import com.thejoa703.repository.CartRepository;
import com.thejoa703.repository.OrderItemRepository;
import com.thejoa703.repository.OrdersRepository;

/**
 * 결제 기능(BookStock/Cart/CartItem/Orders/OrderItem)  Entity - Repository - Mapper  통합테스트
 * ------------------------------------------------------------------------------
 * - Boot2ApplicationTests_1_Entity 패턴을 참고하되, @BeforeEach 로 공통데이터를 미리 만들지
 *   않고 각 테스트메서드 안에서 필요한 데이터를 직접 생성합니다.
 * - 원칙적으로 클래스에 @Transactional 을 걸어 자동롤백시키는 대신, 매 테스트가 끝난 뒤
 *   @AfterEach 에서 실제 커밋된 데이터를 지워서 초기화합니다. (JPA 쓰기를 "실제 커밋된 상태"
 *   기준으로 검증하고, MyBatis 매퍼도 그 커밋된 데이터를 정상적으로 읽어오는지 확인하기 위함)
 * - ★단, testBookStockRepository() / testPaymentMyBatisMappers() 두 개는 예외적으로
 *   메서드 단위 @Transactional 을 붙였습니다. BookStock.book 은 @OneToOne @MapsId 관계라서,
 *   자식(BookStock)을 저장하는 시점에 부모(Book)가 "같은 영속성 컨텍스트 안에서 계속
 *   매니지드 상태"여야 합니다. @Transactional 없이 여러 리포지토리 호출을 이어서 하면
 *   각 호출마다 트랜잭션이 따로 열렸다 닫히면서 Book 이 detached 되어
 *   "detached entity passed to persist" 에러가 납니다. 이 두 테스트만 @Transactional 로
 *   하나의 트랜잭션을 유지시켜 이 문제를 피했습니다 (스프링 테스트의 @Transactional 은
 *   기본적으로 테스트 종료 후 자동 롤백되므로, 이 두 테스트에 한해서는 @AfterEach 가
 *   지울 데이터가 없어 그냥 통과합니다 - 무해합니다).
 * - MyBatis 매퍼(BookStockMapper/CartMapper/CartItemMapper/OrdersMapper/OrderItemMapper)는
 *   전부 조회전용이므로, JPA로 저장한 데이터를 MyBatis 로도 똑같이 읽어올 수 있는지만
 *   검증합니다 (테이블명/컬럼명 불일치를 가장 확실하게 잡아내는 방법입니다). 단,
 *   testPaymentMyBatisMappers() 는 @Transactional 안에서 "저장 직후 바로 조회"를 하므로,
 *   JPA 가 아직 플러시하지 않아 MyBatis 조회에 안 보이는 일이 없도록 save() 대신
 *   saveAndFlush() 를 사용해 매번 즉시 DB에 반영합니다.
 * ------------------------------------------------------------------------------
 */
@SpringBootTest
class Boot2ApplicationTests_5_PaymentEntity {

	@Autowired private AppUserRepository   appUserRepository;
	@Autowired private BookRepository      bookRepository;
	@Autowired private BookStockRepository bookStockRepository;
	@Autowired private CartRepository      cartRepository;
	@Autowired private CartItemRepository  cartItemRepository;
	@Autowired private OrdersRepository    ordersRepository;
	@Autowired private OrderItemRepository orderItemRepository;

	@Autowired private BookStockMapper bookStockMapper;
	@Autowired private CartMapper      cartMapper;
	@Autowired private CartItemMapper  cartItemMapper;
	@Autowired private OrdersMapper    ordersMapper;
	@Autowired private OrderItemMapper orderItemMapper;

	// ------------------------------------------------------------------
	// 공통 헬퍼 : 관리자(도서등록용) / 구매자(장바구니·주문용) 생성
	// ------------------------------------------------------------------
	private AppUser createAdmin() {
		AppUser admin = new AppUser();
		admin.setEmail("pay_admin_" + UUID.randomUUID() + "@test.com");
		admin.setPassword("encoded-pass");
		admin.setNickname("pay_admin_" + UUID.randomUUID().toString().substring(0, 8));
		admin.setRole("ROLE_ADMIN");
		admin.setProvider("local");
		admin.setProviderId("local");
		return appUserRepository.save(admin);
	}

	private AppUser createBuyer() {
		AppUser user = new AppUser();
		user.setEmail("pay_user_" + UUID.randomUUID() + "@test.com");
		user.setPassword("encoded-pass");
		user.setNickname("pay_user_" + UUID.randomUUID().toString().substring(0, 8));
		user.setRole("ROLE_USER");
		user.setProvider("kakao"); // ★소셜로그인(카카오) 구매자도 정상 동작하는지 확인
		user.setProviderId("social-" + UUID.randomUUID());
		return appUserRepository.save(user);
	}

	private Book createBook(AppUser admin, String title, int price) {
		Book book = new Book();
		book.setTitle(title);
		book.setAuthor("결제테스트작가");
		book.setPublisher("결제테스트출판사");
		book.setPublishDate(LocalDate.of(2024, 1, 1));
		book.setCategory("결제테스트카테고리");
		book.setPrice(price);
		book.setUser(admin);
		return bookRepository.save(book);
	}

	// ------------------------------------------------------------------
	// @AfterEach : 매 테스트 종료 후 DB 초기화 (자식 → 부모 순서로 삭제)
	// ------------------------------------------------------------------
	@AfterEach
	void tearDown() {
		orderItemRepository.deleteAll();
		ordersRepository.deleteAll();
		cartItemRepository.deleteAll();
		cartRepository.deleteAll();
		bookStockRepository.deleteAll();
		bookRepository.deleteAll();
		appUserRepository.deleteAll();
	}

	//-------------------------------------------------------------------
	// 1. BookStock - 1:1(Book), 낙관적 락(@Version), 비관적 락 조회
	//-------------------------------------------------------------------
	// ★이 테스트만 별도로 @Transactional 을 붙였습니다. BookStock.book 은 @OneToOne @MapsId
	//   관계라서, 자식(BookStock)을 저장할 때 부모(Book)가 "같은 영속성 컨텍스트 안에서
	//   계속 매니지드 상태"여야 합니다. 그런데 findById() 로 재조회해도 그 조회 자체가
	//   (클래스에 @Transactional 이 없으므로) 별도의 독립 트랜잭션이라, 조회가 끝나는 순간
	//   결과가 다시 detached 되어버려 "detached entity passed to persist" 에러가 재발합니다.
	//   메서드에 @Transactional 을 붙이면, 이 메서드 안의 모든 리포지토리 호출이 "하나의"
	//   트랜잭션에 참여(Propagation.REQUIRED, 스프링 데이터 JPA 기본값)하게 되어 book 이
	//   메서드가 끝날 때까지 계속 매니지드 상태로 유지됩니다.
	//   (스프링 테스트의 @Transactional 은 기본적으로 테스트 종료 후 자동 롤백되므로,
	//    아래 @AfterEach 는 이 테스트에 한해서는 지울 데이터가 없어 그냥 통과합니다 - 무해합니다)
	//-------------------------------------------------------------------
	@Test
	@Transactional
	@DisplayName("■ BookStock - Book과 1:1(@MapsId), 재고 증감, @Version 증가, 비관적 락 조회, Book 삭제시 cascade")
	void testBookStockRepository() {
		AppUser admin = createAdmin();
		Book book = createBook(admin, "결제테스트도서_" + UUID.randomUUID(), 15000);

		// 1) 등록 - @MapsId 로 BookStock.bookId 가 Book.id 와 동일하게 세팅되는지 확인
		BookStock stock = new BookStock();
		stock.setBook(book);
		stock.setStockQuantity(10);
		bookStockRepository.save(stock);

		// ★JPA 는 연관관계의 "주인(BookStock.book)" 쪽만 설정한다고 반대쪽(Book.stock)을
		//   자동으로 채워주지 않습니다. 양방향을 메모리에서도 맞춰줘야, 나중에 Book 삭제시
		//   cascade 가 이 book.stock 을 보고 자식이 있다는 걸 알 수 있습니다. (같은 트랜잭션
		//   안에서는 findById() 로 재조회해도 1차 캐시 때문에 같은 인스턴스가 반환되어
		//   재조회로는 해결이 안 되므로, 직접 채워주는 것이 가장 확실합니다)
		book.setStock(stock);

		assertThat(stock.getBookId()).isEqualTo(book.getId()); // PK 공유 확인
		assertThat(stock.getVersion()).isNotNull();

		// 2) 단건조회
		BookStock found = bookStockRepository.findById(book.getId()).orElseThrow();
		assertThat(found.getStockQuantity()).isEqualTo(10);
		Long versionBeforeUpdate = found.getVersion();

		// 3) 재고 차감(수정) - @Version 이 자동으로 증가하는지 확인 (낙관적 락 동작 확인)
		found.setStockQuantity(found.getStockQuantity() - 3);
		bookStockRepository.saveAndFlush(found);
		assertThat(found.getStockQuantity()).isEqualTo(7);
		assertThat(found.getVersion()).isGreaterThan(versionBeforeUpdate);

		// 4) 비관적 락 조회 - 결제승인 시점에 재고행을 잠그고 읽는 시나리오
		BookStock locked = bookStockRepository.findByIdForUpdate(book.getId()).orElseThrow();
		assertThat(locked.getStockQuantity()).isEqualTo(7);

		// 5) Book 삭제시 BookStock 도 cascade 로 함께 삭제되는지 확인
		bookRepository.delete(book);
		bookRepository.flush();
		assertThat(bookStockRepository.findById(book.getId())).isEmpty();
	}

	//-------------------------------------------------------------------
	// 2. Cart + CartItem - 1:1(AppUser), 1:N(Cart-CartItem), 선택삭제/전체삭제/cascade
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ Cart/CartItem - 사용자당 1개(1:1), 담기/조회/선택삭제/전체삭제, Cart 삭제시 cascade")
	void testCartAndCartItemRepository() {
		AppUser admin = createAdmin();
		AppUser buyer = createBuyer();
		Book bookA = createBook(admin, "장바구니테스트A_" + UUID.randomUUID(), 12000);
		Book bookB = createBook(admin, "장바구니테스트B_" + UUID.randomUUID(), 18000);

		// 1) 장바구니가 아직 없다는 것 확인 후 생성
		assertThat(cartRepository.existsByUser_Id(buyer.getId())).isFalse();

		Cart cart = new Cart();
		cart.setUser(buyer);
		cartRepository.save(cart);

		assertThat(cartRepository.existsByUser_Id(buyer.getId())).isTrue();
		assertThat(cartRepository.findByUser_Id(buyer.getId())).isPresent();

		// 2) 장바구니에 도서 2권 담기
		CartItem itemA = new CartItem();
		itemA.setCart(cart);
		itemA.setBook(bookA);
		itemA.setQuantity(2);
		cartItemRepository.save(itemA);

		CartItem itemB = new CartItem();
		itemB.setCart(cart);
		itemB.setBook(bookB);
		itemB.setQuantity(1);
		cartItemRepository.save(itemB);

		// 3) 담은 순서대로 전체조회
		List<CartItem> items = cartItemRepository.findByCart_IdOrderByIdAsc(cart.getId());
		assertThat(items).hasSize(2);
		assertThat(items.get(0).getId()).isLessThan(items.get(1).getId());

		// 4) 이미 담은 도서인지 확인 (수량 합산 로직에서 사용)
		assertThat(cartItemRepository.findByCart_IdAndBook_Id(cart.getId(), bookA.getId())).isPresent();

		// 5) 선택삭제 - itemA 만 삭제
		cartItemRepository.deleteByCart_IdAndIdIn(cart.getId(), List.of(itemA.getId()));
		assertThat(cartItemRepository.findByCart_IdOrderByIdAsc(cart.getId())).hasSize(1);
		assertThat(cartItemRepository.findByCart_IdOrderByIdAsc(cart.getId()).get(0).getId()).isEqualTo(itemB.getId());

		// 6) 전체삭제(결제완료 후 장바구니 비우기 시나리오)
		cartItemRepository.deleteByCart_Id(cart.getId());
		assertThat(cartItemRepository.findByCart_IdOrderByIdAsc(cart.getId())).isEmpty();

		// 7) Cart 삭제시 남은 CartItem 도 cascade 로 함께 삭제되는지 확인
		//    (다시 담아서 cascade 삭제 검증)
		CartItem itemC = new CartItem();
		itemC.setCart(cart);
		itemC.setBook(bookB);
		itemC.setQuantity(3);
		cartItemRepository.save(itemC);

		// ★cart.getItems() 인메모리 컬렉션은 비어있는 상태(양방향 미동기화)라서, 그대로
		//   delete() 하면 Hibernate 가 자식을 못 찾고 CART 행만 지우려다 FK 제약조건에
		//   걸립니다. 삭제 직전에 다시 조회해서 실제 자식 목록을 로딩한 뒤 삭제합니다.
		Cart cartToDelete = cartRepository.findById(cart.getId()).orElseThrow();
		assertThat(cartToDelete.getItems()).hasSize(1); // cascade 삭제를 위해 자식이 실제로 로딩됐는지 확인
		cartRepository.delete(cartToDelete);
		assertThat(cartItemRepository.findById(itemC.getId())).isEmpty();
	}

	//-------------------------------------------------------------------
	// 3. Orders + OrderItem - N:1(AppUser/Book), 1:N(Orders-OrderItem), 페이징,
	//    베스트셀러 통계(PAID 주문만 집계되는지)
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ Orders/OrderItem - 주문생성, 상태변경, 페이징 조회, 베스트셀러 통계(PAID만 집계)")
	void testOrdersAndOrderItemRepository() {
		AppUser admin = createAdmin();
		AppUser buyer = createBuyer();
		Book bookA = createBook(admin, "주문테스트A_" + UUID.randomUUID(), 20000);
		Book bookB = createBook(admin, "주문테스트B_" + UUID.randomUUID(), 9000);

		// 1) 결제완료(PAID) 주문 생성 - bookA 3권 + bookB 2권
		Orders paidOrder = new Orders();
		paidOrder.setUser(buyer);
		paidOrder.setTotalAmount(20000 * 3 + 9000 * 2);
		paidOrder.setOrderStatus(OrderStatus.PENDING);
		ordersRepository.save(paidOrder);

		OrderItem paidItemA = new OrderItem();
		paidItemA.setOrder(paidOrder);
		paidItemA.setBook(bookA);
		paidItemA.setQuantity(3);
		paidItemA.setPrice(bookA.getPrice());
		paidItemA.setBookTitleSnapshot(bookA.getTitle());
		orderItemRepository.save(paidItemA);

		OrderItem paidItemB = new OrderItem();
		paidItemB.setOrder(paidOrder);
		paidItemB.setBook(bookB);
		paidItemB.setQuantity(2);
		paidItemB.setPrice(bookB.getPrice());
		paidItemB.setBookTitleSnapshot(bookB.getTitle());
		orderItemRepository.save(paidItemB);

		// 2) 카카오페이 결제승인 처리 시뮬레이션 - tid 세팅 + 상태를 PAID 로 변경
		String tid = "T" + UUID.randomUUID().toString().substring(0, 20);
		paidOrder.setTid(tid);
		paidOrder.setOrderStatus(OrderStatus.PAID);
		paidOrder.setApprovedAt(java.time.LocalDateTime.now());
		paidOrder.setKakaoResponseJson("{\"tid\":\"" + tid + "\",\"status\":\"SUCCESS_PAYMENT\"}"); // CLOB 저장 확인
		ordersRepository.save(paidOrder);

		// 3) 결제대기(PENDING) 주문도 하나 더 생성 - bookA 5권 (베스트셀러 집계에서 제외되어야 함)
		Orders pendingOrder = new Orders();
		pendingOrder.setUser(buyer);
		pendingOrder.setTotalAmount(20000 * 5);
		pendingOrder.setOrderStatus(OrderStatus.PENDING);
		ordersRepository.save(pendingOrder);

		OrderItem pendingItemA = new OrderItem();
		pendingItemA.setOrder(pendingOrder);
		pendingItemA.setBook(bookA);
		pendingItemA.setQuantity(5);
		pendingItemA.setPrice(bookA.getPrice());
		pendingItemA.setBookTitleSnapshot(bookA.getTitle());
		orderItemRepository.save(pendingItemA);

		// 4) 단건조회 + CLOB 저장확인
		Orders foundOrder = ordersRepository.findById(paidOrder.getId()).orElseThrow();
		assertThat(foundOrder.getKakaoResponseJson()).contains("SUCCESS_PAYMENT");
		assertThat(foundOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);

		// 5) tid 로 주문 찾기 (결제승인 콜백 시나리오)
		assertThat(ordersRepository.findByTid(tid)).isPresent();
		assertThat(ordersRepository.findByTid(tid).get().getId()).isEqualTo(paidOrder.getId());

		// 6) 내 주문내역 - 페이징(12개씩과 동일한 관례, 여기선 소량이라 1페이지에 다 나옴)
		var page = ordersRepository.findByUser_IdOrderByIdDesc(buyer.getId(), PageRequest.of(0, 12));
		assertThat(page.getTotalElements()).isEqualTo(2); // paidOrder + pendingOrder
		assertThat(page.getContent()).extracting(Orders::getId).contains(paidOrder.getId(), pendingOrder.getId());

		// 7) 상태별 조회
		var paidPage = ordersRepository.findByOrderStatusOrderByIdDesc(OrderStatus.PAID, PageRequest.of(0, 12));
		assertThat(paidPage.getContent()).extracting(Orders::getId).containsExactly(paidOrder.getId());

		// 8) 주문상품 조회
		assertThat(orderItemRepository.findByOrder_Id(paidOrder.getId())).hasSize(2);

		// 9) ★베스트셀러 통계 - PAID 주문만 집계되어야 함 (PENDING 인 pendingOrder 의 bookA 5권은 제외)
		List<Object[]> bestSellers = orderItemRepository.findBestSellerBookIds(10);
		Map<Long, Long> bestSellerMap = new HashMap<>();
		for (Object[] row : bestSellers) {
			Long bookId = ((Number) row[0]).longValue();
			Long totalQty = ((Number) row[1]).longValue();
			bestSellerMap.put(bookId, totalQty);
		}
		assertThat(bestSellerMap).containsEntry(bookA.getId(), 3L); // PAID 의 3권만, PENDING 의 5권은 제외
		assertThat(bestSellerMap).containsEntry(bookB.getId(), 2L);

		// 10) Orders 삭제시 OrderItem 도 cascade 로 함께 삭제되는지 확인
		//    ★paidOrder 는 new Orders() 로 만든 뒤 items 를 orderItemRepository.save() 로
		//      "자식(OrderItem) 쪽에서" 저장했을 뿐이라, paidOrder.getItems() 인메모리 컬렉션은
		//      비어있는 상태입니다(양방향 연관관계를 수동으로 채워주지 않았기 때문). 이 상태로
		//      바로 delete() 하면 Hibernate 가 "지울 자식이 없다"고 판단해 OrderItem 은 그대로
		//      둔 채 ORDERS 행만 지우려다 FK 제약조건(ORA-02292)에 걸립니다.
		//      그래서 삭제 직전에 findById() 로 다시 조회해서, DB에 실제로 있는 OrderItem
		//      목록이 컬렉션에 제대로 채워진 상태로 만든 뒤 삭제합니다.
		Long paidOrderId = paidOrder.getId();
		Orders orderToDelete = ordersRepository.findById(paidOrderId).orElseThrow();
		assertThat(orderToDelete.getItems()).hasSize(2); // cascade 삭제를 위해 자식이 실제로 로딩됐는지 확인
		ordersRepository.delete(orderToDelete);
		assertThat(orderItemRepository.findByOrder_Id(paidOrderId)).isEmpty();
	}

	//-------------------------------------------------------------------
	// 4. MyBatis 매퍼 - JPA로 저장한 데이터를 MyBatis 로도 정상적으로 읽어오는지 확인
	//-------------------------------------------------------------------
	// ★이 테스트도 @Transactional 이 필요합니다 (BookStock 저장 이유는 위와 동일).
	//   추가로, 이 테스트는 "JPA로 저장 직후 MyBatis로 바로 읽기"를 하는데, JPA 의 save()
	//   는 기본적으로 즉시 DB에 INSERT 하지 않고 커밋/플러시 시점까지 미룰 수 있습니다.
	//   MyBatis 는 (같은 트랜잭션의) 커넥션으로 직접 SQL을 실행하므로, Hibernate가 아직
	//   플러시하지 않은 변경분은 MyBatis 조회에 안 보일 수 있습니다. 그래서 save() 대신
	//   saveAndFlush() 를 사용해서 매번 즉시 DB에 반영한 뒤 MyBatis로 확인합니다.
	//-------------------------------------------------------------------
	@Test
	@Transactional
	@DisplayName("■ 결제 MyBatis 매퍼(전부 조회전용) - JPA로 저장한 데이터를 정확히 읽어오는지 확인")
	void testPaymentMyBatisMappers() {
		AppUser admin = createAdmin();
		AppUser buyer = createBuyer();
		Book book = createBook(admin, "매퍼테스트도서_" + UUID.randomUUID(), 25000);

		// --- BookStock ---
		BookStock stock = new BookStock();
		stock.setBook(book);
		stock.setStockQuantity(20);
		bookStockRepository.saveAndFlush(stock);

		BookStock mapperStock = bookStockMapper.findByBookId(book.getId());
		assertThat(mapperStock).isNotNull();
		assertThat(mapperStock.getStockQuantity()).isEqualTo(20);

		// --- Cart / CartItem ---
		Cart cart = new Cart();
		cart.setUser(buyer);
		cartRepository.saveAndFlush(cart);

		CartItem cartItem = new CartItem();
		cartItem.setCart(cart);
		cartItem.setBook(book);
		cartItem.setQuantity(4);
		cartItemRepository.saveAndFlush(cartItem);

		Cart mapperCart = cartMapper.findByUserId(buyer.getId());
		assertThat(mapperCart).isNotNull();
		assertThat(mapperCart.getId()).isEqualTo(cart.getId());

		List<CartItem> mapperCartItems = cartItemMapper.findByCartId(cart.getId());
		assertThat(mapperCartItems).hasSize(1);
		assertThat(mapperCartItems.get(0).getQuantity()).isEqualTo(4);

		// --- Orders / OrderItem ---
		Orders order = new Orders();
		order.setUser(buyer);
		order.setTotalAmount(25000 * 2);
		order.setOrderStatus(OrderStatus.PAID);
		String tid = "M" + UUID.randomUUID().toString().substring(0, 20);
		order.setTid(tid);
		ordersRepository.saveAndFlush(order);

		OrderItem orderItem = new OrderItem();
		orderItem.setOrder(order);
		orderItem.setBook(book);
		orderItem.setQuantity(2);
		orderItem.setPrice(25000);
		orderItem.setBookTitleSnapshot(book.getTitle());
		orderItemRepository.saveAndFlush(orderItem);

		Orders mapperOrder = ordersMapper.findById(order.getId());
		assertThat(mapperOrder).isNotNull();
		assertThat(mapperOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID); // Enum 자동매핑 확인

		assertThat(ordersMapper.findByTid(tid)).isNotNull();
		assertThat(ordersMapper.findByTid(tid).getId()).isEqualTo(order.getId());

		Map<String, Object> pagingMap = new HashMap<>();
		pagingMap.put("userId", buyer.getId());
		pagingMap.put("start", 0);
		pagingMap.put("end", 12);
		List<Orders> mapperOrders = ordersMapper.findByUserId(pagingMap);
		assertThat(mapperOrders).extracting(Orders::getId).contains(order.getId());
		assertThat(ordersMapper.countByUserId(buyer.getId())).isEqualTo(1);

		List<OrderItem> mapperOrderItems = orderItemMapper.findByOrderId(order.getId());
		assertThat(mapperOrderItems).hasSize(1);
		assertThat(mapperOrderItems.get(0).getBookTitleSnapshot()).isEqualTo(book.getTitle());

		// 베스트셀러 통계도 MyBatis 경로로 동일하게 집계되는지 확인
		List<Map<String, Object>> mapperBestSellers = orderItemMapper.findBestSellerBookIds(10);
		boolean found = mapperBestSellers.stream()
				.anyMatch(row -> ((Number) row.get("BOOK_ID")).longValue() == book.getId());
		assertThat(found).isTrue();
	}
}
