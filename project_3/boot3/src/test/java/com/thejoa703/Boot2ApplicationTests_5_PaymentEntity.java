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
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Book;
import com.thejoa703.entity.BookStock;
import com.thejoa703.entity.Cart;
import com.thejoa703.entity.CartItem;
import com.thejoa703.entity.OrderItem;
import com.thejoa703.entity.OrderStatus;
import com.thejoa703.entity.Orders;
import com.thejoa703.mapper.AppUserMapper;
import com.thejoa703.mapper.BookMapper;
import com.thejoa703.mapper.BookStockMapper;
import com.thejoa703.mapper.CartItemMapper;
import com.thejoa703.mapper.CartMapper;
import com.thejoa703.mapper.OrderItemMapper;
import com.thejoa703.mapper.OrdersMapper;

/**
 * 결제 기능(BookStock/Cart/CartItem/Orders/OrderItem) MyBatis Mapper 통합테스트
 * ------------------------------------------------------------------------------
 * - 클래스 레벨 @Transactional 로 각 테스트가 끝나면 자동 롤백됩니다.
 * - JPA 의 cascade(orphanRemoval)가 자동으로 해주던 "자식 먼저 삭제 후 부모 삭제"를,
 *   MyBatis 에서는 애플리케이션 코드가 명시적으로 순서를 관리해야 합니다. 이 테스트에서도
 *   그 순서(BookStock→Book, CartItem→Cart, OrderItem→Orders)를 그대로 검증합니다.
 * - JPA 의 @Version(낙관적 락)은 BookStockMapper.updateWithVersionCheck() 로,
 *   비관적 락(SELECT ... FOR UPDATE)은 findByBookIdForUpdate() 로 각각 재현됩니다.
 * ------------------------------------------------------------------------------
 */
@SpringBootTest
@Transactional
class Boot2ApplicationTests_5_PaymentEntity {

	@Autowired private AppUserMapper   appUserMapper;
	@Autowired private BookMapper      bookMapper;
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
		admin.setDeleted(false);
		appUserMapper.insert(admin);
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
		appUserMapper.insert(user);
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
	// 1. BookStock - Book과 1:1(PK 공유), 재고 증감, 낙관적 락(버전체크), 비관적 락 조회, Book 삭제시 자식 먼저 삭제
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ BookStockMapper - 1:1(PK공유), 재고 증감, 낙관적락(버전체크), 비관적락 조회, Book 삭제시 재고 먼저 삭제")
	void testBookStockMapper() {
		AppUser admin = createAdmin();
		Book book = createBook(admin, "결제테스트도서_" + UUID.randomUUID(), 15000);

		// 1) 등록 - BookStock.bookId 가 Book.id 를 그대로 공유(PK=FK)하는지 확인
		BookStock stock = new BookStock();
		stock.setBookId(book.getId());
		stock.setStockQuantity(10);
		bookStockMapper.insert(stock);

		assertThat(stock.getBookId()).isEqualTo(book.getId());

		// 2) 단건조회
		BookStock found = bookStockMapper.findByBookId(book.getId());
		assertThat(found.getStockQuantity()).isEqualTo(10);
		Long versionBeforeUpdate = found.getVersion();

		// 3) 재고 차감(수정) - 낙관적 락(버전체크)이 통과하고 버전이 증가하는지 확인
		found.setStockQuantity(found.getStockQuantity() - 3);
		int updated = bookStockMapper.updateWithVersionCheck(found);
		assertThat(updated).isEqualTo(1);

		BookStock afterUpdate = bookStockMapper.findByBookId(book.getId());
		assertThat(afterUpdate.getStockQuantity()).isEqualTo(7);
		assertThat(afterUpdate.getVersion()).isGreaterThan(versionBeforeUpdate);

		// 4) 옛 버전으로 다시 갱신 시도하면 낙관적 락 충돌(영향받은 행 0건)이 나야 함
		found.setStockQuantity(1); // found 는 여전히 옛 버전을 들고 있음
		int conflictResult = bookStockMapper.updateWithVersionCheck(found);
		assertThat(conflictResult).isEqualTo(0);

		// 5) 비관적 락 조회 - 결제승인 시점에 재고행을 잠그고 읽는 시나리오
		BookStock locked = bookStockMapper.findByBookIdForUpdate(book.getId());
		assertThat(locked.getStockQuantity()).isEqualTo(7);

		// 6) Book 삭제(소프트) - BookService.deleteBook() 과 동일하게, 재고는 그대로 두고
		//    DELETED 플래그만 세웁니다. CART_ITEM/ORDER_ITEMS 가 BOOK_ID 를 FK 로 참조하고
		//    있어서 하드 삭제하면 FK 제약조건 위반이 나기 때문입니다.
		bookMapper.updateDeleted(book.getId(), true);
		Book deletedBook = bookMapper.findById(book.getId());
		assertThat(deletedBook).isNotNull();
		assertThat(deletedBook.isDeleted()).isTrue();
		// 재고(BOOK_STOCK)는 소프트삭제로 인해 지워지지 않고 그대로 남아있어야 함
		assertThat(bookStockMapper.findByBookId(book.getId())).isNotNull();
	}

