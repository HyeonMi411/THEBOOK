package com.the703;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import com.the703.dao.BookDao;
import com.the703.dao.TestDao;
import com.the703.dto.BookDto;
import com.the703.service.BookService;

@SpringBootTest
class Boot1ApplicationTests4 {

   @Autowired  TestDao  dao;  
   @Autowired BookDao bookDao;
   @Autowired private BookService bookService;
   // @Ignore - JUnit4
	@Disabled  // @Test 
   void contextLoads() {
      System.out.println("..........................");
      System.out.println( dao.readTime());
      System.out.println("..........................");
   }
	
	////////////////////////////////////////////////////////////
	
    // -------------------------------
    // 1. INSERT 테스트 (Oracle SEQ 사용)
    // -------------------------------
	@Disabled	
    // @Test
    public void testServiceInsert() {
        BookDto book = new BookDto();
        book.setTitle("JUnit5 Oracle 테스트 책");
        book.setAuthor("홍길동");
        book.setPublisher("테스트출판사");
        book.setPublishDate("2024-01-01");   // Oracle: TO_DATE 처리됨
        book.setCategory("소설");
        book.setRanking("1위");
        book.setReviewCount(5);
        book.setRating(4.7);
        book.setDescription("JUnit5 Oracle 테스트 설명입니다.");
        book.setPages(250);
        book.setPrice(18000);
        book.setBookCover("test.jpg");

        MockMultipartFile file = new MockMultipartFile("file" , "test.text", "text/plain" , "data".getBytes());        
        
        int result = bookService.insert(book, file);
        System.out.println("INSERT 결과 = " + result);
        System.out.println("생성된 bookId = " + book.getBookId());
    }		// ok 	
	
    // -------------------------------
    // 2. 페이징 테스트 (ROWNUM BETWEEN)
    // -------------------------------
	@Disabled 	
    //@Test
    public void testPaging() {
        int page = 1;

        List<BookDto> list = bookService.findAll(page);
        int totalCnt = bookService.findAllCnt();

        System.out.println("전체 개수 = " + totalCnt);
        System.out.println("1페이지 리스트 = ");
        list.forEach(System.out::println);
    }		// ok
    
    // -------------------------------
    // 3. 상세 조회 테스트
    // -------------------------------
	@Disabled	
    // @Test
    public void testDetail() {
        BookDto dto = new BookDto();
        dto.setBookId(1);   // 존재하는 PK로 테스트

        BookDto book = bookService.detail(dto);
        System.out.println("상세 조회 결과 = " + book);
    }		// ok   
	
    // -------------------------------
    // 4. UPDATE 테스트
    // -------------------------------
    @Disabled	
    // @Test
    public void testServiceUpdate() {
        BookDto dto = new BookDto();
        dto.setBookId(1);   // 수정할 PK
        
        MockMultipartFile file = new MockMultipartFile("file" , "test.text", "text/plain" , "data".getBytes());

        BookDto book = bookService.detail(dto);
        if (book != null) {
            book.setTitle("JUnit5 수정된 제목");
            int result = bookService.edit(book, file);
            System.out.println("UPDATE 결과 = " + result);
        }
    }	// ok 	
	
    // -------------------------------
    // 5. DELETE 테스트
    // -------------------------------
    @Disabled
    // @Test
    public void testServiceDelete() {
        BookDto dto = new BookDto();
        dto.setBookId(1);   // 삭제할 PK

        int result = bookService.delete(dto);
        System.out.println("DELETE 결과 = " + result);
    }		// ok
    
    // -------------------------------
    // 6. 카테고리 검색 테스트
    // -------------------------------    
	@Disabled    
    // @Test
    public void testServiceFindByCategory() {
        List<BookDto> list = bookService.findByCategory("소설");
        list.forEach(System.out::println);
    }	// ok     
    
    // -------------------------------
    // 7. 제목 검색 테스트
    // -------------------------------
	@Disabled	
    // @Test
    public void testServiceSearchByTitle() {
        List<BookDto> list = bookService.searchByTitle("테스트");
        list.forEach(System.out::println);
    }	// ok     
	
	
	////////////////////////////////////////////////////////////

	@Disabled 
	// @Test
	 public void testInsert() {
	     BookDto book = new BookDto();
	     book.setTitle("테스트 책");
	     book.setAuthor("가길동");
	     book.setPublisher("테스트출판사");
	     book.setPublishDate("2020-06-19");
	     book.setCategory("소설");
	     book.setRanking("1위");
	     book.setReviewCount(10);
	     book.setRating(4.5);
	     book.setDescription("테스트용 설명입니다.");
	     book.setPages(300);
	     book.setPrice(15000);
	     book.setBookCover("test.jpg");
	
	     int result = bookDao.insert(book);
	     //assertEquals(1, result);	// 예상되는 결과 ,  코드
	     System.out.println("INSERT 결과: " + result);
	     System.out.println("생성된 bookId: " + book.getBookId());
	 }	//ok
	
	@Disabled 
	// @Test
  public void testFindById() {
      BookDto dto = new BookDto();
      dto.setBookId(57);   // Oracle은 객체로 전달해야 함
      BookDto book = bookDao.findById(dto);
      System.out.println("조회 결과: " + book);
  }		// ok	

	@Disabled	
	//@Test
  public void testFindAll() {
      HashMap<String,Integer> map = new HashMap<>();
      map.put("start", 1);
      map.put("end", 10);

      System.out.println(bookDao.findAll(map));
      System.out.println(bookDao.findAllCnt());
  }		// ok
	
	@Disabled 
	//@Test
  public void testUpdate() {
      BookDto dto = new BookDto();
      dto.setBookId(57);

      BookDto book = bookDao.findById(dto);
      if (book != null) {
          book.setTitle("수정된 제목");
          int result = bookDao.update(book);
          System.out.println("UPDATE 결과: " + result);
      }
  }		// ok 
	
	@Disabled 
	//@Test
  public void testDelete() {
      int result = bookDao.delete(57);
      System.out.println("DELETE 결과: " + result);
//      AppUserDto   user = new AppUserDto();
//      user.setAppUserId(21);
//      assertEquals(  1, bookDao.deleteAppUser(user));
  }		// ok
	
	@Disabled 
	//@Test
  public void testFindByCategory() {
		bookDao.findByCategory("소설").forEach(System.out::println);
  }		// ok
	
	@Disabled 
	//@Test
  public void testSearchByTitle() {
		bookDao.searchByTitle("테스트").forEach(System.out::println);
  }		// ok 		
   
}
