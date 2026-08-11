package com.thejoa703;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.dto.BookDto.BookRequestDto;
import com.thejoa703.dto.BookDto.BookResponseDto;
import com.thejoa703.dto.Sboard2Dto.Sboard2RequestDto;
import com.thejoa703.dto.Sboard2Dto.Sboard2ResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.service.BookService;
import com.thejoa703.service.Sboard2Service;


@SpringBootTest
@Transactional
class Boot1ApplicationBookTests_2_Service {

	@Autowired  BookService    bookService;
	@Autowired  Sboard2Service sboard2Service;
	@Autowired  AppUserRepository appUserRepository;

	// 공통으로 사용할 유저를 생성해주는 헬퍼메서드 (AppUser 는 별도 UserService 가 없어 Repository 직접 사용)
	private Long createTestUser(String email, String nickname) {
		AppUser user = new AppUser();
		user.setEmail(email);
		user.setPassword("pass123");
		user.setNickname(nickname);
		user.setProvider("local");
		user.setDeleted(false);
		return appUserRepository.save(user).getId();
	}

	// 공통으로 사용할 도서 생성 헬퍼메서드
	private Long createTestBook(String title, String category) {
		BookRequestDto dto = new BookRequestDto();
		dto.setTitle(title);
		dto.setAuthor("홍길동");
		dto.setPublisher("테스트출판사");
		dto.setPublishDate(LocalDate.now());
		dto.setCategory(category);
		dto.setPrice(15000);
		return bookService.createBook(dto).getBookId();
	}

	//-------------------------------------------------------------------
	// BookService - CRUD
	//-------------------------------------------------------------------
	@Test
	@Order(1)
	@DisplayName("■ BookService - CRUD : 등록, 단건조회, 수정, 삭제")
	void testBookServiceCrud() {
		Long bookId = createTestBook("이것이 자바다_" + UUID.randomUUID(), "IT");

		// 단건조회
		BookResponseDto found = bookService.getBookById(bookId);
		assertThat(found.getCategory()).isEqualTo("IT");

		// 수정 (더티체킹)
		BookRequestDto updateDto = new BookRequestDto();
		updateDto.setTitle(found.getTitle());
		updateDto.setAuthor("김철수");   // 저자 변경
		updateDto.setPublisher(found.getPublisher());
		updateDto.setPublishDate(found.getPublishDate());
		updateDto.setCategory(found.getCategory());
		updateDto.setPrice(20000);
		BookResponseDto updated = bookService.updateBook(bookId, updateDto);
		assertThat(updated.getAuthor()).isEqualTo("김철수");
		assertThat(updated.getPrice()).isEqualTo(20000);

		// 삭제
		bookService.deleteBook(bookId);
		assertThatThrownBy(() -> bookService.getBookById(bookId))
				.isInstanceOf(IllegalArgumentException.class);
	}

	//-------------------------------------------------------------------
	// BookService - 페이징/검색
	//-------------------------------------------------------------------
	@Test
	@Order(2)
	@DisplayName("■ BookService - 카테고리페이징 / 통합검색")
	void testBookServicePagingAndSearch() {
		String uniqueTitle = "스프링 부트 마스터_" + UUID.randomUUID();
		createTestBook(uniqueTitle, "IT");

		// 카테고리별 페이징조회
		List<BookResponseDto> paged = bookService.getBooksByCategory("IT", 1, 10);
		assertThat(paged).isNotEmpty();

		// 제목/저자 통합검색
		List<BookResponseDto> searched = bookService.searchBooks(uniqueTitle.substring(0, 5), 1, 10);
		assertThat(searched).isNotEmpty();
	}

