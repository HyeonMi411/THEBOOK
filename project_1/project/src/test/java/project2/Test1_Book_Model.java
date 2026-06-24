//package project2;
//
////import java.time.LocalDate;
//import java.util.HashMap;
//import java.util.List;
//
//import javax.sql.DataSource;
//
//import org.apache.ibatis.session.SqlSession;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.ApplicationContext;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//
//import com.the703.dao.BookMapper;
//import com.the703.dto.BookDto;
//import com.the703.service.BookService;
//
//
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations={
//		"classpath:config/root-context.xml"   , 
//		"classpath:config/security-context.xml" 
//})
//public class Test1_Book_Model {
//	@Autowired   ApplicationContext context;
//	@Autowired   DataSource         ds;
//	@Autowired   SqlSession         sqlSession;
//    @Autowired   BookMapper bookMapper;
//    @Autowired   BookService service;
//    
//    ////////////////////////////////// 
//    @Test public void test7() {
//    	BookDto book = new BookDto();
//	    book.setTitle("테스트 책");
//	    book.setAuthor("가길동");
//	    book.setPublisher("테스트출판사");
//	    book.setPublishDate("2020-06-19");
//	    book.setCategory("소설");
//	    book.setRanking("1위");
//	    book.setReviewCount(10);
//	    book.setRating(4.5);
//	    book.setDescription("테스트용 설명입니다.");
//	    book.setPages(300);
//	    book.setPrice(15000);
//	    book.setBookCover("test.jpg");
//	    
//	    System.out.println(service.insert(book));
//		//System.out.println(service.findById(56));
//		//System.out.println(service.findAll(1));
//	}
//
//	@Test 	public void test6() {
//		//2. 최신글 10개씩
//		HashMap<String,Integer> map = new HashMap<>();
//		map.put("start",  0);
//		map.put("end"  ,  10);
//		System.out.println( service.select10(map));
//		
//		//1. 전체갯수 
//		System.out.println( service.selectCnt() ); 
//	
//	}	
//
//	@Ignore @Test 	public void test5() {
//		//삭제
//		BookDto dto = new BookDto();   dto.setBno(4);
//		System.out.println(  service.delete(dto) );
//		//수정
//		//		BoardDto dto = new BoardDto();
//		//		dto.setBname("first");        dto.setBpass("1111"); dto.setBno(4);
//		//		dto.setBtitle("NEW-service-첫번째 글쓰기");  dto.setBcontent("NEW-service-내용");
//		//		System.out.println(  service.edit(dto) ); 
//		
//		//검색
//		System.out.println(service.detail(4)); 
//		//삽입  -  4
//		//		BoardDto dto = new BoardDto();
//		//		dto.setBname("first");        dto.setBpass("1111");
//		//		dto.setBtitle("service-첫번째 글쓰기");  dto.setBcontent("service-내용");
//		//		System.out.println(  service.insert(dto) );
//		
//		//전체리스트
//		//System.out.println(service.selectAll());
//	}
//
//    
//    //////////////////////////////////
////    @Ignore @Test
////    public void testInsert() {
////        BookDto book = new BookDto();
////        book.setTitle("테스트 책");
////        book.setAuthor("가길동");
////        book.setPublisher("테스트출판사");
////        book.setPublishDate("2020-06-19");
////        book.setCategory("소설");
////        book.setRanking("1위");
////        book.setReviewCount(10);
////        book.setRating(4.5);
////        book.setDescription("테스트용 설명입니다.");
////        book.setPages(300);
////        book.setPrice(15000);
////        book.setBookCover("test.jpg");
////
////        int result = bookMapper.insert(book);
////        System.out.println("INSERT 결과: " + result);
////        System.out.println("생성된 bookId: " + book.getBookId());
////    }		// ok  
////
////    @Ignore @Test
////    public void testFindById() {
////        BookDto book = bookMapper.findById(57);
////        System.out.println("조회 결과: " + book);
////    }		// ok 
////
////    @Ignore @Test
////    public void testFindAll() {
////		//2. 최신글 10개씩
////		HashMap<String,Integer> map = new HashMap<>();
////		map.put("start",  0);
////		map.put("end"  ,  10);
////		System.out.println( bookMapper.findAll(map));
////		
////		//1. 전체갯수 
////		System.out.println( bookMapper.findAllCnt() ); 
////    }		// ok
////
////    @Ignore @Test
////    public void testUpdate() {
////        BookDto book = bookMapper.findById(57);
////        if (book != null) {
////            book.setTitle("수정된 제목");
////            int result = bookMapper.update(book);
////            System.out.println("UPDATE 결과: " + result);
////        }
////    }		// ok
////
////    @Ignore @Test
////    public void testDelete() {
////        int result = bookMapper.delete(57);
////        System.out.println("DELETE 결과: " + result);
////    }		// ok
////
////    @Ignore @Test
////    public void testFindByCategory() {
////        bookMapper.findByCategory("소설").forEach(System.out::println);
////    }		// ok
////
////    @Ignore @Test
////    public void testSearchByTitle() {
////        bookMapper.searchByTitle("테스트").forEach(System.out::println);
////    }		// ok
//}
