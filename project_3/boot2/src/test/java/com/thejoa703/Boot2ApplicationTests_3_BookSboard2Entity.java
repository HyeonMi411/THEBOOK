package com.thejoa703;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Book;
import com.thejoa703.entity.Sboard2;
import com.thejoa703.mapper.BookMapper;
import com.thejoa703.mapper.Sboard2Mapper;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.BookRepository;
import com.thejoa703.repository.Sboard2Repository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional   // org.springframework.transaction.annotation.Transactional
class Boot2ApplicationTests_3_BookSboard2Entity {

	@Autowired private AppUserRepository appUserRepository;
	@Autowired private BookRepository    bookRepository;
	@Autowired private Sboard2Repository sboard2Repository;
	@Autowired private BookMapper        bookMapper;
	@Autowired private Sboard2Mapper     sboard2Mapper;
	@Autowired private EntityManager     entityManager;   // 벌크성 쿼리(increaseHit 등) 이후 1차캐시 초기화용

	// 테스트공통데이터 : 관리자 1명 (도서등록/공지작성은 관리자만 가능)
	private AppUser admin;

	@BeforeEach
	void setup() {   // import java.util.UUID
		// 관리자 계정 생성
		String adminEmail = "admin_" + UUID.randomUUID() + "@test.com";

		admin = new AppUser();
		admin.setEmail(adminEmail);
		admin.setPassword("admin123");
		admin.setNickname("admin");
		admin.setProvider("local");
		admin.setRole("ROLE_ADMIN");   // ★관리자권한
		admin.setDeleted(false);

		appUserRepository.save(admin);

		// ★MyBatis(BookMapper/Sboard2Mapper)는 Hibernate 1차캐시를 거치지 않고
		//   같은 커넥션에 바로 INSERT(raw SQL)를 날리기 때문에, admin row가
		//   실제로 DB에 반영(flush)되어 있어야 APP_USER_ID FK 제약을 통과한다.
		entityManager.flush();
	}

	// 테스트용 Book 생성 헬퍼
	private Book createBook(String title, String author, String category,
	                         Integer reviewCount, Double rating) {
		Book book = new Book();
		book.setTitle(title);
		book.setAuthor(author);
		book.setPublisher("테스트출판사");
		book.setPublishDate(LocalDate.now().minusDays(1));
		book.setCategory(category);
		book.setReviewCount(reviewCount);
		book.setRating(rating);
		book.setDescription("설명 - " + title);
		book.setPages(300);
		book.setPrice(15000);
		book.setBookCover("default.png");
		book.setUser(admin);   // ★도서등록은 관리자만
		return book;
	}

	// 테스트용 Sboard2(공지사항) 생성 헬퍼
	private Sboard2 createNotice(String btitle, String bcontent) {
		Sboard2 board = new Sboard2();
		board.setUser(admin);   // ★공지작성은 관리자만
		board.setBtitle(btitle);
		board.setBcontent(bcontent);
		board.setBpass("1234");
		board.setBip("127.0.0.1");
		return board;
	}


	//=====================================================================
	// 1. BookRepository (JPA) - 기본 CRUD + 중복검사 + 관리자연관
	//=====================================================================
	@Test
	@DisplayName("■ BookRepository - 기본CRUD / 도서명중복검사 / 관리자연관")
	void testBookRepository_CRUD() {

		// 등록 (관리자 연결)
		Book book = createBook("자바의 정석", "남궁성", "IT", 10, 4.5);
		bookRepository.save(book);
		assertThat(book.getId()).isNotNull();
		assertThat(book.getRegDate()).isNotNull();     // @PrePersist 자동세팅

		// 단건조회 + 등록한 관리자 확인
		Book found = bookRepository.findById(book.getId()).orElseThrow();
		assertThat(found.getTitle()).isEqualTo("자바의 정석");
		assertThat(found.getUser().getId()).isEqualTo(admin.getId());
		assertThat(found.getUser().getRole()).isEqualTo("ROLE_ADMIN");

		// 도서명 중복검사(AJAX)
		assertThat(bookRepository.existsByTitle("자바의 정석")).isTrue();
		assertThat(bookRepository.existsByTitle("존재하지않는도서명_XYZ")).isFalse();

		// 수정
		found.setPrice(19000);
		found.setRating(4.8);
		bookRepository.save(found);
		assertThat(bookRepository.findById(book.getId()).orElseThrow().getPrice()).isEqualTo(19000);

		// 삭제 후 재조회 불가 확인
		bookRepository.delete(found);
		assertThat(bookRepository.findById(book.getId())).isEmpty();
	}

