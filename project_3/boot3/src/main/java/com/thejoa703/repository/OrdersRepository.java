package com.thejoa703.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.Orders;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> {

	Optional<Orders> findByTid(String tid);

	// hiddenByUser=true(사용자가 숨긴 결제완료/취소/실패 건)는 목록에서 제외합니다
	Page<Orders> findByUser_IdAndHiddenByUserFalseOrderByIdDesc(Long userId, Pageable pageable);
}
