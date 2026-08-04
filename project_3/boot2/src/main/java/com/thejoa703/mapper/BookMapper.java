package com.thejoa703.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.Book;

@Mapper
public interface BookMapper {

	// 도서명(부분일치)으로 검색
	List<Book> findByTitleKeyword(String keyword);

	// 카테고리(분류)별 도서목록 조회
	List<Book> findByCategory(String category);

	// 저자로 검색
	List<Book> findByAuthorKeyword(String author);

	// 단건조회
	Book findById(Long bookId);

	// 도서목록 페이징 (최신등록순)
	List<Book> findBooksWithPaging(Map<String, Object> params);	// params: start, end

	// 전체 도서수
	long countAll();

	// 등록
	int insertBook(Book book);

	// 수정
	int updateBook(Book book);

	// 삭제
	int deleteBook(Long bookId);

	// 리뷰수 +1 (조회/구매등 이벤트 발생시)
	int increaseReviewCount(Long bookId);
}
