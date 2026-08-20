package com.thejoa703.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> { // Entity , PK

	// 특정 주문에 담긴 상품목록 (보통은 Orders.items 로 접근하지만, 직접 조회가 필요할 때 사용)
	List<OrderItem> findByOrder_Id(Long orderId);

	// ★베스트셀러/판매통계 - 결제완료(PAID)된 주문만 집계해서 많이 팔린 도서 TOP N 의
	//   (도서ID, 총 판매수량) 목록을 반환합니다. Oracle 오프셋/페치 문법(FETCH FIRST) 사용.
	//   OrderItem 이 Book 과 FK 로 직접 연결되어 있어 SQL 로 바로 집계가 가능합니다
	//   (요구사항정의서 8-14 항목과 대응).
	@Query(value =
			"SELECT oi.book_id AS bookId, SUM(oi.quantity) AS totalQty " +
			"FROM order_items oi " +
			"JOIN orders o ON oi.order_id = o.id " +
			"WHERE o.order_status = 'PAID' " +
			"GROUP BY oi.book_id " +
			"ORDER BY totalQty DESC " +
			"FETCH FIRST :limit ROWS ONLY",
			nativeQuery = true)
	List<Object[]> findBestSellerBookIds(@Param("limit") int limit);
}