	@Test
	@DisplayName("■ BookRepository - 카테고리 조회/집계 + 관리자별 등록도서 조회/집계")
	void testBookRepository_CategoryAndUser() {

		bookRepository.save(createBook("이펙티브 자바", "조슈아 블로크", "IT", 20, 4.9));
		bookRepository.save(createBook("클린 코드", "로버트 마틴", "IT", 15, 4.7));
		bookRepository.save(createBook("데미안", "헤르만 헤세", "소설", 5, 4.2));

		// 카테고리별 조회 / 집계
		List<Book> itBooks = bookRepository.findByCategory("IT");
		assertThat(itBooks).hasSize(2);
		assertThat(bookRepository.countByCategory("IT")).isEqualTo(2L);
		assertThat(bookRepository.countByCategory("소설")).isEqualTo(1L);

		// 제목/저자 LIKE 검색
		assertThat(bookRepository.findByTitleContaining("자바")).hasSize(1);
		assertThat(bookRepository.findByAuthorContaining("마틴")).hasSize(1);

		// ★관리자(admin)가 등록한 도서 목록/갯수
		List<Book> myBooks = bookRepository.findByUser_Id(admin.getId());
		assertThat(myBooks).hasSize(3);
		assertThat(bookRepository.countByUser_Id(admin.getId())).isEqualTo(3L);
	}

	@Test
	@DisplayName("■ BookRepository - 정렬조회(rating/reviewCount) + ROWNUM 페이징")
	void testBookRepository_OrderAndPaging() {

		bookRepository.save(createBook("도서A", "저자A", "IT", 30, 3.0));
		bookRepository.save(createBook("도서B", "저자B", "IT", 10, 4.9));
		bookRepository.save(createBook("도서C", "저자C", "소설", 50, 4.0));

		// 평점 높은순 정렬 - 1등은 평점 4.9
		List<Book> byRating = bookRepository.findAllByOrderByRatingDesc();
		assertThat(byRating.get(0).getRating()).isEqualTo(4.9);

		// 리뷰수 많은순 정렬 - 1등은 리뷰 50
		List<Book> byReviewCount = bookRepository.findAllByOrderByReviewCountDesc();
		assertThat(byReviewCount.get(0).getReviewCount()).isEqualTo(50);

		// 카테고리별 평점순
		List<Book> itByRating = bookRepository.findByCategoryOrderByRatingDesc("IT");
		assertThat(itByRating).hasSize(2);
		assertThat(itByRating.get(0).getRating()).isEqualTo(4.9);

		// 전체 목록 페이징 (1~3번째 rownum)
		List<Book> paged = bookRepository.findBooksWithPaging(1, 3);
		assertThat(paged).hasSize(3);

		// 카테고리별 페이징
		List<Book> itPaged = bookRepository.findBooksByCategoryWithPaging("IT", 1, 10);
		assertThat(itPaged).hasSize(2);
	}

	@Test
	@DisplayName("■ BookRepository - 통합검색(제목/저자/카테고리) + 페이징 + 카운트")
	void testBookRepository_Search() {

		bookRepository.save(createBook("스프링 부트 실전", "김영한", "IT", 40, 4.6));
		bookRepository.save(createBook("토비의 스프링", "이일민", "IT", 35, 4.8));
		bookRepository.save(createBook("어린왕자", "생텍쥐페리", "소설", 8, 4.4));

		// 제목검색
		assertThat(bookRepository.searchBooks("title", "스프링")).hasSize(2);

		// 저자검색
		assertThat(bookRepository.searchBooks("author", "김영한")).hasSize(1);

		// 카테고리검색
		assertThat(bookRepository.searchBooks("category", "소설")).hasSize(1);

		// 검색 + 페이징
		List<Book> paged = bookRepository.searchBooksWithPaging("title", "스프링", 1, 10);
		assertThat(paged).hasSize(2);

		// 검색결과 카운트
		assertThat(bookRepository.searchBooksCnt("title", "스프링")).isEqualTo(2L);
	}


