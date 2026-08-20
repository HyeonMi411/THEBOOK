package com.thejoa703.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.entity.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> { // Entity , PK

	// 장바구니에 담긴 항목 전체조회 (담은 순서 - id 오름차순)
	List<CartItem> findByCart_IdOrderByIdAsc(Long cartId);

	// 같은 도서를 이미 담았는지 확인 (있으면 수량만 더해서 update, 없으면 새로 insert)
	Optional<CartItem> findByCart_IdAndBook_Id(Long cartId, Long bookId);

	// 장바구니 비우기(결제 완료 후 등)
	@Transactional
	void deleteByCart_Id(Long cartId);

	// 장바구니에서 특정 도서 항목들만 선택삭제
	@Transactional
	void deleteByCart_IdAndIdIn(Long cartId, List<Long> itemIds);
}
