package com.thejoa703.dto;

import java.time.LocalDateTime;

import com.thejoa703.entity.Cart;
import com.thejoa703.entity.CartItem;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class CartDto {

	// 장바구니 담기 요청 Dto
	@Getter @Setter @AllArgsConstructor @NoArgsConstructor
	public static class CartItemRequestDto {
		@NotNull(message = "도서 ID는 필수입니다.")
		private Long bookId;

		@NotNull(message = "수량은 필수입니다.")
		@Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
		private Integer quantity;
	}

	// 장바구니 항목 수량수정 요청 Dto
	@Getter @Setter @AllArgsConstructor @NoArgsConstructor
	public static class CartItemUpdateRequestDto {
		@NotNull(message = "수량은 필수입니다.")
		@Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
		private Integer quantity;
	}

	// 장바구니 항목 응답 Dto
	@Getter @Setter @AllArgsConstructor @NoArgsConstructor
	public static class CartItemResponseDto {
		private Long id;
		private Long bookId;
		private String bookTitle;
		private String bookCover;
		private Integer price;      // 도서 현재가격 (표시용)
		private Integer quantity;
		private Integer subtotal;   // price * quantity
		private Integer stockQuantity; // 현재 재고 (품절/재고부족 표시용)
		private boolean bookDeleted;   // 담은 이후 관리자가 판매중단(소프트삭제)한 도서인지

		public static CartItemResponseDto from(CartItem item) {
			CartItemResponseDto dto = new CartItemResponseDto();
			dto.setId(item.getId());
			dto.setBookId(item.getBook().getId());
			dto.setBookTitle(item.getBook().getTitle());
			dto.setBookCover(item.getBook().getBookCover());
			dto.setPrice(item.getBook().getPrice());
			dto.setQuantity(item.getQuantity());
			dto.setSubtotal((item.getBook().getPrice() != null ? item.getBook().getPrice() : 0) * item.getQuantity());
			dto.setStockQuantity(
					item.getBook().getStock() != null ? item.getBook().getStock().getStockQuantity() : 0
			);
			dto.setBookDeleted(item.getBook().isDeleted());
			return dto;
		}
	}

	// 장바구니 전체 응답 Dto
	@Getter @Setter @AllArgsConstructor @NoArgsConstructor
	public static class CartResponseDto {
		private Long id;
		private java.util.List<CartItemResponseDto> items;
		private Integer totalAmount;
		private LocalDateTime createdAt;

		// 호출 측에서 CartItemRepository 로 방금 막 조회한 목록을 그대로 넘겨받음.
		public static CartResponseDto from(Cart cart, java.util.List<CartItem> freshItems) {
			CartResponseDto dto = new CartResponseDto();
			dto.setId(cart.getId());
			dto.setCreatedAt(cart.getCreatedAt());
			java.util.List<CartItemResponseDto> items = freshItems.stream()
					.map(CartItemResponseDto::from)
					.collect(java.util.stream.Collectors.toList());
			dto.setItems(items);
			// 판매중단(삭제)된 도서는 어차피 구매할 수 없으므로 결제예정금액에서 제외.
			dto.setTotalAmount(items.stream()
					.filter(i -> !i.isBookDeleted())
					.mapToInt(CartItemResponseDto::getSubtotal)
					.sum());
			return dto;
		}
	}
}
