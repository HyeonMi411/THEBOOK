package com.thejoa703.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PaymentDto {

	// 결제 준비 요청 Dto
	@Getter @Setter @AllArgsConstructor @NoArgsConstructor
	public static class PaymentReadyRequestDto {
		@NotNull(message = "주문 ID는 필수입니다.")
		private Long orderId;
	}

	// 결제 준비 응답 Dto - 프론트는 이 redirectUrl 로 사용자를 이동시킵니다
	@Getter @Setter @AllArgsConstructor @NoArgsConstructor
	public static class PaymentReadyResponseDto {
		private Long orderId;
		private String tid;
		private String redirectUrl; // PC 기준 next_redirect_pc_url
	}

	// 결제 승인 요청 Dto - 카카오 리다이렉트 후 프론트가 pg_token 을 담아 호출
	@Getter @Setter @AllArgsConstructor @NoArgsConstructor
	public static class PaymentApproveRequestDto {
		@NotNull(message = "주문 ID는 필수입니다.")
		private Long orderId;

		@NotNull(message = "pg_token 은 필수입니다.")
		private String pgToken;
	}
}
