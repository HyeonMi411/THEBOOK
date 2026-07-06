package com.the703;

import java.util.HashMap;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.the703.dao.BookDao;
import com.the703.dto.BookDto;

@SpringBootTest
class Boot1ApplicationTests3 {
	
//    @Autowired ApplicationContext context;
//    @Autowired DataSource ds;
//    @Autowired SqlSession sqlSession;
    @Autowired BookDao bookDao;
//    @Autowired BookService service;
    
    ////////////////////////////////////////
//	@Autowired   AppUserDao  dao;
//    @Autowired   AppUserService service;
//    // 삭제
//    @Disabled @Test	public void deleteService_User(){
//        AppUserDto   user = new AppUserDto();
//        user.setEmail("2@2");      user.setPassword("2");	user.setAppUserId(61);
//		assertEquals(1, service.delete( user , true));		
//	}
//	
//    // 수정
//	@Disabled @Test	public void updateService_User(){
//        AppUserDto   user = new AppUserDto();
//        user.setEmail("2@2");      user.setPassword("2");   
//        user.setUfile("12.png");    user.setMobile("0102222222");   user.setNickname("2");
//        user.setProvider("local"); user.setProviderId("local_002");	user.setAppUserId(61);
//        
//        MockMultipartFile file = new MockMultipartFile("file" , "test.text", "text/plain" , "data".getBytes());
//                
//        assertEquals(1, service.update(file, user));  	
//    }
//    
//    // 아이디중복
//    @Disabled @Test	public void iddoubleService_User(){
//    	int   mypage = service.iddouble("2@2","local" );
//    	    	
//    	assertEquals(1, mypage);    	
//    }    
//    // 마이페이지
//    @Disabled @Test	public void mypageService_User(){
//    	AppUserDto   mypage = service.selectEmail("2@2","local" );
//    	
//    	assertNotNull(mypage);
//    	assertEquals("2@2", mypage.getEmail());    	
//    }
//    
//    // 로그인
//    @Disabled @Test	public void loginService_User(){
//		AppUserAuthDto   login = service.readAuthByEmail("2@2","local" );
//		
//		assertNotNull(login);
//		assertEquals("2@2", login.getEmail());
//		assertTrue(		login.getAuthList().stream().anyMatch(a -> "ROLE_MEMBER".equals(a.getAuth()))	);
//	}
// 
//    @Disabled @Test    public  void insert_Service_User(){  
//       AppUserDto   user = new AppUserDto();
//       user.setEmail("2@2");      user.setPassword("2");   user.setMbtiTypeId(1);
//       user.setUfile("1.png");    user.setMobile("0101111111");   user.setNickname("2");
//       user.setProvider("local"); user.setProviderId("local_001");
//       
//       MockMultipartFile file = new MockMultipartFile("file" , "test.text", "text/plain" , "data".getBytes());
//       
//       int result = service.insert(file, user);
//       assertEquals(1, result);  // 예상되는 결과 ,  코드
//    } 
	
	////////////////////////////////////////////
//	
//	@Disabled @Test
//    public void testInsert() {
//        BookDto book = new BookDto();
//        book.setTitle("테스트 책");
//        book.setAuthor("가길동");
//        book.setPublisher("테스트출판사");
//        book.setPublishDate("2020-06-19");
//        book.setCategory("소설");
//        book.setRanking("1위");
//        book.setReviewCount(10);
//        book.setRating(4.5);
//        book.setDescription("테스트용 설명입니다.");
//        book.setPages(300);
//        book.setPrice(15000);
//        book.setBookCover("test.jpg");
//
//        int result = bookDao.insert(book);
//        //assertEquals(1, result);	// 예상되는 결과 ,  코드
//        System.out.println("INSERT 결과: " + result);
//        System.out.println("생성된 bookId: " + book.getBookId());
//    }
//
//	@Disabled @Test
//    public void testFindById() {
//        BookDto dto = new BookDto();
//        dto.setBookId(57);   // Oracle은 객체로 전달해야 함
//        BookDto book = bookDao.findById(dto);
//        System.out.println("조회 결과: " + book);
//    }
//
//	@Test
//    public void testFindAll() {
//        HashMap<String,Integer> map = new HashMap<>();
//        map.put("start", 1);
//        map.put("end", 10);
//
//        System.out.println(bookDao.findAll(map));
//        System.out.println(bookDao.findAllCnt());
//    }
//
//	@Disabled @Test
//    public void testUpdate() {
//        BookDto dto = new BookDto();
//        dto.setBookId(57);
//
//        BookDto book = bookDao.findById(dto);
//        if (book != null) {
//            book.setTitle("수정된 제목");
//            int result = bookDao.update(book);
//            System.out.println("UPDATE 결과: " + result);
//        }
//    }
//
//	@Disabled @Test
//    public void testDelete() {
//        int result = bookDao.delete(57);
//        System.out.println("DELETE 결과: " + result);
////        AppUserDto   user = new AppUserDto();
////        user.setAppUserId(21);
////        assertEquals(  1, bookDao.deleteAppUser(user));
//    }
//	
//	@Disabled @Test
//    public void testFindByCategory() {
//		bookDao.findByCategory("소설").forEach(System.out::println);
//    }
//
//	@Disabled @Test
//    public void testSearchByTitle() {
//		bookDao.searchByTitle("테스트").forEach(System.out::println);
//    }	

}


