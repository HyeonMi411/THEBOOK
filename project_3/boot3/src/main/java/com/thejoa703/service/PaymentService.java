package com.thejoa703.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thejoa703.api.KakaoPayApiService;
import com.thejoa703.api.KakaoPayApproveResponse;
import com.thejoa703.api.KakaoPayReadyResponse;
import com.thejoa703.dto.OrderDto.OrderResponseDto;
import com.thejoa703.dto.PaymentDto.PaymentReadyResponseDto;
import com.thejoa703.entity.BookStock;
import com.thejoa703.entity.OrderItem;
import com.thejoa703.entity.OrderStatus;
import com.thejoa703.entity.Orders;
import com.thejoa703.exception.ResourceNotFoundException;
import com.thejoa703.repository.BookStockRepository;
import com.thejoa703.repository.OrdersRepository;

import lombok.RequiredArgsConstructor;

/**
 * 카카오페이 결제 서비스 - 로그인한 사용자라면 누구나 이용 가능 (관리자 전용 아님)
 * ------------------------------------------------------------------
 * 결제흐름(3단계) : 결제준비(ready) → 사용자가 카카오페이 결제창에서 결제 → 결제승인(approve)
 * - 결제승인이 실제로 완료된 시점에만 재고를 차감합니다 (결제준비 단계에서 미리 차감하면
 *   결제를 끝내지 않은 사용자 때문에 다른 사람이 못 사는 상황이 생기기 때문입니다)
 * - 재고차감은 비관적 락(SELECT ... FOR UPDATE)으로 동시성을 제어합니다.
 * ------------------------------------------------------------------
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

	private final OrdersRepository     ordersRepository;
	private final BookStockRepository  bookStockRepository;
	private final KakaoPayApiService   kakaoPayApiService;
	private final ObjectMapper         objectMapper = new ObjectMapper();

	@Value("${app.frontend-base-url:http://localhost:3000}")
	private String frontendBaseUrl;

	// 1. 결제 준비
	@Transactional
	public PaymentReadyResponseDto ready(Long userId, Long orderId) {
		Orders order = getMyPendingOrder(userId, orderId);

		// 재고 재확인 (담긴 이후 다른 사람이 먼저 사갔을 수 있음)
		for (OrderItem item : order.getItems()) {
			BookStock stock = item.getBook().getStock();
			int stockQuantity = (stock != null) ? stock.getStockQuantity() : 0;
			if (item.getQuantity() > stockQuantity) {
				throw new IllegalStateException("[" + item.getBookTitleSnapshot() + "] 재고가 부족합니다.");
			}
		}

		String itemName = buildItemName(order);
		int totalQuantity = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();

		String approvalUrl = frontendBaseUrl + "/payment/complete?orderId=" + orderId;
		String cancelUrl   = frontendBaseUrl + "/payment/cancel?orderId=" + orderId;
		String failUrl     = frontendBaseUrl + "/payment/fail?orderId=" + orderId;

		KakaoPayReadyResponse res = kakaoPayApiService.ready(
				String.valueOf(orderId), String.valueOf(userId), itemName,
				totalQuantity, order.getTotalAmount(), approvalUrl, cancelUrl, failUrl
		);

		order.setTid(res.getTid());
		ordersRepository.save(order);

		PaymentReadyResponseDto dto = new PaymentReadyResponseDto();
		dto.setOrderId(orderId);
		dto.setTid(res.getTid());
		dto.setRedirectUrl(res.getNext_redirect_pc_url());
		return dto;
	}

	// 2. 결제 승인 - ★재고차감을 여기서 처리합니다 (비관적 락)
	@Transactional
	public OrderResponseDto approve(Long userId, Long orderId, String pgToken) {
		Orders order = ordersRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 주문입니다. ID : " + orderId));
		if (!order.getUser().getId().equals(userId)) {
			throw new IllegalStateException("본인의 주문만 승인할 수 있습니다.");
		}
		if (order.getOrderStatus() == OrderStatus.PAID) {
			return OrderResponseDto.from(order); // 이미 승인된 주문 - 중복호출 방지(그대로 반환)
		}
		if (order.getTid() == null) {
			throw new IllegalStateException("결제 준비가 되지 않은 주문입니다.");
		}

		KakaoPayApproveResponse res = kakaoPayApiService.approve(
				order.getTid(), String.valueOf(orderId), String.valueOf(userId), pgToken
		);

		// ★재고차감 - 비관적 락(findByIdForUpdate)으로 동시 결제 경쟁을 순서대로 처리
		for (OrderItem item : order.getItems()) {
			BookStock stock = bookStockRepository.findByIdForUpdate(item.getBook().getId())
					.orElseThrow(() -> new IllegalStateException("재고 정보가 없습니다: " + item.getBookTitleSnapshot()));
			if (stock.getStockQuantity() < item.getQuantity()) {
				throw new IllegalStateException("[" + item.getBookTitleSnapshot() + "] 재고가 부족합니다.");
			}
			stock.setStockQuantity(stock.getStockQuantity() - item.getQuantity());
			bookStockRepository.save(stock);
		}

		order.setOrderStatus(OrderStatus.PAID);
		order.setApprovedAt(LocalDateTime.now());
		order.setKakaoResponseJson(toJsonSafely(res)); // ★CLOB - 카카오 원본 응답 감사로그
		ordersRepository.save(order);

		return OrderResponseDto.from(order);
	}

	// 3. 결제 취소 (사용자가 카카오페이 결제창에서 취소)
	@Transactional
	public void cancel(Long userId, Long orderId) {
		Orders order = getMyOrder(userId, orderId);
		order.setOrderStatus(OrderStatus.CANCELLED);
		ordersRepository.save(order);
	}

	// 4. 결제 실패
	@Transactional
	public void fail(Long userId, Long orderId) {
		Orders order = getMyOrder(userId, orderId);
		order.setOrderStatus(OrderStatus.FAILED);
		ordersRepository.save(order);
	}

	private Orders getMyOrder(Long userId, Long orderId) {
		Orders order = ordersRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 주문입니다. ID : " + orderId));
		if (!order.getUser().getId().equals(userId)) {
			throw new IllegalStateException("본인의 주문만 처리할 수 있습니다.");
		}
		return order;
	}

	private Orders getMyPendingOrder(Long userId, Long orderId) {
		Orders order = getMyOrder(userId, orderId);
		if (order.getOrderStatus() != OrderStatus.PENDING) {
			throw new IllegalStateException("이미 처리된 주문입니다.");
		}
		return order;
	}

	private String buildItemName(Orders order) {
		String firstTitle = order.getItems().get(0).getBookTitleSnapshot();
		int otherCount = order.getItems().size() - 1;
		return (otherCount > 0) ? (firstTitle + " 외 " + otherCount + "건") : firstTitle;
	}

	private String toJsonSafely(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			return String.valueOf(obj);
		}
	}
}