	//=====================================================================
	// 2. Sboard2Repository (JPA) - 공지사항 기본 CRUD + 비밀번호검증 + 관리자연관
	//=====================================================================
	@Test
	@DisplayName("■ Sboard2Repository - 기본CRUD / 비밀번호검증 / 관리자연관")
	void testSboard2Repository_CRUD() {

		// 등록 (관리자 연결)
		Sboard2 board = createNotice("점검 안내", "서버점검 안내드립니다.");
		sboard2Repository.save(board);
		assertThat(board.getId()).isNotNull();
		assertThat(board.getCreatedAt()).isNotNull();   // @PrePersist 자동세팅
		assertThat(board.getBhit()).isEqualTo(0);

		// 단건조회 + 작성한 관리자 확인
		Sboard2 found = sboard2Repository.findById(board.getId()).orElseThrow();
		assertThat(found.getBtitle()).isEqualTo("점검 안내");
		assertThat(found.getUser().getId()).isEqualTo(admin.getId());
		assertThat(found.getUser().getRole()).isEqualTo("ROLE_ADMIN");

		// 비밀번호 검증
		assertThat(sboard2Repository.existsByIdAndBpass(board.getId(), "1234")).isTrue();
		assertThat(sboard2Repository.existsByIdAndBpass(board.getId(), "wrong")).isFalse();
		Optional<Sboard2> byPass = sboard2Repository.findByIdAndBpass(board.getId(), "1234");
		assertThat(byPass).isPresent();

		// 수정
		found.setBcontent("점검이 연장되었습니다.");
		sboard2Repository.save(found);
		assertThat(sboard2Repository.findById(board.getId()).orElseThrow().getBcontent())
				.isEqualTo("점검이 연장되었습니다.");

		// 삭제 후 재조회 불가 확인
		sboard2Repository.delete(found);
		assertThat(sboard2Repository.findById(board.getId())).isEmpty();
	}

	@Test
	@DisplayName("■ Sboard2Repository - 관리자별 작성글 / 제목검색 / ROWNUM페이징 / 조회수증가")
	void testSboard2Repository_UserSearchHit() {

		sboard2Repository.save(createNotice("공지1 - 이용안내", "내용1"));
		sboard2Repository.save(createNotice("공지2 - 이용안내", "내용2"));
		Sboard2 board3 = createNotice("이벤트 안내", "내용3");
		sboard2Repository.save(board3);

		// ★관리자(admin)가 작성한 공지글 목록/갯수
		assertThat(sboard2Repository.findByUser_Id(admin.getId())).hasSize(3);
		assertThat(sboard2Repository.countByUser_Id(admin.getId())).isEqualTo(3L);

		// 제목검색
		List<Sboard2> searched = sboard2Repository.findByBtitleContaining("이용안내");
		assertThat(searched).hasSize(2);
		assertThat(sboard2Repository.countByBtitleContaining("이용안내")).isEqualTo(2L);

		// 목록 페이징
		List<Sboard2> paged = sboard2Repository.findNoticesWithPaging(1, 3);
		assertThat(paged).hasSize(3);

		// 검색 + 페이징
		List<Sboard2> searchPaged = sboard2Repository.searchNoticesWithPaging("이용안내", 1, 10);
		assertThat(searchPaged).hasSize(2);

		// 조회수 증가 (@Modifying 벌크쿼리 → flush/clear 후 재조회해야 반영값 확인가능)
		sboard2Repository.increaseHit(board3.getId());
		entityManager.flush();
		entityManager.clear();
		Sboard2 hitChecked = sboard2Repository.findById(board3.getId()).orElseThrow();
		assertThat(hitChecked.getBhit()).isEqualTo(1);
	}


	//=====================================================================
	// 3. BookMapper (MyBatis) - 복잡한 SQL 처리(페이징/검색/정렬/중복검사)
	//=====================================================================
	@Test
	@DisplayName("■ BookMapper - CRUD / 카테고리 / 검색 / 중복검사 / 관리자별조회")
	void testBookMapper_CRUD() {

		// 등록 (selectKey 로 BOOK_SEQ.NEXTVAL 채번)
		Book book = createBook("모던 자바 인 액션", "라울-게이브리얼", "IT", 12, 4.5);
		int insertCnt = bookMapper.insert(book);
		assertThat(insertCnt).isEqualTo(1);
		assertThat(book.getId()).isNotNull();

		// 단건조회
		Book found = bookMapper.findById(book.getId());
		assertThat(found).isNotNull();
		assertThat(found.getTitle()).isEqualTo("모던 자바 인 액션");
		assertThat(found.getUser().getId()).isEqualTo(admin.getId());   // 등록한 관리자(FK)

		// 목록조회 (페이징)
		Map<String, Object> pageMap = new HashMap<>();
		pageMap.put("start", 0);
		pageMap.put("end", 10);
		List<Book> list = bookMapper.findAll(pageMap);
		assertThat(list).isNotEmpty();

		// 전체 갯수
		assertThat(bookMapper.findAllCnt()).isGreaterThanOrEqualTo(1);

		// 카테고리별 조회/집계
		Map<String, Object> catMap = new HashMap<>();
		catMap.put("category", "IT");
		catMap.put("start", 0);
		catMap.put("end", 10);
		assertThat(bookMapper.findByCategory(catMap)).hasSize(1);
		assertThat(bookMapper.findCategoryCnt("IT")).isEqualTo(1);

		// 통합검색 (제목 + 정렬)
		Map<String, Object> searchMap = new HashMap<>();
		searchMap.put("searchType", "title");
		searchMap.put("keyword", "모던");
		searchMap.put("orderBy", "rating");
		List<Book> searched = bookMapper.searchBooks(searchMap);
		assertThat(searched).hasSize(1);
		assertThat(bookMapper.searchBooksCnt(searchMap)).isEqualTo(1);

		// 도서명 중복검사
		assertThat(bookMapper.countByTitle("모던 자바 인 액션")).isEqualTo(1);
		assertThat(bookMapper.countByTitle("존재하지않는제목_XYZ")).isEqualTo(0);

		// ★관리자별 등록도서 조회
		assertThat(bookMapper.findByUserId(admin.getId())).isNotEmpty();

		// 수정
		found.setPrice(23000);
		int updateCnt = bookMapper.update(found);
		assertThat(updateCnt).isEqualTo(1);
		assertThat(bookMapper.findById(book.getId()).getPrice()).isEqualTo(23000);

		// 삭제
		int deleteCnt = bookMapper.delete(book.getId());
		assertThat(deleteCnt).isEqualTo(1);
		assertThat(bookMapper.findById(book.getId())).isNull();
	}


