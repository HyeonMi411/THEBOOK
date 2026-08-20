package com.thejoa703.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.OrderItem;

/**
 * OrderItem MyBatis 매퍼 (★조회전용)
 * ------------------------------------------------------------------
 * 주문상품은 Orders 저장시 cascade 로 함께 저장되므로, 실제 등록은 반드시
 * JPA(OrdersRepository + Service) 경로로만 처리하세요.
 * ------------------------------------------------------------------
 */
@Mapper
public interface OrderItemMapper {

	// 특정 주문에 담긴 상품목록
	List<OrderItem> findByOrderId(Long orderId);

	// ★베스트셀러/판매통계 - 결제완료(PAID) 주문만 집계해서 판매량 TOP N 조회
	//   반환값 : [ {BOOK_ID: 1, TOTAL_QTY: 15}, {BOOK_ID: 3, TOTAL_QTY: 9}, ... ]
	List<Map<String, Object>> findBestSellerBookIds(int limit);
}
