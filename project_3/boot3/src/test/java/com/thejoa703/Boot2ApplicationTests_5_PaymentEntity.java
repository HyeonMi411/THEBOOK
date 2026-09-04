package com.thejoa703;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
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
import com.thejoa703.mapper.BookMapper;
import com.thejoa703.mapper.OrderItemMapper;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.BookStockRepository;
import com.thejoa703.repository.CartItemRepository;
import com.thejoa703.repository.CartRepository;
import com.thejoa703.repository.OrderItemRepository;
import com.thejoa703.repository.OrdersRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 결제 기능(BookStock/Cart/CartItem/Orders/OrderItem) JPA Repository 통합테스트
 * ------------------------------------------------------------------------------
 * - 클래스 레벨 @Transactional 로 각 테스트가 끝나면 자동 롤백됨.
 * - BookStock/Cart/CartItem/Orders/OrderItem 은 단순 CRUD 라 JPA Repository 를 사용.
 * - Book 조회/등록은 검색/JOIN 이 복잡해 Mapper(BookMapper)를 그대로 사용.
 * - 베스트셀러 집계(findBestSellerBookIds)는 JOIN+GROUP BY 라 OrderItemMapper 에 남겨뒀음.
 * - JPA 의 @Version(낙관적 락)과 @Lock(비관적 락, findByIdForUpdate)이 실제로 재고차감
 *   동시성 제어에 쓰이는지 검증.
 * ------------------------------------------------------------------------------
 */
@SpringBootTest
@Transactional
class Boot2ApplicationTests_5_PaymentEntity {

	@Autowired private AppUserRepository   appUserRepository;
	@Autowired private BookMapper          bookMapper;
	@Autowired private BookStockRepository bookStockRepository;
	@Autowired private CartRepository      cartRepository;
	@Autowired private CartItemRepository  cartItemRepository;
	@Autowired private OrdersRepository    ordersRepository;
	@Autowired private OrderItemRepository orderItemRepository;
	@Autowired private OrderItemMapper     orderItemMapper;

