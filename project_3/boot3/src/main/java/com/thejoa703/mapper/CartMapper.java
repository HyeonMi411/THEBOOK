package com.thejoa703.mapper;

import com.thejoa703.entity.Cart;
import org.apache.ibatis.annotations.Mapper;

/**
 * Cart MyBatis 매퍼 (★조회전용)
 * ------------------------------------------------------------------
 * 장바구니 담기/삭제는 CartItem 의 cascade(orphanRemoval) 처리와 맞물려 있어서,
 * 실제 등록/수정/삭제는 반드시 JPA(CartRepository + Service) 경로로만 처리하세요.
 * ------------------------------------------------------------------
 */
@Mapper
public interface CartMapper {

	// 사용자의 장바구니 단건조회
	Cart findByUserId(Long userId);
}
