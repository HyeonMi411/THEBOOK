package com.thejoa703.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.Book;

/**
 * Book MyBatis 매퍼
 * ------------------------------------------------------------------
 * boot1(the703) 의 BookDao / book-mapper.xml 을 boot3 구조(JPA 엔티티 Book,
 * 시퀀스 BOOK_SEQ, APP_USER_ID 외래키)에 맞춰 재구성한 버전입니다.
 *
 * ★ 실제 서비스에서 등록/수정/삭제는 BookService(JPA, @PreAuthorize("hasRole('ADMIN')"))를
 *   통해서만 호출하세요. 이 매퍼의 insert/update/delete 는 필요시(배치작업, 관리자툴 등)
 *   직접 사용할 수 있도록 제공하는 것으로, 권한체크를 직접 하지 않으므로 컨트롤러에서
 *   바로 노출하지 않는 것을 권장합니다.
 * ------------------------------------------------------------------
 */
@Mapper
public interface BookMapper {

	// ------------------------------------
	// 📘 조회
	// ------------------------------------

	// 전체조회 (오라클 네이티브 페이징) - map : { start, end }
	List<Book> findAll(Map<String, Object> map);

	// 전체 게시글수
	int findAllCnt();

	// 단건조회
	Book findById(Long bookId);

	// 카테고리별 조회 (페이징) - map : { category, start, end }
	List<Book> findByCategory(Map<String, Object> map);

	// 카테고리별 개수
	int findCategoryCnt(String category);

	// 🔍 제목/저자/카테고리 통합검색 - map : { searchType, keyword }
	List<Book> searchBooks(Map<String, Object> map);

	// 제목중복확인(AJAX)
	boolean existsByTitle(String title);

	// 특정 관리자가 등록한 도서목록
	List<Book> findByAppUserId(Long appUserId);

	// ------------------------------------
	// 📘 등록 / 수정 / 삭제
	// ------------------------------------

	// 등록 - map : { title, author, publisher, publishDate(yyyy-MM-dd), category,
	//               ranking, reviewCount, rating, description, pages, price, bookCover, appUserId }
	int insert(Map<String, Object> map);

	// 수정 - map : 위 항목 + { bookId }  (null 인 항목은 미반영)
	int update(Map<String, Object> map);

	// 삭제
	int delete(Long bookId);
}