	@PersistenceContext
	private EntityManager entityManager; // BookStock.book(@MapsId) 참조용

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
		admin.setDeleted(false);
		appUserRepository.saveAndFlush(admin);
		return admin;
	}

	private AppUser createBuyer() {
		AppUser user = new AppUser();
		user.setEmail("pay_user_" + UUID.randomUUID() + "@test.com");
		user.setPassword("encoded-pass");
		user.setNickname("pay_user_" + UUID.randomUUID().toString().substring(0, 8));
		user.setRole("ROLE_USER");
		user.setProvider("kakao"); // 소셜로그인(카카오) 구매자도 정상 동작하는지 확인
		user.setProviderId("social-" + UUID.randomUUID());
		user.setDeleted(false);
		appUserRepository.saveAndFlush(user);
		return user;
	}

	private Book createBook(AppUser admin, String title, int price) {
		Map<String, Object> params = new HashMap<>();
		params.put("title", title);
		params.put("author", "결제테스트작가");
		params.put("publisher", "결제테스트출판사");
		params.put("publishDate", LocalDate.of(2024, 1, 1));
		params.put("category", "결제테스트카테고리");
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
		return bookMapper.findById(bookId);
	}

	//-------------------------------------------------------------------
	// 1. BookStock - Book과 1:1(PK 공유), 재고 증감, 낙관적 락(@Version), 비관적 락 조회
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ BookStockRepository - 1:1(PK공유 @MapsId), 재고 증감, 낙관적락(@Version), 비관적락(findByIdForUpdate)")
	void testBookStockRepository() {
		AppUser admin = createAdmin();
		Book book = createBook(admin, "결제테스트도서_" + UUID.randomUUID(), 15000);

		// 1) 등록 - BookStock.bookId 가 Book.id 를 그대로 공유(@MapsId)하는지 확인
		//    BookStock.book 은 @MapsId 라 book 이 null 이면 ID 생성 자체가 실패하고,
		//    그렇다고 Mapper(MyBatis)로 조회한 detached Book 을 그대로 넘기면 Hibernate 가
		//    cascade persist 를 시도하다 실패. entityManager.getReference() 로 만든
		//    관리 대상 참조(프록시)를 쓰면 DB 재조회도, cascade persist 대상도 아니면서
		//    ID 는 정상적으로 넘겨줄 수 있어 두 문제를 동시에 피할 수 있음.
		BookStock stock = new BookStock();
		stock.setBook(entityManager.getReference(Book.class, book.getId()));
		stock.setStockQuantity(10);
		bookStockRepository.saveAndFlush(stock);

		assertThat(stock.getBookId()).isEqualTo(book.getId());

		// 2) 단건조회
		BookStock found = bookStockRepository.findById(book.getId()).orElseThrow();
		assertThat(found.getStockQuantity()).isEqualTo(10);
		Long versionBeforeUpdate = found.getVersion();

		// 3) 재고 차감(수정) - 낙관적 락(@Version)이 통과하고 버전이 자동으로 증가하는지 확인
		found.setStockQuantity(found.getStockQuantity() - 3);
		bookStockRepository.saveAndFlush(found);

		BookStock afterUpdate = bookStockRepository.findById(book.getId()).orElseThrow();
		assertThat(afterUpdate.getStockQuantity()).isEqualTo(7);
		assertThat(afterUpdate.getVersion()).isGreaterThan(versionBeforeUpdate);

		// 4) 비관적 락 조회 - 결제승인 시점에 재고행을 잠그고 읽는 시나리오
		BookStock locked = bookStockRepository.findByIdForUpdate(book.getId()).orElseThrow();
		assertThat(locked.getStockQuantity()).isEqualTo(7);

		// 5) Book 삭제(소프트) - 재고는 그대로 두고 DELETED 플래그만 세웁니다.
		bookMapper.updateDeleted(book.getId(), true);
		Book deletedBook = bookMapper.findById(book.getId());
		assertThat(deletedBook).isNotNull();
		assertThat(deletedBook.isDeleted()).isTrue();
		assertThat(bookStockRepository.findById(book.getId())).isPresent();
	}

	//-------------------------------------------------------------------
	// 2. Cart + CartItem - 담기/조회/선택삭제/전체삭제, Cart 삭제시 자식 먼저 삭제
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ CartRepository/CartItemRepository - 담기/조회/선택삭제/전체삭제, Cart 삭제시 CartItem 먼저 삭제")
	void testCartAndCartItemRepository() {
		AppUser admin = createAdmin();
		AppUser buyer = createBuyer();
		Book bookA = createBook(admin, "장바구니테스트A_" + UUID.randomUUID(), 12000);
		Book bookB = createBook(admin, "장바구니테스트B_" + UUID.randomUUID(), 18000);

		assertThat(cartRepository.existsByUser_Id(buyer.getId())).isFalse();

		Cart cart = new Cart();
		cart.setUser(buyer);
		cartRepository.save(cart);

		assertThat(cartRepository.existsByUser_Id(buyer.getId())).isTrue();
		assertThat(cartRepository.findByUser_Id(buyer.getId())).isPresent();

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

		List<CartItem> items = cartItemRepository.findByCart_IdOrderByIdAsc(cart.getId());
		assertThat(items).hasSize(2);
		assertThat(items.get(0).getId()).isLessThan(items.get(1).getId());

		assertThat(cartItemRepository.findByCart_IdAndBook_Id(cart.getId(), bookA.getId())).isPresent();

		cartItemRepository.deleteById(itemA.getId());
		List<CartItem> afterSelectDelete = cartItemRepository.findByCart_IdOrderByIdAsc(cart.getId());
		assertThat(afterSelectDelete).hasSize(1);
		assertThat(afterSelectDelete.get(0).getId()).isEqualTo(itemB.getId());

		cartItemRepository.deleteByCart_Id(cart.getId());
		assertThat(cartItemRepository.findByCart_IdOrderByIdAsc(cart.getId())).isEmpty();

		CartItem itemC = new CartItem();
		itemC.setCart(cart);
		itemC.setBook(bookB);
		itemC.setQuantity(3);
		cartItemRepository.save(itemC);

		cartItemRepository.deleteByCart_Id(cart.getId());
		cartRepository.deleteById(cart.getId());
		assertThat(cartItemRepository.findById(itemC.getId())).isEmpty();
		assertThat(cartRepository.findByUser_Id(buyer.getId())).isEmpty();
	}

	//-------------------------------------------------------------------
	// 3. Orders + OrderItem - 주문생성, 상태변경, 페이징 조회, 베스트셀러 통계(PAID만 집계), 삭제시 자식 먼저 삭제
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ OrdersRepository/OrderItemRepository - 주문생성, 상태변경, 페이징, 베스트셀러 통계(Mapper)")
	void testOrdersAndOrderItemRepository() {
		AppUser admin = createAdmin();
		AppUser buyer = createBuyer();
		Book bookA = createBook(admin, "주문테스트A_" + UUID.randomUUID(), 20000);
		Book bookB = createBook(admin, "주문테스트B_" + UUID.randomUUID(), 9000);

		Orders paidOrder = new Orders();
		paidOrder.setUser(buyer);
		paidOrder.setTotalAmount(20000 * 3 + 9000 * 2);
		paidOrder.setOrderStatus(OrderStatus.PENDING);
		paidOrder.setHiddenByUser(false);
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

		String tid = "T" + UUID.randomUUID().toString().substring(0, 20);
		paidOrder.setTid(tid);
		paidOrder.setOrderStatus(OrderStatus.PAID);
		paidOrder.setApprovedAt(java.time.LocalDateTime.now());
		paidOrder.setKakaoResponseJson("{\"tid\":\"" + tid + "\",\"status\":\"SUCCESS_PAYMENT\"}");
		ordersRepository.saveAndFlush(paidOrder);

		Orders pendingOrder = new Orders();
		pendingOrder.setUser(buyer);
		pendingOrder.setTotalAmount(20000 * 5);
		pendingOrder.setOrderStatus(OrderStatus.PENDING);
		pendingOrder.setHiddenByUser(false);
		ordersRepository.save(pendingOrder);

		OrderItem pendingItemA = new OrderItem();
		pendingItemA.setOrder(pendingOrder);
		pendingItemA.setBook(bookA);
		pendingItemA.setQuantity(5);
		pendingItemA.setPrice(bookA.getPrice());
		pendingItemA.setBookTitleSnapshot(bookA.getTitle());
		orderItemRepository.saveAndFlush(pendingItemA);

		Orders foundOrder = ordersRepository.findById(paidOrder.getId()).orElseThrow();
		assertThat(foundOrder.getKakaoResponseJson()).contains("SUCCESS_PAYMENT");
		assertThat(foundOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);

		assertThat(ordersRepository.findByTid(tid)).isPresent();
		assertThat(ordersRepository.findByTid(tid).get().getId()).isEqualTo(paidOrder.getId());

		Page<Orders> myOrders = ordersRepository.findByUser_IdAndHiddenByUserFalseOrderByIdDesc(
				buyer.getId(), PageRequest.of(0, 12));
		assertThat(myOrders.getContent()).extracting(Orders::getId).contains(paidOrder.getId(), pendingOrder.getId());
		assertThat(myOrders.getTotalElements()).isEqualTo(2);

		assertThat(orderItemRepository.findByOrder_Id(paidOrder.getId())).hasSize(2);

		// 베스트셀러 통계(Mapper, JOIN+GROUP BY) - PAID 주문만 집계 (PENDING 의 bookA 5권은 제외)
		List<Map<String, Object>> bestSellers = orderItemMapper.findBestSellerBookIds(10);
		Map<Long, Long> bestSellerMap = new HashMap<>();
		for (Map<String, Object> row : bestSellers) {
			Long bookId = ((Number) row.get("BOOK_ID")).longValue();
			Long totalQty = ((Number) row.get("TOTAL_QTY")).longValue();
			bestSellerMap.put(bookId, totalQty);
		}
		assertThat(bestSellerMap).containsEntry(bookA.getId(), 3L);
		assertThat(bestSellerMap).containsEntry(bookB.getId(), 2L);

		// Orders 삭제시 ORDER_ITEMS 를 먼저 지워야 FK 제약 위반이 안 남
		Long paidOrderId = paidOrder.getId();
		orderItemRepository.deleteByOrder_Id(paidOrderId);
		ordersRepository.deleteById(paidOrderId);
		assertThat(orderItemRepository.findByOrder_Id(paidOrderId)).isEmpty();
		assertThat(ordersRepository.findById(paidOrderId)).isEmpty();
	}
}
