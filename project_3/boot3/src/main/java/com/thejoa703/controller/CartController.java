package com.thejoa703.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thejoa703.dto.CartDto.CartItemRequestDto;
import com.thejoa703.dto.CartDto.CartItemUpdateRequestDto;
import com.thejoa703.dto.CartDto.CartResponseDto;
import com.thejoa703.service.AuthUserJwtService;
import com.thejoa703.service.CartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Cart Api", description = "장바구니 관련 API (로그인한 회원이면 누구나 이용 가능)")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;
	private final AuthUserJwtService authUserJwtService;

	@Operation(summary = "장바구니 조회")
	@GetMapping
	public ResponseEntity<CartResponseDto> getCart(Authentication authentication) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		return ResponseEntity.ok(cartService.getCart(userId));
	}

	@Operation(summary = "장바구니에 도서 담기", description = "이미 담긴 도서면 수량을 더합니다. 재고초과시 거부됩니다.")
	@PostMapping
	public ResponseEntity<CartResponseDto> addToCart(
			Authentication authentication,
			@Valid @RequestBody CartItemRequestDto dto
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		return ResponseEntity.ok(cartService.addToCart(userId, dto));
	}

	@Operation(summary = "장바구니 항목 수량수정")
	@PatchMapping("/{itemId}")
	public ResponseEntity<CartResponseDto> updateQuantity(
			Authentication authentication,
			@Parameter(description = "장바구니 항목 ID") @PathVariable("itemId") Long itemId,
			@Valid @RequestBody CartItemUpdateRequestDto dto
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		return ResponseEntity.ok(cartService.updateQuantity(userId, itemId, dto.getQuantity()));
	}

	@Operation(summary = "장바구니 항목 삭제 (선택삭제)")
	@DeleteMapping("/{itemId}")
	public ResponseEntity<Void> removeItem(
			Authentication authentication,
			@Parameter(description = "장바구니 항목 ID") @PathVariable("itemId") Long itemId
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		cartService.removeItem(userId, itemId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "장바구니 비우기 (전체삭제)")
	@DeleteMapping
	public ResponseEntity<Void> clearCart(Authentication authentication) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		cartService.clearCart(userId);
		return ResponseEntity.noContent().build();
	}
}
