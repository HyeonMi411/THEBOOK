package com.thejoa703;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Book;
import com.thejoa703.entity.Sboard2;
import com.thejoa703.mapper.BookMapper;
import com.thejoa703.mapper.Sboard2Mapper;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.BookRepository;
import com.thejoa703.repository.Sboard2Repository;

/**
 * Book / Sboard2  Entity - Repository - Mapper  전용 테스트
 * ------------------------------------------------------------------------------
 * - Boot2ApplicationTests_1_Entity 를 참고하되, @BeforeEach 로 공통데이터를 미리 만들지 않고
 *   각 테스트메서드 안에서 필요한 데이터를 그때그때 직접 생성합니다.
 * - 클래스에 @Transactional 을 걸어 자동롤백시키는 대신, 매 테스트가 끝난 뒤
 *   @AfterEach 에서 실제 커밋된 데이터를 지워서 초기화합니다.
 *   (JPA 쓰기 + MyBatis 쓰기를 모두 "실제 커밋된 상태" 기준으로 검증하기 위함)
 * - Service/Security(@PreAuthorize) 검증은 포함하지 않고, Entity/Repository/Mapper 만 검증합니다.
 * ------------------------------------------------------------------------------
 */
@SpringBootTest
class Boot2ApplicationTests_3_BookSboard2Entity {

	@Autowired private AppUserRepository appUserRepository;
	@Autowired private BookRepository    bookRepository;
	@Autowired private Sboard2Repository sboard2Repository;
	@Autowired private BookMapper        bookMapper;
	@Autowired private Sboard2Mapper     sboard2Mapper;

	// ------------------------------------------------------------------
	// 공통 헬퍼 : 도서/공지 등록시 필요한 관리자(AppUser) 생성
	// ------------------------------------------------------------------
	private AppUser createAdmin(String provider) {
		AppUser admin = new AppUser();
		admin.setEmail("admin_" + UUID.randomUUID() + "@test.com");
		admin.setPassword("encoded-pass");
		admin.setNickname("admin_" + UUID.randomUUID().toString().substring(0, 8));
		admin.setRole("ROLE_ADMIN");
		admin.setProvider(provider);                                   // local / google / kakao / naver
		admin.setProviderId(provider.equals("local") ? "local" : "social-" + UUID.randomUUID());
		return appUserRepository.save(admin);
	}

	// ------------------------------------------------------------------
	// @AfterEach : 매 테스트 종료 후 DB 초기화 (자식(Book/Sboard2) → 부모(AppUser) 순서)
	// ------------------------------------------------------------------
	@AfterEach
	void tearDown() {
		bookRepository.deleteAll();
		sboard2Repository.deleteAll();
		appUserRepository.deleteAll();
	}