	//=====================================================================
	// 4. Sboard2Mapper (MyBatis) - 복잡한 SQL 처리(페이징/검색/조회수/비밀번호검증)
	//=====================================================================
	@Test
	@DisplayName("■ Sboard2Mapper - CRUD / 조회수 / 검색 / 비밀번호검증 / 관리자별조회")
	void testSboard2Mapper_CRUD() {

		// 등록 (selectKey 로 SBOARD2_SEQ.NEXTVAL 채번)
		Sboard2 board = createNotice("[Mapper]시스템 점검", "점검내용입니다.");
		int insertCnt = sboard2Mapper.insert(board);
		assertThat(insertCnt).isEqualTo(1);
		assertThat(board.getId()).isNotNull();

		// 단건조회
		Sboard2 found = sboard2Mapper.findById(board.getId());
		assertThat(found).isNotNull();
		assertThat(found.getBtitle()).isEqualTo("[Mapper]시스템 점검");
		assertThat(found.getUser().getId()).isEqualTo(admin.getId());   // 작성한 관리자(FK)
		assertThat(found.getBhit()).isEqualTo(0);

		// 목록조회 (페이징) / 전체갯수
		Map<String, Object> pageMap = new HashMap<>();
		pageMap.put("start", 0);
		pageMap.put("end", 10);
		assertThat(sboard2Mapper.findAll(pageMap)).isNotEmpty();
		assertThat(sboard2Mapper.findAllCnt()).isGreaterThanOrEqualTo(1);

		// 조회수 증가
		int hitCnt = sboard2Mapper.updateHit(board.getId());
		assertThat(hitCnt).isEqualTo(1);
		assertThat(sboard2Mapper.findById(board.getId()).getBhit()).isEqualTo(1);

		// 제목검색
		Map<String, Object> searchMap = new HashMap<>();
		searchMap.put("keyword", "점검");
		searchMap.put("start", 0);
		searchMap.put("end", 10);
		assertThat(sboard2Mapper.searchByTitle(searchMap)).hasSize(1);
		assertThat(sboard2Mapper.searchByTitleCnt("점검")).isEqualTo(1);

		// 비밀번호 검증
		Map<String, Object> passMap = new HashMap<>();
		passMap.put("id", board.getId());
		passMap.put("bpass", "1234");
		assertThat(sboard2Mapper.checkPassword(passMap)).isEqualTo(1);

		Map<String, Object> wrongPassMap = new HashMap<>();
		wrongPassMap.put("id", board.getId());
		wrongPassMap.put("bpass", "wrong");
		assertThat(sboard2Mapper.checkPassword(wrongPassMap)).isEqualTo(0);

		// ★관리자별 작성글 조회
		assertThat(sboard2Mapper.findByUserId(admin.getId())).isNotEmpty();

		// 수정
		found.setBcontent("점검이 완료되었습니다.");
		int updateCnt = sboard2Mapper.update(found);
		assertThat(updateCnt).isEqualTo(1);
		assertThat(sboard2Mapper.findById(board.getId()).getBcontent()).isEqualTo("점검이 완료되었습니다.");

		// 삭제
		int deleteCnt = sboard2Mapper.delete(board.getId());
		assertThat(deleteCnt).isEqualTo(1);
		assertThat(sboard2Mapper.findById(board.getId())).isNull();
	}

}
