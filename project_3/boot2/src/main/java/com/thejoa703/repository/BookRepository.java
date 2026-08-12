package com.thejoa703.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> { //Entity , PK

	// ------------------------------------------------------------
	// 📘 기본 CRUD ( JpaRepository 기본 제공 )
	// ------------------------------------------------------------
	// create - save        : insert into  book (컬럼1, 컬럼2,,,)  values (?,?,,,)
	// read   - findAll()   : select * from book
	//          findById(id): select * from book  where book_id=?
	// update - save        : update  book  set  컬럼1=?,,,,  where  book_id=?
	// delete - deleteById  : delete from book  where book_id=?


	// ------------------------------------------------------------
	// ⭐ 도서명 중복검사(AJAX)
	// ------------------------------------------------------------
	boolean existsByTitle(String title);


	// ------------------------------------------------------------
	// 🔎 단일조건 조회 / 집계
	// ------------------------------------------------------------
	// 카테고리별 조회 (페이징없이 전체)
	List<Book> findByCategory(String category);

	// 카테고리별 갯수
	long countByCategory(String category);

	// 제목 검색 (LIKE)
	List<Book> findByTitleContaining(String title);

	// 저자 검색 (LIKE)
	List<Book> findByAuthorContaining(String author);

	// ★특정 관리자(AppUser)가 등록한 도서 목록 - Book.user 필드 참조 (도서등록은 관리자만 가능)
	List<Book> findByUser_Id(Long userId);

	// ★특정 관리자가 등록한 도서 갯수
	long countByUser_Id(Long userId);


	// ------------------------------------------------------------
	// ⭐ 정렬 조회 - BookDto.orderBy(rating, reviewCount 등) 대응
	// ------------------------------------------------------------
	List<Book> findAllByOrderByRegDateDesc();      // 최신등록순(기본)
	List<Book> findAllByOrderByRatingDesc();       // 평점 높은순
	List<Book> findAllByOrderByReviewCountDesc();  // 리뷰많은순
	List<Book> findByCategoryOrderByRatingDesc(String category);


	// ------------------------------------------------------------
	// 📄 도서 목록 페이징 (Oracle ROWNUM)
	// ------------------------------------------------------------
	@Query(
			value = "SELECT * FROM ( " +
	                "SELECT b.*, ROWNUM AS rnum " +
	                "FROM (SELECT * FROM BOOK ORDER BY BOOK_ID DESC) b " +
	               ") " +
	               "WHERE rnum BETWEEN :start AND :end",
			nativeQuery = true
	)
	List<Book> findBooksWithPaging(@Param("start") int start, @Param("end") int end);

	// 📄 카테고리별 도서 목록 페이징
	@Query(
			value = "SELECT * FROM ( " +
	                "SELECT b.*, ROWNUM AS rnum " +
	                "FROM (SELECT * FROM BOOK WHERE CATEGORY LIKE '%' || :category || '%' ORDER BY BOOK_ID DESC) b " +
	               ") " +
	               "WHERE rnum BETWEEN :start AND :end",
			nativeQuery = true
	)
	List<Book> findBooksByCategoryWithPaging(@Param("category") String category,
	                                          @Param("start") int start,
	                                          @Param("end") int end);


	// ------------------------------------------------------------
	// 🔍 통합검색 (제목 + 저자 + 카테고리) - BookDto.searchType / keyword 대응
	// ------------------------------------------------------------
	@Query(value = "SELECT * FROM BOOK " +
                    "WHERE (:searchType = 'title'    AND TITLE    LIKE '%' || :keyword || '%') " +
                    "   OR (:searchType = 'author'   AND AUTHOR   LIKE '%' || :keyword || '%') " +
                    "   OR (:searchType = 'category' AND CATEGORY LIKE '%' || :keyword || '%') " +
                    "ORDER BY BOOK_ID DESC",
	      nativeQuery = true)
	List<Book> searchBooks(@Param("searchType") String searchType, @Param("keyword") String keyword);

	// 🔍 통합검색 + 페이징
	@Query(value = "SELECT * FROM ( " +
                    "SELECT b.*, ROWNUM AS rnum " +
                    "FROM ( " +
                    "   SELECT * FROM BOOK " +
                    "   WHERE (:searchType = 'title'    AND TITLE    LIKE '%' || :keyword || '%') " +
                    "      OR (:searchType = 'author'   AND AUTHOR   LIKE '%' || :keyword || '%') " +
                    "      OR (:searchType = 'category' AND CATEGORY LIKE '%' || :keyword || '%') " +
                    "   ORDER BY BOOK_ID DESC " +
                    ") b " +
                    ") " +
                    "WHERE rnum BETWEEN :start AND :end",
	      nativeQuery = true)
	List<Book> searchBooksWithPaging(@Param("searchType") String searchType,
	                                  @Param("keyword") String keyword,
	                                  @Param("start") int start,
	                                  @Param("end") int end);

	// 검색결과 갯수 (페이징 total count 용)
	@Query(value = "SELECT COUNT(*) FROM BOOK " +
                    "WHERE (:searchType = 'title'    AND TITLE    LIKE '%' || :keyword || '%') " +
                    "   OR (:searchType = 'author'   AND AUTHOR   LIKE '%' || :keyword || '%') " +
                    "   OR (:searchType = 'category' AND CATEGORY LIKE '%' || :keyword || '%')",
	      nativeQuery = true)
	long searchBooksCnt(@Param("searchType") String searchType, @Param("keyword") String keyword);

}
/*
(1) 사용할수 있는 기본 SQL
	1. CREATE : save       - insert into  book ( 컬럼1, 컬럼2,,,)  values (?,?,,,,)
	2. READ   : findAll    - select * from book
	            findById   - select * from book  where book_id=?
	3. UPDATE : save       - update  book  set  컬럼1=?,,,,  where  book_id=?
	4. DELETE : deleteById - delete from book  where book_id=?

(2) 도서등록(관리자only)은 Service 단에서 user.getRole()=="ROLE_ADMIN" 체크 후
    book.setUser(관리자AppUser) 세팅하여 save()
*/
