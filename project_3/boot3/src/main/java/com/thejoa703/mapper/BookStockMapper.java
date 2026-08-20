package com.thejoa703.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.BookStock;

/**
 * BookStock MyBatis 매퍼 (★조회전용)
 * ------------------------------------------------------------------
 * BookStock 은 @Version(낙관적 락)이 걸려있는 엔티티입니다. MyBatis 로 직접 UPDATE 를 하면
 * Hibernate 의 버전체크를 완전히 우회하게 되어 동시성 제어가 깨집니다. 그래서 이 매퍼는
 * 조회 전용 메서드만 제공하며, 재고 등록/변경/차감은 반드시 JPA(BookStockRepository +
 * Service) 경로로만 처리하세요.
 * ------------------------------------------------------------------
 */
@Mapper
public interface BookStockMapper {

	// 도서 재고 단건조회
	BookStock findByBookId(Long bookId);
}
