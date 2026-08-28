package com.thejoa703.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.BookRepository;
import com.thejoa703.repository.CartItemRepository;
import com.thejoa703.repository.OrderItemRepository;
import com.thejoa703.repository.OrdersRepository;

import lombok.RequiredArgsConstructor;

/**
 * 주문 서비스 - 로그인한 사용자라면 누구나 이용 가능 (관리자 전용 아님)
 * - 장바구니 결제(cartItemIds) 또는 바로구매(bookId+quantity) 두 방식을 모두 지원합니다.
 * - 주문 생성 시점에는 재고를 "확인만" 하고 차감하지는 않습니다. 실제 차감은 결제 승인
 *   완료 시점(PaymentService.approve)에 이루어집니다.
 * - ★검증(재고체크/요청유효성)을 전부 먼저 끝낸 뒤에만 Orders 를 생성/저장합니다. 검증
 *   순서를 뒤집어서 "일단 Orders 부터 저장하고 나중에 검증"하면, 검증에 실패했을 때도
 *   이미 저장 예약된(pending) Orders 가 트랜잭션 경계에 따라 남아버릴 수 있습니다
 *   (특히 상위 트랜잭션에 참여(REQUIRED)하는 구조에서, 실패 시점에 즉시 롤백되지 않고
 *   트랜잭션이 끝날 때까지 지연되는 경우). 검증을 먼저 다 마치고 나서 저장하면 이런
 *   문제 자체가 생기지 않습니다.
 * - ★주문 삭제(deleteOrder) : 결제전(PENDING)은 실제 DB 삭제, 결제완료/취소/실패는
 *   hiddenByUser 플래그로 "숨기기"만 처리해서 회계·이력 기록을 보존합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

	private static final int DEFAULT_PAGE_SIZE = 12;

	private final OrdersRepository    ordersRepository;
	private final OrderItemRepository orderItemRepository;
	private final CartItemRepository  cartItemRepository;
	private final BookRepository      bookRepository;
	private final AppUserRepository   appUserRepository;

	// 주문 생성 직전까지 검증/계산한 "한 줄(도서/수량)" 정보를 임시로 담아두는 내부 레코드
	private record OrderLine(Book book, int quantity) {}

	// 1. 주문 생성 ( ★장바구니 결제 or 바로구매 )
	@Transactional
	public OrderResponseDto createOrder(Long userId, OrderCreateRequestDto dto) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다. ID : " + userId));

		// ---------------------------------------------------------------
		// 1단계 : 검증 + 계산만 먼저 (Orders 는 아직 만들지 않음)
		// ---------------------------------------------------------------
		List<OrderLine> lines = new ArrayList<>();
		List<CartItem> usedCartItems = new ArrayList<>();
		int totalAmount = 0;

		if (dto.getCartItemIds() != null && !dto.getCartItemIds().isEmpty()) {
			// ---- 장바구니에서 선택한 항목으로 주문 ----
			for (Long cartItemId : dto.getCartItemIds()) {
				CartItem cartItem = cartItemRepository.findById(cartItemId)
						.orElseThrow(() -> new ResourceNotFoundException("장바구니 항목이 없습니다. ID : " + cartItemId));
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
			// ---- 바로구매 ----
			Book book = bookRepository.findById(dto.getBookId())
					.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 도서입니다. ID : " + dto.getBookId()));
			int quantity = (dto.getQuantity() != null) ? dto.getQuantity() : 1;
			checkPurchasable(book, quantity);

			lines.add(new OrderLine(book, quantity));
			totalAmount += book.getPrice() * quantity;

		} else {
			throw new IllegalArgumentException("장바구니 항목(cartItemIds) 또는 바로구매 도서(bookId)를 지정해야 합니다.");
		}

		// ---------------------------------------------------------------
		// 2단계 : 검증을 전부 통과한 경우에만 Orders + OrderItem 저장
		// ---------------------------------------------------------------
		Orders order = new Orders();
		order.setUser(user);
		order.setOrderStatus(OrderStatus.PENDING);
		order.setTotalAmount(totalAmount); // ★이제는 이미 계산이 끝난 실제 값으로 바로 저장
		ordersRepository.save(order);

		for (OrderLine line : lines) {
			addOrderItem(order, line.book(), line.quantity());
		}

		// 주문에 사용한 장바구니 항목은 제거
		// ★Cart.items(부모의 메모리상 컬렉션)는 아예 건드리지 않고, CartItemRepository 로만
		//   개별 삭제합니다. (orphanRemoval 컬렉션 조작과 repository 삭제를 같이 쓰면
		//   같은 행을 두 번 지우려다 충돌하고, 컬렉션 조작만 믿고 repository 삭제를 안 하면
		//   타이밍에 따라 실제로 삭제가 안 되는 경우가 있어, 가장 단순하고 확실한 개별
		//   repository.delete() 방식 하나로 통일했습니다. CartService 도 동일한 원칙입니다.)
		for (CartItem used : usedCartItems) {
			cartItemRepository.delete(used);
		}

		return OrderResponseDto.from(order);
	}

	// ★재고 + 가격 검증을 함께 처리 (기존 이름 checkStock 에서 checkPurchasable 로 변경)
	//   - 재고 부족 시 거부 (기존과 동일)
	//   - ★가격(price)이 비어있는 도서는 여기서 즉시 거부합니다. 예전에는 가격이 null이면
	//     조용히 0원으로 처리해서, 겉보기엔 정상적으로 PENDING 주문이 만들어졌다가 카카오페이
	//     결제준비(ready) 단계에서 total_amount=0 을 이상하게 여긴 카카오 서버가
	//     "error_code: -1 internal server error!" 라는, 원인을 전혀 알 수 없는 응답을
	//     돌려주는 문제가 있었습니다. 문제를 훨씬 이해하기 쉬운 시점(주문생성)에서,
	//     훨씬 명확한 한국어 메시지로 미리 막습니다.
	private void checkPurchasable(Book book, int quantity) {
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
		orderItem.setPrice(book.getPrice());               // ★주문 시점 가격 스냅샷
		orderItem.setBookTitleSnapshot(book.getTitle());   // ★주문 시점 도서명 스냅샷
		orderItemRepository.save(orderItem);
		order.getItems().add(orderItem); // ★양방향 동기화
	}

	// 2. 내 주문내역 조회 - 12개씩 페이징
	public PageResponseDto<OrderResponseDto> getMyOrders(Long userId, int page, int size) {
		int currentPage = Math.max(page, 1);
		int pageSize = (size > 0) ? size : DEFAULT_PAGE_SIZE;
		Pageable pageable = PageRequest.of(currentPage - 1, pageSize);

		Page<Orders> result = ordersRepository.findByUser_IdAndHiddenByUserFalseOrderByIdDesc(userId, pageable);
		List<OrderResponseDto> content = result.getContent().stream()
				.map(OrderResponseDto::from)
				.collect(Collectors.toList());

		return new PageResponseDto<>(content, currentPage, pageSize, result.getTotalElements(), result.getTotalPages());
	}

	// 3. 주문 상세 조회 (본인 주문만)
	public OrderResponseDto getOrder(Long userId, Long orderId) {
		Orders order = ordersRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 주문입니다. ID : " + orderId));
		if (!order.getUser().getId().equals(userId)) {
			throw new IllegalStateException("본인의 주문만 조회할 수 있습니다.");
		}
		return OrderResponseDto.from(order);
	}

	// 4. 주문 삭제 (본인 주문만, ★결제전(PENDING) 주문만 삭제 가능)
	//    ★결제완료(PAID)/취소(CANCELLED)/실패(FAILED) 주문은 회계·이력 보존을 위해
	//    삭제를 허용하지 않습니다(실제 결제/재고차감이 일어났던 기록이라 지우면 안 됨).
	//    "장바구니에 담았다가 결제까지 안 가고 방치된" PENDING 주문만 정리 목적으로
	//    지울 수 있게 합니다.
	@Transactional
	// 4. 주문 삭제 (본인 주문만)
	//    ★결제전(PENDING) : 실제 거래 기록이 없으므로 DB에서 진짜로 삭제합니다.
	//    ★결제완료/취소/실패 : 실제 결제·재고차감이 있었던 기록이라 DB에서 지우지 않고,
	//      hiddenByUser 플래그만 true 로 바꿔서 "내 주문내역 목록"에서만 안 보이게 합니다.
	//      (회계·이력 보존 목적 - 관리자/DB 상에는 그대로 남아있습니다)
	@Transactional
	public void deleteOrder(Long userId, Long orderId) {
		Orders order = ordersRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 주문입니다. ID : " + orderId));
		if (!order.getUser().getId().equals(userId)) {
			throw new IllegalStateException("본인의 주문만 삭제할 수 있습니다.");
		}

		if (order.getOrderStatus() == OrderStatus.PENDING) {
			// ★자식(OrderItem)을 먼저 개별 삭제한 뒤 부모(Orders)를 삭제합니다.
			//   (Cart/CartItem 에서 겪었던 것과 동일한 이유로, 컬렉션(orphanRemoval)에
			//   의존하지 않고 repository.delete() 로만 확실하게 처리합니다)
			orderItemRepository.deleteAll(order.getItems());
			ordersRepository.delete(order);
		} else {
			order.setHiddenByUser(true);
			ordersRepository.save(order);
		}
	}
}