	//-------------------------------------------------------------------
	// 1. BookRepository (JPA) - CRUD / 검색 / 오라클 네이티브 페이징
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ BookRepository - CRUD / 검색 / 오라클 네이티브 페이징")
	void testBookRepository() {
		AppUser admin = createAdmin("local");

		Book book = new Book();
		book.setTitle("스프링부트 완전정복");
		book.setAuthor("김코딩");
		book.setPublisher("코딩출판사");
		book.setPublishDate(LocalDate.of(2024, 1, 1));
		book.setCategory("IT");
		book.setDescription("아주 긴 도서설명입니다.".repeat(50)); // @Lob(CLOB) 대용량 문자열 확인
		book.setPrice(20000);
		book.setUser(admin);
		bookRepository.save(book);

		// ★대소문자 검증용 - 한글은 대소문자 구분 자체가 없으므로, 영문이 섞인 도서를 하나
		//   더 만들어서 IgnoreCase 가 실제로 작동하는지 확인합니다.
		Book bookEn = new Book();
		bookEn.setTitle("Modern JavaScript Deep Dive");
		bookEn.setAuthor("Kim Coding");
		bookEn.setPublisher("코딩출판사");
		bookEn.setPublishDate(LocalDate.of(2024, 1, 1));
		bookEn.setCategory("IT");
		bookEn.setDescription("영문 제목/저자 대소문자 검색 테스트용 도서입니다.");
		bookEn.setPrice(25000);
		bookEn.setUser(admin);
		bookRepository.save(bookEn);

		// 단건조회 + CLOB 저장확인
		assertThat(bookRepository.findById(book.getId())).isPresent();
		assertThat(bookRepository.findById(book.getId()).get().getDescription().length())
				.isGreaterThan(100);

		// 전체조회(최신순)
		assertThat(bookRepository.findAllByOrderByIdDesc())
				.extracting(Book::getId).contains(book.getId());

		// 카테고리조회 + 카운트
		assertThat(bookRepository.findByCategoryOrderByIdDesc("IT")).isNotEmpty();
		assertThat(bookRepository.countByCategory("IT")).isEqualTo(1L);

		// 제목/저자 검색 (대소문자 구분없음)
		assertThat(bookRepository.findByTitleContainingIgnoreCaseOrderByIdDesc("스프링")).isNotEmpty();
		assertThat(bookRepository.findByAuthorContainingIgnoreCaseOrderByIdDesc("김코딩")).isNotEmpty();

		// ★대소문자 무시 검증 - 소문자/대문자/섞어써도 전부 찾아져야 함
		assertThat(bookRepository.findByTitleContainingIgnoreCaseOrderByIdDesc("modern"))
				.extracting(Book::getId).contains(bookEn.getId());
		assertThat(bookRepository.findByTitleContainingIgnoreCaseOrderByIdDesc("JAVASCRIPT"))
				.extracting(Book::getId).contains(bookEn.getId());
		assertThat(bookRepository.findByTitleContainingIgnoreCaseOrderByIdDesc("dEeP dIvE"))
				.extracting(Book::getId).contains(bookEn.getId());
		assertThat(bookRepository.findByAuthorContainingIgnoreCaseOrderByIdDesc("kim"))
				.extracting(Book::getId).contains(bookEn.getId());

		// 제목/저자/카테고리 동시검색(OR, 대소문자 구분없음) - 셋 중 하나라도 일치하면 조회
		assertThat(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrderByIdDesc(
				"매칭안됨", "매칭안됨", "IT")).extracting(Book::getId).contains(book.getId());
		assertThat(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrderByIdDesc(
				"매칭안됨", "매칭안됨", "매칭안됨")).isEmpty();

		// 제목중복확인
		assertThat(bookRepository.existsByTitle("스프링부트 완전정복")).isTrue();
		assertThat(bookRepository.existsByTitle("존재하지않는제목입니다")).isFalse();

		// 등록한 관리자기준 조회 ( ★book, bookEn 2권 등록했으므로 2건 )
		assertThat(bookRepository.findByUser_IdOrderByIdDesc(admin.getId())).hasSize(2);

		// 오라클 네이티브 페이징(ROWNUM)
		assertThat(bookRepository.findBooksWithPaging(1, 10))
				.extracting(Book::getId).contains(book.getId());
		assertThat(bookRepository.findBooksByCategoryWithPaging("IT", 1, 10))
				.extracting(Book::getId).contains(book.getId());

		// 수정(더티체킹)
		book.setPrice(30000);
		bookRepository.save(book);
		assertThat(bookRepository.findById(book.getId()).get().getPrice()).isEqualTo(30000);

		// 삭제
		bookRepository.delete(book);
		assertThat(bookRepository.findById(book.getId())).isEmpty();
	}

