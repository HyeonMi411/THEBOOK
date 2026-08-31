package com.thejoa703.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.thejoa703.entity.CartItem;

@Mapper
public interface CartItemMapper {

	List<CartItem> findByCartId(Long cartId);

	CartItem findById(Long id);

	CartItem findByCartIdAndBookId(@Param("cartId") Long cartId, @Param("bookId") Long bookId);

	void insert(CartItem item);

	void update(CartItem item);

	void deleteById(Long id);

	void deleteByCartId(Long cartId);
}
