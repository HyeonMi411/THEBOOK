package com.thejoa703.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.Book;

@Mapper
public interface BookMapper {
	// 제목 키워드로 검색 (boot2 DeptUserMapper.findByNameKeyword 참고)
	List<Book> findByKeyword(String keyword);

	// 카테고리별 통계(도서수/평균평점) - JPQL 로는 표현이 번거로운 집계라 MyBatis 사용
	List<BookCategoryStat> findCategoryStats();
}
