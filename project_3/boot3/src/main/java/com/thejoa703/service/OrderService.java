package com.thejoa703.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.dto.OrderDto.OrderCreateRequestDto;
import com.thejoa703.dto.OrderDto.OrderResponseDto;
import com.thejoa703.dto.PageResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Book;
import com.thejoa703.entity.CartItem;
import com.thejoa703.entity.OrderItem;
import com.thejoa703.entity.OrderStatus;
import com.thejoa703.entity.Orders;
import com.thejoa703.exception.ResourceNotFoundException;
import com.thejoa703.mapper.AppUserMapper;
import com.thejoa703.mapper.BookMapper;
import com.thejoa703.mapper.CartItemMapper;
import com.thejoa703.mapper.OrderItemMapper;
import com.thejoa703.mapper.OrdersMapper;

import lombok.RequiredArgsConstructor;

/**
 * 주문 서비스 - 로그인한 사용자라면 누구나 이용 가능 (관리자 전용 아님)
 * - 장바구니 결제(cartItemIds) 또는 바로구매(bookId+quantity) 두 방식을 모두 지원합니다.
 * - 주문 생성 시점에는 재고를 "확인만" 하고 차감하지는 않습니다. 실제 차감은 결제 승인
 *   완료 시점(PaymentService.approve)에 이루어집니다.
 * - 결제전(PENDING) 주문 삭제는 DB에서 실제 삭제, 결제완료/취소/실패는 hiddenByUser
 *   플래그로 "숨기기"만 처리해서 회계·이력 기록을 보존합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

	private static final int DEFAULT_PAGE_SIZE = 12;

	private final OrdersMapper    ordersMapper;
	private final OrderItemMapper orderItemMapper;
	private final CartItemMapper  cartItemMapper;
	private final BookMapper      bookMapper;
	private final AppUserMapper   appUserMapper;

	private record OrderLine(Book book, int quantity) {}

	// MyBatis 는 JPA 의 @OneToMany 자동 로딩이 없으므로, Orders 를 조회할 때마다
	// OrderItem 목록을 직접 채워서 완전한 객체로 만들어줍니다.
	private Orders loadOrderWithItems(Long orderId) {
		Orders order = ordersMapper.findById(orderId);
		if (order == null) { return null; }
		order.setItems(orderItemMapper.findByOrderId(orderId));
		return order;
	}

	@Transactional
	public OrderResponseDto createOrder(Long userId, OrderCreateRequestDto dto) {
		AppUser user = appUserMapper.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다. ID : " + userId));

		// 1단계 : 검증 + 계산만 먼저 (Orders 는 아직 만들지 않음)
		List<OrderLine> lines = new ArrayList<>();
		List<CartItem> usedCartItems = new ArrayList<>();
		int totalAmount = 0;

		if (dto.getCartItemIds() != null && !dto.getCartItemIds().isEmpty()) {
			for (Long cartItemId : dto.getCartItemIds()) {
				CartItem cartItem = cartItemMapper.findById(cartItemId);
				if (cartItem == null) {
					throw new ResourceNotFoundException("장바구니 항목이 없습니다. ID : " + cartItemId);
				}
				if (!cartItem.getCart().getUser().getId().equals(userId)) {
					throw new IllegalStateException("본인의 장바구니 항목만 주문할 수 있습니다.");
				}
				Book book = cartItem.getBook();
				checkPurchasable(book, cartItem.getQuantity());

				lines.add(new OrderLine(book, cartItem.getQuantity()));
				totalAmount += book.getPrice() * cartItem.getQuantity();
				usedCartItems.add(cartItem);
			}

		} else if (dto.getBookId() != null) {
			Book book = bookMapper.findById(dto.getBookId());
			if (book == null) {
				throw new ResourceNotFoundException("존재하지 않는 도서입니다. ID : " + dto.getBookId());
			}
			int quantity = (dto.getQuantity() != null) ? dto.getQuantity() : 1;
			checkPurchasable(book, quantity);

			lines.add(new OrderLine(book, quantity));
			totalAmount += book.getPrice() * quantity;

		} else {
			throw new IllegalArgumentException("장바구니 항목(cartItemIds) 또는 바로구매 도서(bookId)를 지정해야 합니다.");
		}

		// 2단계 : 검증을 전부 통과한 경우에만 Orders + OrderItem 저장
		Orders order = new Orders();
		order.setUser(user);
		order.setOrderStatus(OrderStatus.PENDING);
		order.setTotalAmount(totalAmount);
		order.setHiddenByUser(false);
		ordersMapper.insert(order);

		for (OrderLine line : lines) {
			addOrderItem(order, line.book(), line.quantity());
		}

		for (CartItem used : usedCartItems) {
			cartItemMapper.deleteById(used.getId());
		}

		return OrderResponseDto.from(loadOrderWithItems(order.getId()));
	}

	// 재고 + 가격 검증 - 가격이 비어있는 도서는 여기서 즉시 거부합니다 (0원 주문 방지)
	private void checkPurchasable(Book book, int quantity) {
		if (book.isDeleted()) {
			throw new IllegalStateException("[" + book.getTitle() + "] 판매가 중단된 도서라 구매할 수 없습니다.");
		}
		if (book.getPrice() == null || book.getPrice() <= 0) {
			throw new IllegalStateException(
					"[" + book.getTitle() + "] 이 도서는 가격이 등록되지 않아 구매할 수 없습니다. 관리자에게 문의해주세요."
			);
		}
		int stockQuantity = (book.getStock() != null) ? book.getStock().getStockQuantity() : 0;
		if (quantity > stockQuantity) {
			throw new IllegalStateException("[" + book.getTitle() + "] 재고가 부족합니다. (현재 재고 : " + stockQuantity + "권)");
		}
	}

	private void addOrderItem(Orders order, Book book, int quantity) {
		OrderItem orderItem = new OrderItem();
		orderItem.setOrder(order);
		orderItem.setBook(book);
		orderItem.setQuantity(quantity);
		orderItem.setPrice(book.getPrice());
		orderItem.setBookTitleSnapshot(book.getTitle());
		orderItemMapper.insert(orderItem);
	}

	public PageResponseDto<OrderResponseDto> getMyOrders(Long userId, int page, int size) {
		int currentPage = Math.max(page, 1);
		int pageSize = (size > 0) ? size : DEFAULT_PAGE_SIZE;

		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		params.put("start", (currentPage - 1) * pageSize);
		params.put("end", pageSize);

		List<Orders> orders = ordersMapper.findByUserId(params);
		orders.forEach(o -> o.setItems(orderItemMapper.findByOrderId(o.getId())));

		int totalElements = ordersMapper.countByUserId(userId);
		int totalPages = (int) Math.ceil((double) totalElements / pageSize);

		List<OrderResponseDto> content = orders.stream()
				.map(OrderResponseDto::from)
				.collect(Collectors.toList());

		return new PageResponseDto<>(content, currentPage, pageSize, totalElements, totalPages);
	}

	public OrderResponseDto getOrder(Long userId, Long orderId) {
		Orders order = loadOrderWithItems(orderId);
		if (order == null) {
			throw new ResourceNotFoundException("존재하지 않는 주문입니다. ID : " + orderId);
		}
		if (!order.getUser().getId().equals(userId)) {
			throw new IllegalStateException("본인의 주문만 조회할 수 있습니다.");
		}
		return OrderResponseDto.from(order);
	}

	@Transactional
	public void deleteOrder(Long userId, Long orderId) {
		Orders order = ordersMapper.findById(orderId);
		if (order == null) {
			throw new ResourceNotFoundException("존재하지 않는 주문입니다. ID : " + orderId);
		}
		if (!order.getUser().getId().equals(userId)) {
			throw new IllegalStateException("본인의 주문만 삭제할 수 있습니다.");
		}

		if (order.getOrderStatus() == OrderStatus.PENDING) {
			// FK 제약(ORDER_ITEMS.ORDER_ID → ORDERS.ID) 때문에, 자식(OrderItem)을
			// 먼저 지운 뒤 부모(Orders)를 삭제해야 합니다.
			orderItemMapper.deleteByOrderId(orderId);
			ordersMapper.deleteById(orderId);
		} else {
			order.setHiddenByUser(true);
			ordersMapper.update(order);
		}
	}
}
