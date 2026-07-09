package com.the703;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.the703.dao.BookDao;
import com.the703.dto.BookDto;

@SpringBootTest
public class Boot1ApplicationTests3 {
	
    @Autowired private BookDao bookDao; 
	
    // ---------------------------------------------------------
    // 전체 조회 테스트 (페이징)
    // ---------------------------------------------------------
    @Disabled 
    @Test
    void testFindAll() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("start", 0);   // OFFSET
        map.put("end", 10);    // FETCH NEXT

        List<BookDto> list = bookDao.findAll(map);

        System.out.println("=== 전체 조회 테스트 ===");
        list.forEach(System.out::println);
    }

    // ---------------------------------------------------------
    // 전체 개수 조회
    // ---------------------------------------------------------
    @Disabled 
    @Test
    void testFindAllCnt() {
        int cnt = bookDao.findAllCnt();
        System.out.println("총 도서 수 = " + cnt);
    }

    // ---------------------------------------------------------
    // 단건 조회 테스트
    // ---------------------------------------------------------
    @Disabled
    @Test
    void testFindById() {
        BookDto dto = new BookDto();
        dto.setBookId(1);

        BookDto book = bookDao.findById(dto);

        System.out.println("=== 단건 조회 ===");
        System.out.println(book);
    }

    // ---------------------------------------------------------
    // 등록 테스트
    // ---------------------------------------------------------
    @Disabled  @Test
    void testInsert() {
        BookDto dto = new BookDto();
        dto.setTitle("테스트 등록 책");
        dto.setAuthor("테스트 저자");
        dto.setPublisher("테스트 출판사");
        dto.setPublishDate("2026-07-01");
        dto.setCategory("테스트");
        dto.setRanking("테스트 랭킹");
        dto.setReviewCount(0);
        dto.setRating(4.5);
        dto.setDescription("테스트 설명");
        dto.setPages(100);
        dto.setPrice(15000);
        dto.setBookCover("test_cover.jpg");

        int result = bookDao.insert(dto);

        System.out.println("등록 결과 = " + result);
    }

    // ---------------------------------------------------------
    // 수정 테스트 (동적 SQL 적용 버전)
    // ---------------------------------------------------------
    @Disabled
    @Test
    void testUpdate() {
        BookDto dto = new BookDto();
        dto.setBookId(63);
        dto.setTitle("수정된 제목");
        dto.setAuthor("수정된 저자");
        dto.setPublisher("수정된 출판사");
        dto.setPublishDate("2026-07-02");
        dto.setCategory("수정");
        dto.setRanking("수정 랭킹");
        dto.setReviewCount(10);
        dto.setRating(4.8);
        dto.setDescription("수정된 설명");
        dto.setPages(200);
        dto.setPrice(20000);
        dto.setBookCover("updated_cover.jpg");

        int result = bookDao.update(dto);

        System.out.println("수정 결과 = " + result);
    }

    // ---------------------------------------------------------
    // 삭제 테스트
    // ---------------------------------------------------------
    @Disabled
    @Test
    void testDelete() {
        BookDto dto = new BookDto();
        dto.setBookId(63);

        int result = bookDao.delete(dto);

        System.out.println("삭제 결과 = " + result);
    }

    // ---------------------------------------------------------
    // 통합 검색 테스트 (제목/저자/카테고리 통합)
    // ---------------------------------------------------------
    @Disabled
    @Test
    void testSearchBooks() {
        BookDto dto = new BookDto();
        dto.setKeyword("행복");
        dto.setSearchType("title"); // title, author, category 로 변경하며 테스트 가능
        // dto.setOrderBy("rating"); // Mapper 정렬 단일화로 인해 더 이상 필요 없음

        List<BookDto> list = bookDao.searchBooks(dto);

        System.out.println("=== 통합 검색 ===");
        list.forEach(System.out::println);
    }    

    // =========================================================
    // ❌ 아래 메서드들은 BookDao 인터페이스에서 삭제(주석)되어 
    //    컴파일 에러를 방지하기 위해 테스트 코드에서도 주석 처리했습니다.
    // =========================================================
    
    /*
    // 카테고리 검색 테스트
    @Disabled
    @Test
    void testFindByCategory() {
        BookDto dto = new BookDto();
        dto.setCategory("소설");
        List<BookDto> list = bookDao.findByCategory(dto);
        System.out.println("=== 카테고리 검색 ===");
        list.forEach(System.out::println);
    }

    // 제목 검색(AJAX) 테스트
    @Disabled
    @Test
    void testSearchByTitle() {
        BookDto dto = new BookDto();
        dto.setKeyword("행복");
        List<BookDto> list = bookDao.searchByTitle(dto);
        System.out.println("=== 제목 검색(AJAX) ===");
        list.forEach(System.out::println);
    }

    // 도서명 중복검사(AJAX) 테스트
    @Disabled
    @Test
    void testCheckTitleDuplicate() {
        BookDto dto = new BookDto();
        dto.setTitle("행복의 조건");
        int count = bookDao.checkTitleDuplicate(dto);
        System.out.println("=== 도서명 중복검사 ===");
        System.out.println("중복 개수 = " + count);
    }
    */
}