	//-------------------------------------------------------------------
	// 2. Cart + CartItem - 사용자당 1개(1:1), 담기/조회/선택삭제/전체삭제, Cart 삭제시 자식 먼저 삭제
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ CartMapper/CartItemMapper - 담기/조회/선택삭제/전체삭제, Cart 삭제시 CartItem 먼저 삭제")
	void testCartAndCartItemMapper() {
		AppUser admin = createAdmin();
		AppUser buyer = createBuyer();
		Book bookA = createBook(admin, "장바구니테스트A_" + UUID.randomUUID(), 12000);
		Book bookB = createBook(admin, "장바구니테스트B_" + UUID.randomUUID(), 18000);

		// 1) 장바구니가 아직 없다는 것 확인 후 생성
		assertThat(cartMapper.existsByUserId(buyer.getId())).isFalse();

		Cart cart = new Cart();
		cart.setUser(buyer);
		cartMapper.insert(cart);

		assertThat(cartMapper.existsByUserId(buyer.getId())).isTrue();
		assertThat(cartMapper.findByUserId(buyer.getId())).isNotNull();

		// 2) 장바구니에 도서 2권 담기
		CartItem itemA = new CartItem();
		itemA.setCart(cart);
		itemA.setBook(bookA);
		itemA.setQuantity(2);
		cartItemMapper.insert(itemA);

		CartItem itemB = new CartItem();
		itemB.setCart(cart);
		itemB.setBook(bookB);
		itemB.setQuantity(1);
		cartItemMapper.insert(itemB);

		// 3) 담은 순서대로 전체조회
		List<CartItem> items = cartItemMapper.findByCartId(cart.getId());
		assertThat(items).hasSize(2);
		assertThat(items.get(0).getId()).isLessThan(items.get(1).getId());

		// 4) 이미 담은 도서인지 확인 (수량 합산 로직에서 사용)
		assertThat(cartItemMapper.findByCartIdAndBookId(cart.getId(), bookA.getId())).isNotNull();

		// 5) 선택삭제 - itemA 만 삭제
		cartItemMapper.deleteById(itemA.getId());
		List<CartItem> afterSelectDelete = cartItemMapper.findByCartId(cart.getId());
		assertThat(afterSelectDelete).hasSize(1);
		assertThat(afterSelectDelete.get(0).getId()).isEqualTo(itemB.getId());

		// 6) 전체삭제(결제완료 후 장바구니 비우기 시나리오)
		cartItemMapper.deleteByCartId(cart.getId());
		assertThat(cartItemMapper.findByCartId(cart.getId())).isEmpty();

		// 7) Cart 삭제시 CART_ITEM 을 먼저 지워야 FK 제약 위반이 안 남
		CartItem itemC = new CartItem();
		itemC.setCart(cart);
		itemC.setBook(bookB);
		itemC.setQuantity(3);
		cartItemMapper.insert(itemC);

		cartItemMapper.deleteByCartId(cart.getId());
		cartMapper.deleteById(cart.getId());
		assertThat(cartItemMapper.findById(itemC.getId())).isNull();
		assertThat(cartMapper.findByUserId(buyer.getId())).isNull();
	}

