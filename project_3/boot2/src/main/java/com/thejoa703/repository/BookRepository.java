package com.thejoa703.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.Book;

@Repository									// Entity , PK
public interface BookRepository extends JpaRepository<Book, Long> {

	// 도서명으로 검색 (부분일치, 대소문자 무시)
	List<Book> findByTitleContainingIgnoreCase(String title);

	// 저자명으로 검색
	List<Book> findByAuthorContainingIgnoreCase(String author);

	// 출판사로 검색
	List<Book> findByPublisher(String publisher);

	// 카테고리(분류)별 도서목록 조회
	List<Book> findByCategory(String category);

	// 특정 회원(관리자)이 등록한 도서목록  (Book.user.id 로 조회)
	List<Book> findByUser_Id(Long appUserId);

	// 제목 + 카테고리 동시조건 검색
	List<Book> findByTitleContainingIgnoreCaseAndCategory(String title, String category);

	// 단건조회 - 제목으로 정확히 일치하는 도서 (결과 없거나 1개일때 Optional)
	Optional<Book> findByTitle(String title);

	// 리뷰수 많은순 TOP N (베스트셀러) - 카테고리별
	List<Book> findByCategoryOrderByReviewCountDesc(String category);

	// 평점 높은순 정렬
	List<Book> findAllByOrderByRatingDesc();

	// 최신 등록도서 순 (Oracle 페이징 - ROWNUM)
	@Query(
			value = "SELECT * FROM ( " +
					"SELECT b.*, ROWNUM AS rnum " +
					"FROM (SELECT * FROM BOOK ORDER BY REG_DATE DESC) b " +
					") " +
					"WHERE rnum BETWEEN :start AND :end",
			nativeQuery = true
	)
	List<Book> findBooksWithPaging(@Param("start") int start, @Param("end") int end);

	// 특정회원이 찜(위시리스트)한 도서 목록  (Book : AppUser = N : N, wishUsers)
	List<Book> findByWishUsers_Id(Long appUserId);
}

/*
(1) 사용할수 있는 기본 SQL
	1. CREATE : save       - insert into  book ( 컬럼1, 컬럼2,,,)  values (?,?,,,,)
	2. READ   : findAll    - select * from book
				findById   - select * from book where book_id=?
	3. UPDATE : save       - update  테이블명  set  컬럼1=?,,,, where book_id=?
	4. DELETE : deleteById - delete from book where book_id=?

(2) 검색       : findBy필드명
(3) 복잡한 sql : @Query
*/
