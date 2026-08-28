package com.thejoa703.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.OrderStatus;
import com.thejoa703.entity.Orders;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> { // Entity , PK

	// 내 주문내역 조회 (최신순, 12개씩 페이징 - 다른 목록화면들과 동일한 페이징 관례)
	// ★사용자가 "삭제(숨기기)"한 주문(hiddenByUser=true)은 목록에서 제외합니다.
	Page<Orders> findByUser_IdAndHiddenByUserFalseOrderByIdDesc(Long userId, Pageable pageable);

	// 카카오페이 결제승인 콜백에서 tid 로 원래 주문 찾기
	Optional<Orders> findByTid(String tid);

	// 특정 상태의 주문만 조회 (예: 결제대기 중인 오래된 주문 정리 배치 등에 활용 가능)
	Page<Orders> findByOrderStatusOrderByIdDesc(OrderStatus orderStatus, Pageable pageable);
}