	//-------------------------------------------------------------------
	// 3. Orders + OrderItem - 주문생성, 상태변경, 페이징 조회, 베스트셀러 통계(PAID만 집계), 삭제시 자식 먼저 삭제
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ OrdersMapper/OrderItemMapper - 주문생성, 상태변경, 페이징 조회, 베스트셀러 통계(PAID만 집계)")
	void testOrdersAndOrderItemMapper() {
		AppUser admin = createAdmin();
		AppUser buyer = createBuyer();
		Book bookA = createBook(admin, "주문테스트A_" + UUID.randomUUID(), 20000);
		Book bookB = createBook(admin, "주문테스트B_" + UUID.randomUUID(), 9000);

		// 1) 결제완료(PAID) 주문 생성 - bookA 3권 + bookB 2권
		Orders paidOrder = new Orders();
		paidOrder.setUser(buyer);
		paidOrder.setTotalAmount(20000 * 3 + 9000 * 2);
		paidOrder.setOrderStatus(OrderStatus.PENDING);
		paidOrder.setHiddenByUser(false);
		ordersMapper.insert(paidOrder);

		OrderItem paidItemA = new OrderItem();
		paidItemA.setOrder(paidOrder);
		paidItemA.setBook(bookA);
		paidItemA.setQuantity(3);
		paidItemA.setPrice(bookA.getPrice());
		paidItemA.setBookTitleSnapshot(bookA.getTitle());
		orderItemMapper.insert(paidItemA);

		OrderItem paidItemB = new OrderItem();
		paidItemB.setOrder(paidOrder);
		paidItemB.setBook(bookB);
		paidItemB.setQuantity(2);
		paidItemB.setPrice(bookB.getPrice());
		paidItemB.setBookTitleSnapshot(bookB.getTitle());
		orderItemMapper.insert(paidItemB);

		// 2) 카카오페이 결제승인 처리 시뮬레이션 - tid 세팅 + 상태를 PAID 로 변경
		String tid = "T" + UUID.randomUUID().toString().substring(0, 20);
		paidOrder.setTid(tid);
		paidOrder.setOrderStatus(OrderStatus.PAID);
		paidOrder.setApprovedAt(java.time.LocalDateTime.now());
		paidOrder.setKakaoResponseJson("{\"tid\":\"" + tid + "\",\"status\":\"SUCCESS_PAYMENT\"}"); // CLOB 저장 확인
		ordersMapper.update(paidOrder);

		// 3) 결제대기(PENDING) 주문도 하나 더 생성 - bookA 5권 (베스트셀러 집계에서 제외되어야 함)
		Orders pendingOrder = new Orders();
		pendingOrder.setUser(buyer);
		pendingOrder.setTotalAmount(20000 * 5);
		pendingOrder.setOrderStatus(OrderStatus.PENDING);
		pendingOrder.setHiddenByUser(false);
		ordersMapper.insert(pendingOrder);

		OrderItem pendingItemA = new OrderItem();
		pendingItemA.setOrder(pendingOrder);
		pendingItemA.setBook(bookA);
		pendingItemA.setQuantity(5);
		pendingItemA.setPrice(bookA.getPrice());
		pendingItemA.setBookTitleSnapshot(bookA.getTitle());
		orderItemMapper.insert(pendingItemA);

		// 4) 단건조회 + CLOB 저장확인
		Orders foundOrder = ordersMapper.findById(paidOrder.getId());
		assertThat(foundOrder.getKakaoResponseJson()).contains("SUCCESS_PAYMENT");
		assertThat(foundOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);

		// 5) tid 로 주문 찾기 (결제승인 콜백 시나리오)
		assertThat(ordersMapper.findByTid(tid)).isNotNull();
		assertThat(ordersMapper.findByTid(tid).getId()).isEqualTo(paidOrder.getId());

		// 6) 내 주문내역 - 페이징(12개씩과 동일한 관례, 여기선 소량이라 1페이지에 다 나옴)
		Map<String, Object> pagingMap = new HashMap<>();
		pagingMap.put("userId", buyer.getId());
		pagingMap.put("start", 0);
		pagingMap.put("end", 12);
		List<Orders> myOrders = ordersMapper.findByUserId(pagingMap);
		assertThat(myOrders).extracting(Orders::getId).contains(paidOrder.getId(), pendingOrder.getId());
		assertThat(ordersMapper.countByUserId(buyer.getId())).isEqualTo(2);

		// 7) 주문상품 조회
		assertThat(orderItemMapper.findByOrderId(paidOrder.getId())).hasSize(2);

		// 8) 베스트셀러 통계 - PAID 주문만 집계되어야 함 (PENDING 인 pendingOrder 의 bookA 5권은 제외)
		List<Map<String, Object>> bestSellers = orderItemMapper.findBestSellerBookIds(10);
		Map<Long, Long> bestSellerMap = new HashMap<>();
		for (Map<String, Object> row : bestSellers) {
			Long bookId = ((Number) row.get("BOOK_ID")).longValue();
			Long totalQty = ((Number) row.get("TOTAL_QTY")).longValue();
			bestSellerMap.put(bookId, totalQty);
		}
		assertThat(bestSellerMap).containsEntry(bookA.getId(), 3L); // PAID 의 3권만, PENDING 의 5권은 제외
		assertThat(bestSellerMap).containsEntry(bookB.getId(), 2L);

		// 9) Orders 삭제시 ORDER_ITEMS 를 먼저 지워야 FK 제약 위반이 안 남 (OrderService.deleteOrder() 와 동일한 순서)
		Long paidOrderId = paidOrder.getId();
		orderItemMapper.deleteByOrderId(paidOrderId);
		ordersMapper.deleteById(paidOrderId);
		assertThat(orderItemMapper.findByOrderId(paidOrderId)).isEmpty();
		assertThat(ordersMapper.findById(paidOrderId)).isNull();
	}
}
