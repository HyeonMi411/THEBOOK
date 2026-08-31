package com.thejoa703.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.OrderItem;

@Mapper
public interface OrderItemMapper {

	List<OrderItem> findByOrderId(Long orderId);

	void insert(OrderItem item);

	void deleteByOrderId(Long orderId);

	// 베스트셀러/판매통계 - 결제완료(PAID) 주문만 집계해서 판매량 TOP N 조회
	// 반환값 : [ {BOOK_ID: 1, TOTAL_QTY: 15}, {BOOK_ID: 3, TOTAL_QTY: 9}, ... ]
	List<Map<String, Object>> findBestSellerBookIds(int limit);
}
