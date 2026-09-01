package com.thejoa703.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	List<CartItem> findByCart_IdOrderByIdAsc(Long cartId);

	Optional<CartItem> findByCart_IdAndBook_Id(Long cartId, Long bookId);

	void deleteByCart_Id(Long cartId);
}
