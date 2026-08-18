package com.thejoa703.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> { // Entity , PK

	// ------------------------------------
	// 📘 기본 조회 (JPA 쿼리메서드 - findBy필드명)
	// ------------------------------------

	// 전체조회 (최신순)
	List<Book> findAllByOrderByIdDesc();

	// ★전체조회 - 페이징(Spring Data JPA 표준 Pageable) - 12개씩 화면표시용
	Page<Book> findAllByOrderByIdDesc(Pageable pageable);

	// 카테고리별 조회
	List<Book> findByCategoryOrderByIdDesc(String category);

	// ★카테고리별 조회 - 페이징
	Page<Book> findByCategoryOrderByIdDesc(String category, Pageable pageable);

	// 카테고리별 개수
	long countByCategory(String category);

	// 제목검색(부분일치)  findBy필드명Containing
	List<Book> findByTitleContainingOrderByIdDesc(String keyword);

	// 저자검색(부분일치)
	List<Book> findByAuthorContainingOrderByIdDesc(String keyword);

	// 제목/저자/카테고리 동시 검색(OR)
	List<Book> findByTitleContainingOrAuthorContainingOrCategoryContainingOrderByIdDesc(
			String title, String author, String category);

	// 제목중복확인(AJAX)   existsBy필드명
	boolean existsByTitle(String title);

	// 등록한 관리자기준 조회 (마이페이지 - 내가 등록한 도서)
	List<Book> findByUser_IdOrderByIdDesc(Long userId);

	// ------------------------------------
	// 📘 오라클 네이티브 페이징 (ROWNUM)
	// ------------------------------------

	// 전체조회 - 페이징
	@Query(
			value = "SELECT * FROM ( " +
					"SELECT b.*, ROWNUM AS rnum " +
					"FROM (SELECT * FROM BOOK ORDER BY BOOK_ID DESC) b " +
					") " +
					"WHERE rnum BETWEEN :start AND :end",
			nativeQuery = true
	)
	List<Book> findBooksWithPaging(@Param("start") int start, @Param("end") int end);

	// 카테고리별 조회 - 페이징
	@Query(
			value = "SELECT * FROM ( " +
					"SELECT b.*, ROWNUM AS rnum " +
					"FROM (SELECT * FROM BOOK WHERE CATEGORY = :category ORDER BY BOOK_ID DESC) b " +
					") " +
					"WHERE rnum BETWEEN :start AND :end",
			nativeQuery = true
	)
	List<Book> findBooksByCategoryWithPaging(@Param("category") String category,
	                                          @Param("start") int start, @Param("end") int end);
}
