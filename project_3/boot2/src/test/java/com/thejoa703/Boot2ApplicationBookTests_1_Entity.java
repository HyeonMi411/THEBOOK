package com.thejoa703;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
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
import com.thejoa703.mapper.AppUserMapper;
import com.thejoa703.mapper.BookCategoryStat;
import com.thejoa703.mapper.BookMapper;
import com.thejoa703.mapper.Sboard2Mapper;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.BookRepository;
import com.thejoa703.repository.Sboard2Repository;


@SpringBootTest
@Transactional   // org.springframework.transaction.annotation.Transactional
class Boot2ApplicationBookTests_1_Entity {

	@Autowired  private AppUserRepository appUserRepository;
	@Autowired  private BookRepository    bookRepository;
	@Autowired  private Sboard2Repository sboard2Repository;

	@Autowired  private jakarta.persistence.EntityManager entityManager;  // 연관관계 재조회(flush/clear) 용

	// 테스트공통데이터 : 사용자2명 + 책 1권
	private AppUser user1;
	private AppUser user2;
	private Book    book;

	@BeforeEach
	void setup() {   // import java.util.UUID
		// 사용자 생성
		String email1 = "user1_" + UUID.randomUUID() + "@test.com";
		String email2 = "user2_" + UUID.randomUUID() + "@test.com";

		user1 = new AppUser();
		user1.setEmail(email1);
		user1.setPassword("pass123");
		user1.setNickname("user1_" + UUID.randomUUID());
		user1.setProvider("local");
		user1.setDeleted(false);

		user2 = new AppUser();
		user2.setEmail(email2);
		user2.setPassword("pass123");
		user2.setNickname("user2_" + UUID.randomUUID());
		user2.setProvider("local");
		user2.setDeleted(false);

		appUserRepository.save(user1);
		appUserRepository.save(user2);

		// 책 생성
		book = new Book();
		book.setTitle("테스트 도서_" + UUID.randomUUID());
		book.setAuthor("홍길동");
		book.setPublisher("테스트출판사");
		book.setPublishDate(LocalDate.now());
		book.setCategory("IT");
		bookRepository.save(book);
	}

	//-------------------------------------------------------------------
	// AppUserRepository
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ AppUserRepository-CRUD")
	void testAppUserRepository() {
		// 이메일로 단건조회
		assertThat(   appUserRepository.findByEmail(  user1.getEmail()  ).get().getEmail()  )
		          .isEqualTo(  user1.getEmail()  );

		// 이메일 중복확인
		assertThat( appUserRepository.existsByEmail(user1.getEmail()) ).isTrue();

		// 닉네임으로 단건조회
		assertThat( appUserRepository.findByNickname(user1.getNickname()) ).isPresent();
	}


	//-------------------------------------------------------------------
	// BookRepository
	//-------------------------------------------------------------------
	// insert : save / select:findBy / update:save / delete:delete
	@Test
	@DisplayName("■ BookRepository-CRUD")
	void testBookRepository() {
		// 단건조회
		assertThat( bookRepository.findById(book.getBookId()).get().getTitle() )
				 .isEqualTo(book.getTitle());

		// 제목검색
		assertThat( bookRepository.findByTitleContaining(book.getTitle().substring(0,5)) )
				 .isNotEmpty();

		// 수정
		book.setPrice(15000);
		bookRepository.save(book);
		assertThat( bookRepository.findById(book.getBookId()).get().getPrice() )
				 .isEqualTo(15000);

		// 삭제후조회불가확인
		Long bookId = book.getBookId();
		bookRepository.delete(book);
		assertThat( bookRepository.findById(bookId) ).isEmpty();
	}

	@Test
	@DisplayName("■ BookRepository-페이징/집계 (native @Query)")
	void testBookRepositoryPagingAndStats() {
		// 카테고리별 페이징 조회
		List<Book> pagedByCategory = bookRepository.findByCategoryWithPaging("IT", 1, 10);
		assertThat(pagedByCategory).isNotEmpty();

		// 제목/저자 통합검색 페이징
		List<Book> searched = bookRepository.searchBooksWithPaging(book.getAuthor(), 1, 10);
		assertThat(searched).isNotEmpty();
	}


	//-------------------------------------------------------------------
	// Sboard2Repository  (Sboard2 - ManyToOne - AppUser / AppUser - OneToMany - Sboard2)
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ Sboard2Repository-CRUD (ManyToOne/OneToMany)")
	void testSboard2Repository() {
		// 게시글 생성 - user1 이 작성
		Sboard2 board = new Sboard2();
		board.setUser(user1);
		board.setBtitle("테스트 제목");
		board.setBcontent("테스트 내용입니다.");
		board.setBpass("1234");
		board.setBip("127.0.0.1");
		sboard2Repository.save(board);

		// 단건조회 - ManyToOne(user) 이 잘 매핑되었는지 확인
		Sboard2 found = sboard2Repository.findById(board.getId()).get();
		assertThat( found.getBtitle() ).isEqualTo("테스트 제목");
		assertThat( found.getUser().getId() ).isEqualTo(user1.getId());

		// 특정유저가 쓴 글목록 조회 (@EntityGraph)
		List<Sboard2> myBoards = sboard2Repository.findByUser_Id(user1.getId());
		assertThat(myBoards.size()).isEqualTo(1);

		// 조회수 증가 (@Modifying)
		sboard2Repository.increaseHit(board.getId());
		entityManager.flush();
		entityManager.clear();
		assertThat( sboard2Repository.findById(board.getId()).get().getBhit() ).isEqualTo(1);

		// 전체글 페이징 조회
		List<Sboard2> paged = sboard2Repository.findBoardsWithPaging(1, 10);
		assertThat(paged).isNotEmpty();

		// OneToMany(AppUser.boards) 반대편 확인 - flush/clear 후 재조회
		entityManager.clear();
		AppUser reloadedUser = appUserRepository.findById(user1.getId()).get();
		assertThat( reloadedUser.getBoards() ).hasSize(1);
		assertThat( reloadedUser.getBoards().get(0).getBtitle() ).isEqualTo("테스트 제목");

		// 삭제후조회불가확인
		Long boardId = board.getId();
		sboard2Repository.delete(board);
		assertThat( sboard2Repository.findById(boardId) ).isEmpty();
	}


