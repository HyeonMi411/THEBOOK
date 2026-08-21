package com.thejoa703.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.dto.CartDto.CartItemRequestDto;
import com.thejoa703.dto.CartDto.CartResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Book;
import com.thejoa703.entity.Cart;
import com.thejoa703.entity.CartItem;
import com.thejoa703.exception.ResourceNotFoundException;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.BookRepository;
import com.thejoa703.repository.CartItemRepository;
import com.thejoa703.repository.CartRepository;

import lombok.RequiredArgsConstructor;

/**
 * 장바구니 서비스 - 로그인한 사용자라면 누구나 이용 가능 (관리자 전용 아님)
 * ------------------------------------------------------------------
 * ★설계 원칙: Cart.items(엔티티의 메모리상 컬렉션, 양방향 연관관계)에 의존하지 않습니다.
 *   담기/수정/삭제 등 여러 메서드에서 각각 항목을 손대다보니, 부모(Cart)의 컬렉션을
 *   매번 정확히 동기화하기가 까다롭고(orphanRemoval 과 repository.delete() 를 함께
 *   쓰면 같은 행을 두 번 지우려다 충돌하는 등), 실수하기 쉬운 지점입니다.
 *   그래서 "화면에 보여줄 목록"은 항상 CartItemRepository 로 그 시점에 새로 조회해서
 *   만듭니다. 삭제도 repository.delete()/deleteByCart_Id() 로만 처리하고, Cart.items
 *   컬렉션 자체는 아예 건드리지 않습니다 - 이러면 애초에 동기화가 안 맞을 여지가 없습니다.
 * ------------------------------------------------------------------
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

	private final CartRepository     cartRepository;
	private final CartItemRepository cartItemRepository;
	private final BookRepository     bookRepository;
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

	// 2. 장바구니에 담기 (이미 담긴 도서면 수량을 더함, 재고초과시 거부)
	@Transactional
	public CartResponseDto addToCart(Long userId, CartItemRequestDto dto) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다. ID : " + userId));
		Book book = bookRepository.findById(dto.getBookId())
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 도서입니다. ID : " + dto.getBookId()));

		Cart cart = cartRepository.findByUser_Id(userId).orElseGet(() -> {
			Cart newCart = new Cart();
			newCart.setUser(user);
			return cartRepository.save(newCart);
		});

		int stockQuantity = (book.getStock() != null) ? book.getStock().getStockQuantity() : 0;

		Optional<CartItem> existing = cartItemRepository.findByCart_IdAndBook_Id(cart.getId(), book.getId());
		if (existing.isPresent()) {
			CartItem item = existing.get();
			int newQuantity = item.getQuantity() + dto.getQuantity();
			if (newQuantity > stockQuantity) {
				throw new IllegalStateException("[" + book.getTitle() + "] 재고가 부족합니다. (현재 재고 : " + stockQuantity + "권)");
			}
			item.setQuantity(newQuantity);
			cartItemRepository.save(item);
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
		if (quantity > stockQuantity) {
			throw new IllegalStateException("[" + item.getBook().getTitle() + "] 재고가 부족합니다. (현재 재고 : " + stockQuantity + "권)");
		}

		item.setQuantity(quantity);
		cartItemRepository.save(item);

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
		// ★Cart.items 컬렉션은 건드리지 않고, repository 로만 삭제합니다 (동기화 문제 회피)
		cartItemRepository.delete(item);
	}

	// 5. 장바구니 비우기 (전체삭제)
	@Transactional
	public void clearCart(Long userId) {
		Cart cart = cartRepository.findByUser_Id(userId).orElse(null);
		if (cart != null) {
			cartItemRepository.deleteByCart_Id(cart.getId());
		}
	}
}
