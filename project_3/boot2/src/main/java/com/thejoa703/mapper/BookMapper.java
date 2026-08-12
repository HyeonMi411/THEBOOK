package com.thejoa703.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.Book;

@Mapper
public interface BookMapper {

	// ------------------------------------------------------------
	// 📘 기본 CRUD
	// ------------------------------------------------------------
	// 전체 목록조회 (페이징) - map: start, end
	List<Book> findAll(Map<String, Object> map);

	// 전체 갯수
	int findAllCnt();

	// 단건조회
	Book findById(Long bookId);

	// 등록 - 관리자(ROLE_ADMIN)만 호출, book.user.id 필수
	int insert(Book book);

	// 수정
	int update(Book book);

	// 삭제
	int delete(Long bookId);


	// ------------------------------------------------------------
	// ⭐ 카테고리별 조회 (페이징)
	// ------------------------------------------------------------
	// map: category, start, end
	List<Book> findByCategory(Map<String, Object> map);

	// 카테고리별 갯수
	int findCategoryCnt(String category);


	// ------------------------------------------------------------
	// 🔍 통합검색 (제목 + 저자 + 카테고리) - map: searchType, keyword, start, end, orderBy
	// ------------------------------------------------------------
	List<Book> searchBooks(Map<String, Object> map);

	// 검색결과 갯수
	int searchBooksCnt(Map<String, Object> map);


	// ------------------------------------------------------------
	// ⭐ 도서명 중복검사(AJAX)
	// ------------------------------------------------------------
	int countByTitle(String title);


	// ------------------------------------------------------------
	// ★특정 관리자(AppUser)가 등록한 도서 목록 - APP_USER_ID 참조
	// ------------------------------------------------------------
	List<Book> findByUserId(Long userId);

}
