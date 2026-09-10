package com.thejoa703.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thejoa703.dto.OrderDto.OrderResponseDto;
import com.thejoa703.dto.PaymentDto.PaymentApproveRequestDto;
import com.thejoa703.dto.PaymentDto.PaymentReadyRequestDto;
import com.thejoa703.dto.PaymentDto.PaymentReadyResponseDto;
import com.thejoa703.service.AuthUserJwtService;
import com.thejoa703.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Payment Api", description = "카카오페이 결제 관련 API (로그인한 회원이면 누구나 이용 가능)")
@RestController
@RequestMapping("/api/payments/kakao")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;
	private final AuthUserJwtService authUserJwtService;

	@Operation(
			summary = "카카오페이 결제 준비",
			description = "PENDING 상태인 내 주문의 결제를 준비합니다. 응답의 redirectUrl 로 사용자를 이동시키면 카카오페이 결제창이 뜹니다."
	)
	@PostMapping("/ready")
	public ResponseEntity<PaymentReadyResponseDto> ready(
			Authentication authentication,
			@Valid @RequestBody PaymentReadyRequestDto dto
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		return ResponseEntity.ok(paymentService.ready(userId, dto.getOrderId()));
	}

	@Operation(
			summary = "카카오페이 결제 승인",
			description = "카카오 결제창에서 결제를 마치고 돌아온 뒤(pg_token 전달받은 시점), 이 API로 최종 승인 처리합니다. "
					+ "이 시점에 재고가 실제로 차감됩니다."
	)
	@PostMapping("/approve")
	public ResponseEntity<OrderResponseDto> approve(
			Authentication authentication,
			@Valid @RequestBody PaymentApproveRequestDto dto
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		return ResponseEntity.ok(paymentService.approve(userId, dto.getOrderId(), dto.getPgToken()));
	}

	@Operation(summary = "카카오페이 결제 취소", description = "결제창에서 사용자가 결제를 취소했을 때 호출합니다.")
	@PostMapping("/cancel/{orderId}")
	public ResponseEntity<Void> cancel(
			Authentication authentication,
			@Parameter(description = "주문 ID") @PathVariable("orderId") Long orderId
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		paymentService.cancel(userId, orderId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "카카오페이 결제 실패", description = "결제 진행 중 오류/거절 등으로 실패했을 때 호출합니다.")
	@PostMapping("/fail/{orderId}")
	public ResponseEntity<Void> fail(
			Authentication authentication,
			@Parameter(description = "주문 ID") @PathVariable("orderId") Long orderId
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		paymentService.fail(userId, orderId);
		return ResponseEntity.noContent().build();
	}
}