	//-------------------------------------------------------------------
	// BookService - 찜하기(ManyToMany)
	//-------------------------------------------------------------------
	@Test
	@Order(3)
	@DisplayName("■ BookService - 찜하기/찜취소 (AppUser-Book ManyToMany)")
	void testBookServiceLike() {
		Long userId = createTestUser("liker_" + UUID.randomUUID() + "@test.com", "liker_" + UUID.randomUUID());
		Long bookId = createTestBook("찜테스트도서_" + UUID.randomUUID(), "소설");

		// 찜 등록
		bookService.likeBook(userId, bookId);
		BookResponseDto afterLike = bookService.getBookById(bookId);
		assertThat(afterLike.getLikedCount()).isEqualTo(1L);

		// 찜 랭킹 TOP N 에 포함되는지 확인
		List<BookResponseDto> topLiked = bookService.getTopLikedBooks(20);
		assertThat(topLiked.stream().anyMatch(b -> b.getBookId().equals(bookId))).isTrue();

		// 찜 취소
		bookService.unlikeBook(userId, bookId);
		BookResponseDto afterUnlike = bookService.getBookById(bookId);
		assertThat(afterUnlike.getLikedCount()).isEqualTo(0L);
	}

	//-------------------------------------------------------------------
	// Sboard2Service - CRUD (ManyToOne : AppUser)
	//-------------------------------------------------------------------
	@Test
	@Order(4)
	@DisplayName("■ Sboard2Service - CRUD : 작성, 조회(조회수증가), 수정, 삭제")
	void testSboard2ServiceCrud() {
		Long userId = createTestUser("writer_" + UUID.randomUUID() + "@test.com", "writer_" + UUID.randomUUID());

		Sboard2RequestDto createDto = new Sboard2RequestDto();
		createDto.setBtitle("서비스 테스트 제목");
		createDto.setBcontent("서비스 테스트 내용");
		createDto.setBpass("1234");

		Sboard2ResponseDto created = sboard2Service.createBoard(userId, createDto, "127.0.0.1");
		assertThat(created.getUserNickname()).isNotNull();
		assertThat(created.getBhit()).isEqualTo(0);

		// 단건조회 - 조회수 +1 확인
		Sboard2ResponseDto found = sboard2Service.getBoardById(created.getId());
		assertThat(found.getBhit()).isEqualTo(1);

		// 특정유저 작성글목록 (ManyToOne)
		List<Sboard2ResponseDto> myBoards = sboard2Service.getBoardsByUser(userId);
		assertThat(myBoards).hasSize(1);

		// 수정 - 비밀번호 일치
		Sboard2RequestDto updateDto = new Sboard2RequestDto();
		updateDto.setBtitle("수정된 제목");
		updateDto.setBcontent("수정된 내용");
		updateDto.setBpass("1234");
		Sboard2ResponseDto updated = sboard2Service.updateBoard(userId, created.getId(), updateDto);
		assertThat(updated.getBtitle()).isEqualTo("수정된 제목");

		// 수정 - 비밀번호 불일치시 예외
		Sboard2RequestDto wrongPassDto = new Sboard2RequestDto();
		wrongPassDto.setBtitle("실패해야함");
		wrongPassDto.setBcontent("내용");
		wrongPassDto.setBpass("9999");
		assertThatThrownBy(() -> sboard2Service.updateBoard(userId, created.getId(), wrongPassDto))
				.isInstanceOf(IllegalArgumentException.class);

		// 삭제 - 비밀번호 불일치시 예외
		assertThatThrownBy(() -> sboard2Service.deleteBoard(userId, created.getId(), "0000"))
				.isInstanceOf(IllegalArgumentException.class);

		// 삭제 - 정상
		sboard2Service.deleteBoard(userId, created.getId(), "1234");
		assertThatThrownBy(() -> sboard2Service.getBoardById(created.getId()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	//-------------------------------------------------------------------
	// Sboard2Service - 페이징
	//-------------------------------------------------------------------
	@Test
	@Order(5)
	@DisplayName("■ Sboard2Service - 전체글 페이징조회")
	void testSboard2ServicePaging() {
		Long userId = createTestUser("pager_" + UUID.randomUUID() + "@test.com", "pager_" + UUID.randomUUID());

		Sboard2RequestDto dto = new Sboard2RequestDto();
		dto.setBtitle("페이징 테스트 글");
		dto.setBcontent("내용");
		dto.setBpass("1234");
		sboard2Service.createBoard(userId, dto, "127.0.0.1");

		List<Sboard2ResponseDto> paged = sboard2Service.getBoardsPaged(1, 10);
		assertThat(paged).isNotEmpty();
	}
}
