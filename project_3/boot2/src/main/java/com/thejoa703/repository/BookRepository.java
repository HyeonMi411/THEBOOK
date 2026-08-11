package com.thejoa703.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {   // Entity , PK

	Optional<Book> findByTitle(String title);
	List<Book>      findByCategory(String category);
	List<Book>      findByTitleContaining(String keyword);

	// ManyToMany(likedByUsers) 컬렉션까지 함께 조회 (fetch join)
	@Query("SELECT b FROM Book b JOIN FETCH b.likedByUsers WHERE b.bookId = :bookId")
	Optional<Book> findByIdWithLikedUsers(@Param("bookId") Long bookId);

	// 카테고리별 도서목록 - Oracle 페이징 (boot2 PostRepository.findPostsWithPaging 참고)
	@Query(
		value = "SELECT * FROM ( " +
				"SELECT b.*, ROWNUM AS rnum " +
				"FROM (SELECT * FROM BOOK WHERE CATEGORY = :category ORDER BY BOOK_ID DESC) b " +
				") " +
				"WHERE rnum BETWEEN :start AND :end",
		nativeQuery = true
	)
	List<Book> findByCategoryWithPaging(@Param("category") String category,
										 @Param("start") int start, @Param("end") int end);

	// 제목/저자 통합검색 - Oracle 페이징
	@Query(
		value = "SELECT * FROM ( " +
				"SELECT b.*, ROWNUM AS rnum " +
				"FROM (SELECT * FROM BOOK " +
				"      WHERE TITLE LIKE '%' || :keyword || '%' " +
				"         OR AUTHOR LIKE '%' || :keyword || '%' " +
				"      ORDER BY BOOK_ID DESC) b " +
				") " +
				"WHERE rnum BETWEEN :start AND :end",
		nativeQuery = true
	)
	List<Book> searchBooksWithPaging(@Param("keyword") String keyword,
									  @Param("start") int start, @Param("end") int end);

	// 찜(좋아요) 많이 받은 책 랭킹 TOP N (BOOK_LIKE 조인테이블 집계)
	@Query(
		value = "SELECT * FROM ( " +
				"  SELECT                 b.BOOK_ID,\n"
				+ "        b.TITLE,\n"
				+ "        b.AUTHOR,\n"
				+ "        b.PUBLISHER,\n"
				+ "        b.PUBLISH_DATE,\n"
				+ "        b.CATEGORY,\n"
				+ "        b.RANKING,\n"
				+ "        b.REVIEW_COUNT,\n"
				+ "        b.RATING,\n"
				+ "        b.DESCRIPTION,\n"
				+ "        b.PAGES,\n"
				+ "        b.PRICE,\n"
				+ "        b.REG_DATE,\n"
				+ "        b.BOOK_COVER, COUNT(bl.APP_USER_ID) AS LIKE_CNT " +
				"  FROM BOOK b LEFT JOIN BOOK_LIKE bl ON b.BOOK_ID = bl.BOOK_ID " +
				"  GROUP BY b.BOOK_ID, b.TITLE, b.AUTHOR, b.PUBLISHER, b.PUBLISH_DATE, b.CATEGORY, " +
				"           b.RANKING, b.REVIEW_COUNT, b.RATING, b.PAGES, b.PRICE, " +	// b.DESCRIPTION
				"           b.REG_DATE  " +		// b.BOOK_COVER
				"  ORDER BY LIKE_CNT DESC " +
				") WHERE ROWNUM <= :topN",
		nativeQuery = true
	)
	List<Book> findTopLikedBooks(@Param("topN") int topN);
}
/*
 (1) 사용할수 있는 기본 SQL
	1. CREATE : save       - insert into  book ( 컬럼1, 컬럼2,,,)  values (?,?,,,,)
	2. READ   : findAll    - select * from book
	            findById   - select * from book  where book_id=?
	3. UPDATE : save       - update 테이블명  set  컬럼1=?,,,,  where  book_id=?
	4. DELETE : deleteById - delete from book  where book_id=?

(2) 카테고리/키워드로 찾기  findBy필드명
(3) 복잡한 sql(페이징, 집계) - @Query(nativeQuery = true)
*/
