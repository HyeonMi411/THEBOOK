package com.thejoa703.mapper;

import lombok.Getter;
import lombok.Setter;

// BOOK 테이블 카테고리별 집계 결과 매핑용 (Entity 아님, 조회전용 값객체)
@Getter @Setter
public class BookCategoryStat {
	private String category;
	private Long   bookCount;
	private Double avgRating;
}
