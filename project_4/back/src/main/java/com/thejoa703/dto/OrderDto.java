package com.thejoa703.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.thejoa703.entity.OrderItem;
import com.thejoa703.entity.OrderStatus;
import com.thejoa703.entity.Orders;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class OrderDto {

	// 주문 생성 요청 Dto
	// - cartItemIds 를 넘기면 "장바구니에서 선택한 항목으로 주문"
	// - bookId + quantity 를 넘기면 "바로구매"
	// - 즉 둘 중 하나만 채워서 요청.
	@Getter @Setter @AllArgsConstructor @NoArgsConstructor
	public static class OrderCreateRequestDto {
		private List<Long> cartItemIds; // 장바구니 결제용 (선택한 CartItem id 목록)

		private Long bookId;            // 바로구매용
		@Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
		private Integer quantity;       // 바로구매용

		// 배송 정보 (앱 주문서에서 필수 입력, Swagger 테스트 편의를 위해 서버에서는 값이 있을 때만 형식 검증)
		@Size(max = 50, message = "받는 분 이름은 50자 이하로 입력해주세요.")
		private String receiverName;
		@Pattern(regexp = "^$|^01[016789]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
		private String receiverPhone;
		@Pattern(regexp = "^$|^\\d{5}$", message = "우편번호는 5자리 숫자입니다.")
		private String zipcode;
		@Size(max = 255, message = "주소가 너무 깁니다.")
		private String address;
		@Size(max = 255, message = "상세주소가 너무 깁니다.")
		private String addressDetail;

		/** 기존 테스트/호출부 호환용 (배송정보 없이 주문) */
		public OrderCreateRequestDto(List<Long> cartItemIds, Long bookId, Integer quantity) {
			this.cartItemIds = cartItemIds;
			this.bookId = bookId;
			this.quantity = quantity;
		}
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
		private String receiverName;
		private String receiverPhone;
		private String zipcode;
		private String address;
		private String addressDetail;

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
			dto.setReceiverName(order.getReceiverName());
			dto.setReceiverPhone(order.getReceiverPhone());
			dto.setZipcode(order.getZipcode());
			dto.setAddress(order.getAddress());
			dto.setAddressDetail(order.getAddressDetail());
			return dto;
		}
	}
}
