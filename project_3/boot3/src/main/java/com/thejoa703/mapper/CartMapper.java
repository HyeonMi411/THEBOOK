package com.thejoa703.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.Cart;

@Mapper
public interface CartMapper {

	Cart findByUserId(Long userId);

	boolean existsByUserId(Long userId);

	void insert(Cart cart);

	void deleteById(Long id);
}
