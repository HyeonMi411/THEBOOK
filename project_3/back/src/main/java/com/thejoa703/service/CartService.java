package com.thejoa703.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.dto.CartDto.CartItemRequestDto;
import com.thejoa703.dto.CartDto.CartResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Book;
import com.thejoa703.entity.Cart;
import com.thejoa703.entity.CartItem;
import com.thejoa703.exception.ResourceNotFoundException;
import com.thejoa703.mapper.BookMapper;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.CartItemRepository;
import com.thejoa703.repository.CartRepository;

import lombok.RequiredArgsConstructor;

/**
 * 장바구니 서비스 - 로그인한 사용자라면 누구나 이용 가능 (관리자 전용 아님)
 * - Cart/CartItem 은 단순 CRUD 라 JPA Repository 를 사용.
 * - Book 조회는 검색/JOIN 이 복잡해 Mapper(BookMapper)를 그대로 사용.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

	private final CartRepository     cartRepository;
	private final CartItemRepository cartItemRepository;
	private final BookMapper         bookMapper;
	private final AppUserRepository  appUserRepository;

	// 1. 내 장바구니 조회 (아직 장바구니가 없으면 빈 상태로 응답, DB에는 생성하지 않음)
	public CartResponseDto getCart(Long userId) {
		Cart cart = cartRepository.findByUser_Id(userId).orElse(null);
		if (cart == null) {
			CartResponseDto empty = new CartResponseDto();
			empty.setItems(List.of());
			empty.setTotalAmount(0);
			return empty;
		}
		List<CartItem> freshItems = cartItemRepository.findByCart_IdOrderByIdAsc(cart.getId());
		return CartResponseDto.from(cart, freshItems);
	}

	// 2. 장바구니에 담기 (이미 담긴 도서면 수량을 더함, 재고초과시 거부, 판매중단 도서는 거부)
	@Transactional
	public CartResponseDto addToCart(Long userId, CartItemRequestDto dto) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다. ID : " + userId));
		Book book = bookMapper.findById(dto.getBookId());
		if (book == null || book.isDeleted()) {
			throw new ResourceNotFoundException("존재하지 않는 도서입니다. ID : " + dto.getBookId());
		}

		Cart cart = cartRepository.findByUser_Id(userId).orElseGet(() -> {
			Cart newCart = new Cart();
			newCart.setUser(user);
			return cartRepository.save(newCart);
		});

		int stockQuantity = (book.getStock() != null) ? book.getStock().getStockQuantity() : 0;

		CartItem existing = cartItemRepository.findByCart_IdAndBook_Id(cart.getId(), book.getId()).orElse(null);
		if (existing != null) {
			int newQuantity = existing.getQuantity() + dto.getQuantity();
			if (newQuantity > stockQuantity) {
				throw new IllegalStateException("[" + book.getTitle() + "] 재고가 부족합니다. (현재 재고 : " + stockQuantity + "권)");
			}
			existing.setQuantity(newQuantity); // 더티체킹으로 트랜잭션 커밋시 자동 UPDATE
		} else {
			if (dto.getQuantity() > stockQuantity) {
				throw new IllegalStateException("[" + book.getTitle() + "] 재고가 부족합니다. (현재 재고 : " + stockQuantity + "권)");
			}
			CartItem item = new CartItem();
			item.setCart(cart);
			item.setBook(book);
			item.setQuantity(dto.getQuantity());
			cartItemRepository.save(item);
		}

		List<CartItem> freshItems = cartItemRepository.findByCart_IdOrderByIdAsc(cart.getId());
		return CartResponseDto.from(cart, freshItems);
	}

	// 3. 장바구니 항목 수량수정
	@Transactional
	public CartResponseDto updateQuantity(Long userId, Long itemId, int quantity) {
		Cart cart = cartRepository.findByUser_Id(userId)
				.orElseThrow(() -> new ResourceNotFoundException("장바구니가 없습니다."));
		CartItem item = cartItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("장바구니 항목이 없습니다. ID : " + itemId));
		if (!item.getCart().getId().equals(cart.getId())) {
			throw new IllegalStateException("본인의 장바구니 항목만 수정할 수 있습니다.");
		}

		int stockQuantity = (item.getBook().getStock() != null) ? item.getBook().getStock().getStockQuantity() : 0;
		// 판매중단(삭제)된 도서는 수량을 늘릴 수 없음(줄이거나 삭제(removeItem)하는 것은 항상 허용).
		if (item.getBook().isDeleted() && quantity > item.getQuantity()) {
			throw new IllegalStateException("[" + item.getBook().getTitle() + "] 판매가 중단된 도서라 수량을 늘릴 수 없습니다. 장바구니에서 삭제해주세요.");
		}
		if (quantity > stockQuantity) {
			throw new IllegalStateException("[" + item.getBook().getTitle() + "] 재고가 부족합니다. (현재 재고 : " + stockQuantity + "권)");
		}

		item.setQuantity(quantity);

		List<CartItem> freshItems = cartItemRepository.findByCart_IdOrderByIdAsc(cart.getId());
		return CartResponseDto.from(cart, freshItems);
	}

	// 4. 장바구니 항목 삭제 (선택삭제)
	@Transactional
	public void removeItem(Long userId, Long itemId) {
		Cart cart = cartRepository.findByUser_Id(userId)
				.orElseThrow(() -> new ResourceNotFoundException("장바구니가 없습니다."));
		CartItem item = cartItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("장바구니 항목이 없습니다. ID : " + itemId));
		if (!item.getCart().getId().equals(cart.getId())) {
			throw new IllegalStateException("본인의 장바구니 항목만 삭제할 수 있습니다.");
		}
		cartItemRepository.deleteById(itemId);
	}

	// 5. 장바구니 비우기 (전체삭제)
	@Transactional
	public void clearCart(Long userId) {
		cartRepository.findByUser_Id(userId)
				.ifPresent(cart -> cartItemRepository.deleteByCart_Id(cart.getId()));
	}
}
