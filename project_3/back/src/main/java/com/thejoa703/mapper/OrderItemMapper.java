package com.thejoa703.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

/**
 * OrderItem 의 기본 CRUD(findByOrderId/insert/delete)는 JpaRepository(OrderItemRepository)로
 * 처리하고, 이 Mapper 는 여러 테이블을 JOIN+GROUP BY 하는 집계 쿼리(베스트셀러 통계)만 담당.
 */
@Mapper
public interface OrderItemMapper {

	// 베스트셀러/판매통계 - 결제완료(PAID) 주문만 집계해서 판매량 TOP N 조회
	// 반환값 : [ {BOOK_ID: 1, TOTAL_QTY: 15}, {BOOK_ID: 3, TOTAL_QTY: 9}, ... ]
	List<Map<String, Object>> findBestSellerBookIds(int limit);
}
