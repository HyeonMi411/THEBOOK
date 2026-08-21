package com.thejoa703.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.thejoa703.entity.OrderItem;
import com.thejoa703.entity.OrderStatus;
import com.thejoa703.entity.Orders;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class OrderDto {

	// 주문 생성 요청 Dto
	// - cartItemIds 를 넘기면 "장바구니에서 선택한 항목으로 주문"
	// - bookId + quantity 를 넘기면 "바로구매"
	// - 즉 둘 중 하나만 채워서 요청합니다.
	@Getter @Setter @AllArgsConstructor @NoArgsConstructor
	public static class OrderCreateRequestDto {
		private List<Long> cartItemIds; // 장바구니 결제용 (선택한 CartItem id 목록)

		private Long bookId;            // 바로구매용
		@Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
		private Integer quantity;       // 바로구매용
	}

	// 주문상품 응답 Dto
	@Getter @Setter @AllArgsConstructor @NoArgsConstructor
	public static class OrderItemResponseDto {
		private Long id;
		private Long bookId;
		private String bookTitle;   // 주문 시점 스냅샷
		private String bookCover;
		private Integer price;      // 주문 시점 스냅샷
		private Integer quantity;

		public static OrderItemResponseDto from(OrderItem item) {
			OrderItemResponseDto dto = new OrderItemResponseDto();
			dto.setId(item.getId());
			dto.setBookId(item.getBook().getId());
			dto.setBookTitle(item.getBookTitleSnapshot());
			dto.setBookCover(item.getBook().getBookCover());
			dto.setPrice(item.getPrice());
			dto.setQuantity(item.getQuantity());
			return dto;
		}
	}

	// 주문 응답 Dto
	@Getter @Setter @AllArgsConstructor @NoArgsConstructor
	public static class OrderResponseDto {
		private Long id;
		private Integer totalAmount;
		private OrderStatus orderStatus;
		private String tid;
		private List<OrderItemResponseDto> items;
		private LocalDateTime createdAt;
		private LocalDateTime approvedAt;

		public static OrderResponseDto from(Orders order) {
			OrderResponseDto dto = new OrderResponseDto();
			dto.setId(order.getId());
			dto.setTotalAmount(order.getTotalAmount());
			dto.setOrderStatus(order.getOrderStatus());
			dto.setTid(order.getTid());
			dto.setItems(order.getItems().stream()
					.map(OrderItemResponseDto::from)
					.collect(Collectors.toList()));
			dto.setCreatedAt(order.getCreatedAt());
			dto.setApprovedAt(order.getApprovedAt());
			return dto;
		}
	}
}
