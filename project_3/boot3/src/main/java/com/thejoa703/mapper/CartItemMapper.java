package com.thejoa703.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.CartItem;

/**
 * CartItem MyBatis 매퍼 (★조회전용)
 * ------------------------------------------------------------------
 * 담기/수량변경/삭제는 반드시 JPA(CartItemRepository + Service) 경로로만 처리하세요.
 * ------------------------------------------------------------------
 */
@Mapper
public interface CartItemMapper {

	// 장바구니에 담긴 항목 전체조회
	List<CartItem> findByCartId(Long cartId);
}