	//-------------------------------------------------------------------
	// AppUser <-> Book  (ManyToMany : 찜/좋아요)
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ AppUser-Book ManyToMany (찜하기)")
	void testAppUserBookLike() {
		// user1, user2 모두 같은 책을 찜
		user1.getLikedBooks().add(book);
		user2.getLikedBooks().add(book);
		appUserRepository.save(user1);
		appUserRepository.save(user2);

		entityManager.flush();
		entityManager.clear();

		// 소유쪽(AppUser.likedBooks) 확인
		AppUser reloadedUser1 = appUserRepository.findById(user1.getId()).get();
		assertThat( reloadedUser1.getLikedBooks() ).hasSize(1);
		assertThat( reloadedUser1.getLikedBooks().get(0).getTitle() ).isEqualTo(book.getTitle());

		// 반대쪽(Book.likedByUsers) 확인 - fetch join 쿼리 사용
		Optional<Book> foundBook = bookRepository.findByIdWithLikedUsers(book.getBookId());
		assertThat(foundBook).isPresent();
		assertThat(foundBook.get().getLikedByUsers()).hasSize(2);

		// 찜 많이받은 책 랭킹 TOP N
		List<Book> topLiked = bookRepository.findTopLikedBooks(5);
		assertThat(topLiked).isNotEmpty();

		// 찜 취소 (컬렉션에서 제거)
		AppUser managedUser1 = appUserRepository.findById(user1.getId()).get();
		managedUser1.getLikedBooks().removeIf(b -> b.getBookId().equals(book.getBookId()));
		appUserRepository.save(managedUser1);

		entityManager.flush();
		entityManager.clear();

		AppUser reCheckUser1 = appUserRepository.findById(user1.getId()).get();
		assertThat( reCheckUser1.getLikedBooks() ).isEmpty();
	}


	//-------------------------------------------------------------------
	// AppUser 삭제시 Sboard2 cascade + orphanRemoval 확인
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ AppUser 삭제시 작성글도 함께 삭제 (cascade, orphanRemoval)")
	void testAppUserCascadeDeleteBoards() {
		Sboard2 board = new Sboard2();
		board.setUser(user1);
		board.setBtitle("삭제테스트용 글");
		board.setBcontent("내용");
		board.setBpass("1234");
		board.setBip("127.0.0.1");
		sboard2Repository.save(board);

		entityManager.flush();
		entityManager.clear();

		Long userId  = user1.getId();
		Long boardId = board.getId();

		AppUser managedUser = appUserRepository.findById(userId).get();
		appUserRepository.delete(managedUser);

		entityManager.flush();
		entityManager.clear();

		assertThat( appUserRepository.findById(userId) ).isEmpty();
		assertThat( sboard2Repository.findById(boardId) ).isEmpty();
	}


	//-------------------------------------------------------------------
	// Mapper (MyBatis) - BookMapper
	//-------------------------------------------------------------------
	@Autowired  private BookMapper    bookMapper;
	@Test
	@DisplayName("■ BookMapper-CRUD (MyBatis)")
	void testBookMapper() {
		
		entityManager.flush();

		// 제목 키워드검색
		List<Book> found = bookMapper.findByKeyword(book.getTitle().substring(0,5));
		assertThat(found).isNotEmpty();
		assertThat(found.get(0).getTitle()).contains(book.getTitle().substring(0,5));

		// 카테고리별 통계
		List<BookCategoryStat> stats = bookMapper.findCategoryStats();
		assertThat(stats).isNotEmpty();
		assertThat(stats.stream().anyMatch(s -> "IT".equals(s.getCategory()))).isTrue();
	}


	//-------------------------------------------------------------------
	// Mapper (MyBatis) - Sboard2Mapper
	//-------------------------------------------------------------------
	@Autowired  private Sboard2Mapper sboard2Mapper;
	@Test
	@DisplayName("■ Sboard2Mapper-CRUD (MyBatis)")
	void testSboard2Mapper() {
		Sboard2 board = new Sboard2();
		board.setUser(user1);
		board.setBtitle("마이바티스 테스트 제목");
		board.setBcontent("내용");
		board.setBpass("1234");
		board.setBip("127.0.0.1");
		sboard2Repository.save(board);
		entityManager.flush();
		entityManager.clear();

		// 제목 키워드검색
		List<Sboard2> found = sboard2Mapper.findByKeyword("마이바티스");
		assertThat(found).isNotEmpty();

		// 특정유저 글수 집계
		int count = sboard2Mapper.countByAppUserId(user1.getId());
		assertThat(count).isEqualTo(1);
	}


	//-------------------------------------------------------------------
	// Mapper (MyBatis) - AppUserMapper
	//-------------------------------------------------------------------
	@Autowired  private AppUserMapper appUserMapper;
	@Test
	@DisplayName("■ AppUserMapper-CRUD (MyBatis)")
	void testAppUserMapper() {
		entityManager.flush();
		entityManager.clear();

		// 닉네임 키워드검색
		List<AppUser> found = appUserMapper.findByNicknameKeyword(user1.getNickname().substring(0,5));
		assertThat(found).isNotEmpty();
	}
}
