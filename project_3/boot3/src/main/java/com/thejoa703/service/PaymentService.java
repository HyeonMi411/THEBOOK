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
import com.thejoa703.mapper.BookStockMapper;
import com.thejoa703.mapper.OrderItemMapper;
import com.thejoa703.mapper.OrdersMapper;

import lombok.RequiredArgsConstructor;

/**
 * 카카오페이 결제 서비스 - 로그인한 사용자라면 누구나 이용 가능 (관리자 전용 아님)
 * ------------------------------------------------------------------
 * 결제흐름(3단계) : 결제준비(ready) → 사용자가 카카오페이 결제창에서 결제 → 결제승인(approve)
 * - 결제승인이 실제로 완료된 시점에만 재고를 차감합니다.
 * - 재고차감은 비관적 락(SELECT ... FOR UPDATE)으로 동시성을 제어하고, 갱신 자체는
 *   낙관적 락(버전체크 UPDATE)으로 다시 한번 확정합니다.
 * ------------------------------------------------------------------
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

	private final OrdersMapper       ordersMapper;
	private final OrderItemMapper    orderItemMapper;
	private final BookStockMapper    bookStockMapper;
	private final KakaoPayApiService kakaoPayApiService;
	private final BookService        bookService; // 결제완료시 베스트셀러 캐시 무효화용
	private final ObjectMapper       objectMapper = new ObjectMapper();

	@Value("${app.frontend-base-url:http://localhost:3000}")
	private String frontendBaseUrl;

	@Transactional
	public PaymentReadyResponseDto ready(Long userId, Long orderId) {
		Orders order = getMyPendingOrder(userId, orderId);
		order.setItems(orderItemMapper.findByOrderId(orderId));

		for (OrderItem item : order.getItems()) {
			BookStock stock = bookStockMapper.findByBookId(item.getBook().getId());
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
		ordersMapper.update(order);

		PaymentReadyResponseDto dto = new PaymentReadyResponseDto();
		dto.setOrderId(orderId);
		dto.setTid(res.getTid());
		dto.setRedirectUrl(res.getNext_redirect_pc_url());
		return dto;
	}

	// 결제 승인 - 재고차감을 여기서 처리합니다 (비관적 락 + 낙관적 락 버전체크)
	@Transactional
	public OrderResponseDto approve(Long userId, Long orderId, String pgToken) {
		Orders order = ordersMapper.findById(orderId);
		if (order == null) {
			throw new ResourceNotFoundException("존재하지 않는 주문입니다. ID : " + orderId);
		}
		if (!order.getUser().getId().equals(userId)) {
			throw new IllegalStateException("본인의 주문만 승인할 수 있습니다.");
		}
		order.setItems(orderItemMapper.findByOrderId(orderId));

		if (order.getOrderStatus() == OrderStatus.PAID) {
			return OrderResponseDto.from(order); // 이미 승인된 주문 - 중복호출 방지(그대로 반환)
		}
		if (order.getTid() == null) {
			throw new IllegalStateException("결제 준비가 되지 않은 주문입니다.");
		}

		KakaoPayApproveResponse res = kakaoPayApiService.approve(
				order.getTid(), String.valueOf(orderId), String.valueOf(userId), pgToken
		);

		for (OrderItem item : order.getItems()) {
			BookStock stock = bookStockMapper.findByBookIdForUpdate(item.getBook().getId());
			if (stock == null) {
				throw new IllegalStateException("재고 정보가 없습니다: " + item.getBookTitleSnapshot());
			}
			if (stock.getStockQuantity() < item.getQuantity()) {
				throw new IllegalStateException("[" + item.getBookTitleSnapshot() + "] 재고가 부족합니다.");
			}
			stock.setStockQuantity(stock.getStockQuantity() - item.getQuantity());
			int updated = bookStockMapper.updateWithVersionCheck(stock);
			if (updated == 0) {
				throw new IllegalStateException("[" + item.getBookTitleSnapshot() + "] 재고 갱신 충돌이 발생했습니다. 다시 시도해주세요.");
			}
		}

		order.setOrderStatus(OrderStatus.PAID);
		order.setApprovedAt(LocalDateTime.now());
		order.setKakaoResponseJson(toJsonSafely(res));
		ordersMapper.update(order);

		// 판매량이 바뀌었으므로 베스트셀러(TOP 10) 캐시를 무효화해서 다음 조회 때 최신 랭킹으로 재계산되게 합니다.
		bookService.evictBestsellerCache();

		return OrderResponseDto.from(order);
	}

	@Transactional
	public void cancel(Long userId, Long orderId) {
		Orders order = getMyOrder(userId, orderId);
		order.setOrderStatus(OrderStatus.CANCELLED);
		ordersMapper.update(order);
	}

	@Transactional
	public void fail(Long userId, Long orderId) {
		Orders order = getMyOrder(userId, orderId);
		order.setOrderStatus(OrderStatus.FAILED);
		ordersMapper.update(order);
	}

	private Orders getMyOrder(Long userId, Long orderId) {
		Orders order = ordersMapper.findById(orderId);
		if (order == null) {
			throw new ResourceNotFoundException("존재하지 않는 주문입니다. ID : " + orderId);
		}
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
