package com.thejoa703.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> { // Entity , PK

	// 사용자당 장바구니 1개 - 로그인한 사용자의 장바구니 조회
	Optional<Cart> findByUser_Id(Long userId);

	// 장바구니 존재여부 확인 (없으면 최초 담기 시 새로 생성)
	boolean existsByUser_Id(Long userId);
}
