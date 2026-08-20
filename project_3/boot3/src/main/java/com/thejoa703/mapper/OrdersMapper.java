package com.thejoa703.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.Orders;

/**
 * Orders MyBatis 매퍼 (★조회전용)
 * ------------------------------------------------------------------
 * 주문 생성/상태변경은 OrderItem cascade, 재고차감 트랜잭션과 맞물려 있어서,
 * 실제 등록/수정은 반드시 JPA(OrdersRepository + Service) 경로로만 처리하세요.
 * ------------------------------------------------------------------
 */
@Mapper
public interface OrdersMapper {

	// 주문 단건조회
	Orders findById(Long id);

	// 카카오페이 결제승인 콜백에서 tid 로 원래 주문 찾기
	Orders findByTid(String tid);

	// 내 주문내역 - 페이징 (map : { userId, start, end })
	List<Orders> findByUserId(Map<String, Object> map);

	// 내 주문내역 전체 건수
	int countByUserId(Long userId);
}
