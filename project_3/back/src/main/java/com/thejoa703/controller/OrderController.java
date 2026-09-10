package com.thejoa703.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thejoa703.dto.OrderDto.OrderCreateRequestDto;
import com.thejoa703.dto.OrderDto.OrderResponseDto;
import com.thejoa703.dto.PageResponseDto;
import com.thejoa703.service.AuthUserJwtService;
import com.thejoa703.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Order Api", description = "주문 관련 API (로그인한 회원이면 누구나 이용 가능)")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;
	private final AuthUserJwtService authUserJwtService;

	@Operation(
			summary = "주문 생성",
			description = "cartItemIds 를 넘기면 장바구니 결제, bookId(+quantity) 를 넘기면 바로구매입니다. 결제 전 단계(PENDING)로 생성됩니다."
	)
	@PostMapping
	public ResponseEntity<OrderResponseDto> createOrder(
			Authentication authentication,
			@Valid @RequestBody OrderCreateRequestDto dto
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		return ResponseEntity.ok(orderService.createOrder(userId, dto));
	}

	@Operation(summary = "내 주문내역 조회 (12개씩 페이징)")
	@GetMapping
	public ResponseEntity<PageResponseDto<OrderResponseDto>> getMyOrders(
			Authentication authentication,
			@Parameter(description = "페이지 번호(1부터)") @RequestParam(name = "page", defaultValue = "1") int page,
			@Parameter(description = "페이지당 개수") @RequestParam(name = "size", defaultValue = "12") int size
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		return ResponseEntity.ok(orderService.getMyOrders(userId, page, size));
	}

	@Operation(summary = "주문 상세 조회 (본인 주문만)")
	@GetMapping("/{id}")
	public ResponseEntity<OrderResponseDto> getOrder(
			Authentication authentication,
			@Parameter(description = "주문 ID") @PathVariable("id") Long id
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		return ResponseEntity.ok(orderService.getOrder(userId, id));
	}

	@Operation(
			summary = "주문 삭제 (본인 주문만)",
			description = "결제전(PENDING) 주문은 실제로 DB에서 삭제됩니다. "
					+ "결제완료/취소/실패 주문은 회계·이력 보존을 위해 DB에서 지우지 않고, "
					+ "\"숨기기\" 처리되어 이후 내 주문내역 목록에서만 보이지 않게 됩니다."
	)
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteOrder(
			Authentication authentication,
			@Parameter(description = "삭제할 주문 ID") @PathVariable("id") Long id
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		orderService.deleteOrder(userId, id);
		return ResponseEntity.noContent().build();
	}
}