	//-------------------------------------------------------------------
	// 2. Sboard2Repository (JPA) - CRUD / 조회수증가 / 오라클 네이티브 페이징
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ Sboard2Repository - CRUD / 조회수증가 / 오라클 네이티브 페이징")
	void testSboard2Repository() {
		AppUser admin = createAdmin("kakao"); // 소셜로그인(카카오) 관리자로 등록해도 문제없는지 확인

		Sboard2 board = new Sboard2();
		board.setBtitle("긴급 공지사항");
		board.setBcontent("공지사항 본문내용입니다.".repeat(100)); // @Lob(CLOB) 대용량 문자열 확인
		board.setBip("127.0.0.1");
		board.setUser(admin);
		sboard2Repository.save(board);

		// ★대소문자 검증용 - 영문 제목의 공지사항을 하나 더 만듭니다.
		Sboard2 boardEn = new Sboard2();
		boardEn.setBtitle("System Maintenance Notice");
		boardEn.setBcontent("영문 제목 대소문자 검색 테스트용 공지사항입니다.");
		boardEn.setBip("127.0.0.1");
		boardEn.setUser(admin);
		sboard2Repository.save(boardEn);

		// 단건조회 + CLOB 저장확인
		assertThat(sboard2Repository.findById(board.getId())).isPresent();
		assertThat(sboard2Repository.findById(board.getId()).get().getBcontent().length())
				.isGreaterThan(100);

		// 전체조회(최신순)
		assertThat(sboard2Repository.findAllByOrderByIdDesc())
				.extracting(Sboard2::getId).contains(board.getId());

		// 제목검색 (대소문자 구분없음)
		assertThat(sboard2Repository.findByBtitleContainingIgnoreCaseOrderByIdDesc("긴급")).isNotEmpty();

		// ★대소문자 무시 검증 - 소문자/대문자 섞어써도 찾아져야 함
		assertThat(sboard2Repository.findByBtitleContainingIgnoreCaseOrderByIdDesc("maintenance"))
				.extracting(Sboard2::getId).contains(boardEn.getId());
		assertThat(sboard2Repository.findByBtitleContainingIgnoreCaseOrderByIdDesc("SYSTEM"))
				.extracting(Sboard2::getId).contains(boardEn.getId());

		// 제목중복확인
		assertThat(sboard2Repository.existsByBtitle("긴급 공지사항")).isTrue();

		// 작성한 관리자기준 조회 ( ★board, boardEn 2건 작성했으므로 2건 )
		assertThat(sboard2Repository.findByUser_IdOrderByIdDesc(admin.getId())).hasSize(2);

		// 조회수 증가 (@Modifying)
		assertThat(board.getBhit()).isEqualTo(0);
		sboard2Repository.increaseHit(board.getId());
		assertThat(sboard2Repository.findById(board.getId()).get().getBhit()).isEqualTo(1);

		// 오라클 네이티브 페이징(ROWNUM)
		assertThat(sboard2Repository.findNoticesWithPaging(1, 10))
				.extracting(Sboard2::getId).contains(board.getId());

		// 수정(더티체킹)
		Sboard2 target = sboard2Repository.findById(board.getId()).get();
		target.setBtitle("수정된 공지사항");
		sboard2Repository.save(target);
		assertThat(sboard2Repository.findById(board.getId()).get().getBtitle())
				.isEqualTo("수정된 공지사항");

		// 삭제
		sboard2Repository.delete(target);
		assertThat(sboard2Repository.findById(board.getId())).isEmpty();
	}

	//-------------------------------------------------------------------
	// 3. BookMapper (MyBatis) - CRUD / 검색 / 페이징
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ BookMapper - CRUD / 검색 / 페이징 (MyBatis)")
	void testBookMapper() {
		AppUser admin = createAdmin("local");

		// 등록
		Map<String, Object> insertMap = new HashMap<>();
		insertMap.put("title", "마이바티스 도서");
		insertMap.put("author", "이바티스");
		insertMap.put("publisher", "마이출판사");
		insertMap.put("publishDate", LocalDate.of(2023, 5, 1));
		insertMap.put("category", "프로그래밍");
		insertMap.put("ranking", "TOP10");
		insertMap.put("reviewCount", 10);
		insertMap.put("rating", 4.5);
		insertMap.put("description", "마이바티스 CLOB 설명입니다.".repeat(30));
		insertMap.put("pages", 300);
		insertMap.put("price", 25000);
		insertMap.put("bookCover", "uploads/mybatis.png");
		insertMap.put("appUserId", admin.getId());

		assertThat(bookMapper.insert(insertMap)).isEqualTo(1);

		// 검색으로 등록된 도서 id 확보
		Map<String, Object> searchMap = new HashMap<>();
		searchMap.put("searchType", "title");
		searchMap.put("keyword", "마이바티스");
		List<Book> found = bookMapper.searchBooks(searchMap);
		assertThat(found).isNotEmpty();
		Long bookId = found.get(0).getId();

		// 단건조회
		Book book = bookMapper.findById(bookId);
		assertThat(book.getAuthor()).isEqualTo("이바티스");
		assertThat(book.getDescription().length()).isGreaterThan(100);

		// 전체조회(페이징) + 카운트
		// ※ selectPaging/findAll/findByCategory 는 "OFFSET #{start} ROWS FETCH NEXT #{end} ROWS ONLY"
		//   방식이라 start 는 "건너뛸 행 개수"입니다(0부터 시작). 방금 등록한 도서는 ID 역순 정렬에서
		//   가장 앞(최신)에 오므로 start=0 이어야 결과에 포함됩니다.
		Map<String, Object> pagingMap = new HashMap<>();
		pagingMap.put("start", 0);
		pagingMap.put("end", 10);
		assertThat(bookMapper.findAll(pagingMap)).extracting(Book::getId).contains(bookId);
		assertThat(bookMapper.findAllCnt()).isGreaterThanOrEqualTo(1);

		// 카테고리조회(페이징) + 카운트
		Map<String, Object> categoryMap = new HashMap<>();
		categoryMap.put("category", "프로그래밍");
		categoryMap.put("start", 0);
		categoryMap.put("end", 10);
		assertThat(bookMapper.findByCategory(categoryMap)).extracting(Book::getId).contains(bookId);
		assertThat(bookMapper.findCategoryCnt("프로그래밍")).isEqualTo(1);

		// 제목중복확인
		assertThat(bookMapper.existsByTitle("마이바티스 도서")).isTrue();

		// 관리자기준 조회
		assertThat(bookMapper.findByAppUserId(admin.getId()))
				.extracting(Book::getId).contains(bookId);

		// 수정 (일부항목만)
		Map<String, Object> updateMap = new HashMap<>();
		updateMap.put("bookId", bookId);
		updateMap.put("price", 19000);
		assertThat(bookMapper.update(updateMap)).isEqualTo(1);
		assertThat(bookMapper.findById(bookId).getPrice()).isEqualTo(19000);
		assertThat(bookMapper.findById(bookId).getAuthor()).isEqualTo("이바티스"); // 나머지값 유지확인

		// 삭제
		assertThat(bookMapper.delete(bookId)).isEqualTo(1);
		assertThat(bookMapper.findById(bookId)).isNull();
	}

