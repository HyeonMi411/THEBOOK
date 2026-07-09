package com.the703;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import com.the703.dto.BookDto;
import com.the703.service.BookService;

@SpringBootTest
public class Boot1ApplicationTests5 {
	 
    @Autowired private BookService bookService;
	
    // 📘 전체 조회 테스트
    @Disabled    
    @Test
    void testServiceFindAll() {
        List<BookDto> list = bookService.findAll(1);
        System.out.println("=== 전체 조회 테스트 ===");
        list.forEach(System.out::println);
    }

    // 📘 전체 개수 테스트
    @Disabled    
    @Test
    void testServiceFindAllCnt() {
        int cnt = bookService.findAllCnt();
        System.out.println("총 도서 수 = " + cnt);
    }

    // 📘 단건 조회 테스트
    @Disabled    
    @Test
    void testDetail() {
        BookDto dto = new BookDto();
        dto.setBookId(1);

        BookDto book = bookService.detail(dto);
        System.out.println("=== 단건 조회 ===");
        System.out.println(book);
    }

    // 📘 등록 테스트 (파일 포함)
    @Disabled    
    @Test
    void testServiceInsert() {
    	MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

        BookDto dto = new BookDto();
        dto.setTitle("테스트 등록 책" + System.currentTimeMillis()); // 무결성 제약조건(중복) 방지
        dto.setAuthor("테스트 저자2");
        dto.setPublisher("테스트 출판사2");
        dto.setPublishDate("2026-07-02");
        dto.setCategory("테스트2");
        dto.setRanking("테스트 랭킹2");
        dto.setReviewCount(0);
        dto.setRating(4.5);
        dto.setDescription("테스트 설명2");
        dto.setPages(200);
        dto.setPrice(25000);

        int result = bookService.insert(dto, file);
        System.out.println("등록 결과 = " + result);
    }

    // 📘 수정 테스트
    @Disabled    
    @Test
    void testEdit() {
    	MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

        BookDto dto = new BookDto();
        dto.setBookId(60); // 현재 DB에 존재하는 번호로 설정 필수
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

        int result = bookService.edit(dto, file);
        System.out.println("수정 결과 = " + result);
    }

    // 📘 삭제 테스트
    @Disabled    
    @Test
    void testServiceDelete() {
        BookDto dto = new BookDto();
        dto.setBookId(60);

        int result = bookService.delete(dto);
        System.out.println("삭제 결과 = " + result);
    }

    // 🔍 통합 검색(AJAX) 테스트
    @Disabled    
    @Test
    void testServiceSearchBooks() {
        BookDto dto = new BookDto();
        dto.setKeyword("행복");
        dto.setSearchType("title");

        List<BookDto> list = bookService.searchBooks(dto);
        System.out.println("=== 통합 검색(AJAX) ===");
        list.forEach(System.out::println);
    }

    // =========================================================
    // ❌ 미사용 기능 컴파일 에러 방지 주석 처리 파트
    // =========================================================
    /*
    @Disabled    
    @Test
    void testServiceFindByCategory() {
        List<BookDto> list = bookService.findByCategory("소설");
        System.out.println("=== 카테고리 검색 ===");
        list.forEach(System.out::println);
    }

    @Disabled    
    @Test
    void testServiceSearchByTitle() {
        List<BookDto> list = bookService.searchByTitle("행복");
        System.out.println("=== 제목 검색(AJAX) ===");
        list.forEach(System.out::println);
    }

    @Disabled    
    @Test
    void testServiceCheckTitleDuplicate() {
        int count = bookService.checkTitleDuplicate("행복의 조건");
        System.out.println("=== 도서명 중복검사(AJAX) ===");
        System.out.println("중복 개수 = " + count);
    }
    */
}