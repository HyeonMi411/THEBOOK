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

		// ★cart.getItems()(엔티티의 메모리상 컬렉션)에 의존하지 않고, 호출 측에서
		//   CartItemRepository 로 "방금 막" 조회한 목록을 그대로 넘겨받습니다.
		//   Cart.items 는 addToCart()/removeItem() 등 여러 메서드에서 개별적으로 손대다보니
		//   항상 실제 DB 상태와 100% 동기화된다고 신뢰하기 어렵습니다(양방향 연관관계 특성상).
		//   그래서 화면에 보여줄 목록은 항상 자식 테이블(CART_ITEM)에서 새로 조회한 값을
		//   기준으로 만듭니다 - 이게 가장 확실하게 실제 DB 상태를 반영하는 방법입니다.
		public static CartResponseDto from(Cart cart, java.util.List<CartItem> freshItems) {
			CartResponseDto dto = new CartResponseDto();
			dto.setId(cart.getId());
			dto.setCreatedAt(cart.getCreatedAt());
			java.util.List<CartItemResponseDto> items = freshItems.stream()
					.map(CartItemResponseDto::from)
					.collect(java.util.stream.Collectors.toList());
			dto.setItems(items);
			dto.setTotalAmount(items.stream().mapToInt(CartItemResponseDto::getSubtotal).sum());
			return dto;
		}
	}
}