	//-------------------------------------------------------------------
	// 4. Sboard2Mapper (MyBatis) - CRUD / 조회수 / 페이징
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ Sboard2Mapper - CRUD / 조회수 / 페이징 (MyBatis)")
	void testSboard2Mapper() {
		AppUser admin = createAdmin("naver"); // 소셜로그인(네이버) 관리자로 등록해도 문제없는지 확인

		// 등록
		Map<String, Object> insertMap = new HashMap<>();
		insertMap.put("btitle", "마이바티스 공지");
		insertMap.put("bcontent", "마이바티스 CLOB 본문입니다.".repeat(30));
		insertMap.put("bfile", null);
		insertMap.put("bip", "127.0.0.1");
		insertMap.put("appUserId", admin.getId());

		assertThat(sboard2Mapper.insert(insertMap)).isEqualTo(1);

		// 검색으로 등록된 공지 id 확보
		List<Sboard2> found = sboard2Mapper.searchByTitle("마이바티스");
		assertThat(found).isNotEmpty();
		Long boardId = found.get(0).getId();

		// 단건조회
		Sboard2 board = sboard2Mapper.selectById(boardId);
		assertThat(board.getBtitle()).isEqualTo("마이바티스 공지");
		assertThat(board.getBhit()).isEqualTo(0);
		assertThat(board.getBcontent().length()).isGreaterThan(100);

		// 전체조회 / 페이징 / 카운트
		assertThat(sboard2Mapper.selectAll()).extracting(Sboard2::getId).contains(boardId);

		// ※ selectPaging 은 "OFFSET #{start} ROWS FETCH NEXT #{end} ROWS ONLY" 방식이라
		//   start 는 "건너뛸 행 개수"입니다(0부터 시작). 방금 등록한 공지는 ID 역순 정렬에서
		//   가장 앞(최신)에 오므로 start=0 이어야 결과에 포함됩니다.
		Map<String, Object> pagingMap = new HashMap<>();
		pagingMap.put("start", 0);
		pagingMap.put("end", 10);
		assertThat(sboard2Mapper.selectPaging(pagingMap)).extracting(Sboard2::getId).contains(boardId);
		assertThat(sboard2Mapper.selectCnt()).isGreaterThanOrEqualTo(1);

		// 관리자기준 조회
		assertThat(sboard2Mapper.findByAppUserId(admin.getId()))
				.extracting(Sboard2::getId).contains(boardId);

		// 조회수 +1
		assertThat(sboard2Mapper.updateHit(boardId)).isEqualTo(1);
		assertThat(sboard2Mapper.selectById(boardId).getBhit()).isEqualTo(1);

		// 수정 (일부항목만)
		Map<String, Object> updateMap = new HashMap<>();
		updateMap.put("id", boardId);
		updateMap.put("btitle", "수정된 마이바티스 공지");
		assertThat(sboard2Mapper.update(updateMap)).isEqualTo(1);
		assertThat(sboard2Mapper.selectById(boardId).getBtitle()).isEqualTo("수정된 마이바티스 공지");

		// 삭제
		assertThat(sboard2Mapper.delete(boardId)).isEqualTo(1);
		assertThat(sboard2Mapper.selectById(boardId)).isNull();
	}
}
