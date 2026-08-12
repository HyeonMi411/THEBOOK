package com.thejoa703;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.dto.BookDto.BookRequestDto;
import com.thejoa703.dto.BookDto.BookResponseDto;
import com.thejoa703.dto.Sboard2Dto.Sboard2RequestDto;
import com.thejoa703.dto.Sboard2Dto.Sboard2ResponseDto;
import com.thejoa703.dto.UserDto.UserRequestDto;
import com.thejoa703.dto.UserDto.UserResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.exception.ResourceNotFoundException;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.service.BookService;
import com.thejoa703.service.Sboard2Service;
import com.thejoa703.service.UserService;

@SpringBootTest
@Transactional
class Boot2ApplicationTests_4_BookSboard2Service {

	@Autowired private BookService        bookService;
	@Autowired private Sboard2Service     sboard2Service;
	@Autowired private UserService        userService;
	@Autowired private AppUserRepository  appUserRepository;

	//-------------------------------------------------------------------
	// 공통 헬퍼메서드
	//-------------------------------------------------------------------
	// ★관리자 계정 생성 - 도서등록/공지작성은 관리자(ROLE_ADMIN)만 가능하므로
	//   회원가입 서비스(UserService.createUser, 항상 ROLE_USER 고정)를 거치지 않고
	//   AppUserRepository로 직접 생성한다.
	private Long createAdmin(String email, String nickname) {
		AppUser admin = new AppUser();
		admin.setEmail(email);
		admin.setPassword("admin123");
		admin.setNickname(nickname);
		admin.setProvider("local");
		admin.setRole("ROLE_ADMIN");
		admin.setDeleted(false);
		appUserRepository.save(admin);
		return admin.getId();
	}

	// 일반회원 생성 - 권한없음(403 대신 400) 케이스 검증용, UserService.createUser 그대로 사용
	private Long createTestUser(String email, String nickname) {
		UserRequestDto signupDto = new UserRequestDto();
		signupDto.setEmail(email);
		signupDto.setPassword("password123");
		signupDto.setNickname(nickname);
		signupDto.setProvider("local");

		MockMultipartFile profileImage = new MockMultipartFile(
				"profileImage", "test.png", "image/png", "test image content".getBytes()
		);

		UserResponseDto res = userService.createUser(signupDto, profileImage);
		return res.getId();
	}

	// 도서 등록 요청 Dto 생성 헬퍼
	private BookRequestDto buildBookRequest(String title, String author, String category,
	                                         Integer reviewCount, Double rating) {
		BookRequestDto dto = new BookRequestDto();
		dto.setTitle(title);
		dto.setAuthor(author);
		dto.setPublisher("테스트출판사");
		dto.setPublishDate(LocalDate.now().minusDays(1));
		dto.setCategory(category);
		dto.setReviewCount(reviewCount);
		dto.setRating(rating);
		dto.setDescription("설명 - " + title);
		dto.setPages(300);
		dto.setPrice(15000);
		return dto;
	}

	// 공지글 작성 요청 Dto 생성 헬퍼
	private Sboard2RequestDto buildNoticeRequest(String btitle, String bcontent) {
		Sboard2RequestDto dto = new Sboard2RequestDto();
		dto.setBtitle(btitle);
		dto.setBcontent(bcontent);
		dto.setBpass("1234");
		return dto;
	}


	//=====================================================================
	// 1. BookService - 등록/조회/수정/삭제 + 관리자 권한검증
	//=====================================================================
	@Test
	@Order(1)
	@DisplayName("■ BookService - CRUD : 등록(관리자전용), 조회, 수정, 삭제, 도서명중복검사")
	void testBookService_CRUD_AdminOnly() {

		Long adminId  = createAdmin("book_admin_" + System.nanoTime() + "@test.com", "bookAdmin");
		Long userId   = createTestUser("book_user_" + System.nanoTime() + "@test.com", "bookUser");

		MockMultipartFile cover = new MockMultipartFile(
				"cover", "cover.png", "image/png", "cover-image-bytes".getBytes()
		);

		// ★관리자만 도서등록 가능 - 정상 등록
		BookRequestDto createDto = buildBookRequest("스프링 부트 입문", "김영한", "IT", 10, 4.5);
		BookResponseDto created  = bookService.createBook(adminId, createDto, cover);

		assertThat(created.getId()).isNotNull();
		assertThat(created.getTitle()).isEqualTo("스프링 부트 입문");
		assertThat(created.getAdminId()).isEqualTo(adminId);
		assertThat(created.getAdminNickname()).isEqualTo("bookAdmin");
		assertThat(created.getBookCover()).isNotBlank();
		assertThat(created.getRegDate()).isNotNull();

		// 도서명 중복검사(AJAX)
		assertThat(bookService.existsByTitle("스프링 부트 입문")).isTrue();

		// 중복된 제목으로 재등록 시도 → 예외
		assertThatThrownBy(() ->
				bookService.createBook(adminId, buildBookRequest("스프링 부트 입문", "다른저자", "IT", 1, 3.0), null)
		).isInstanceOf(IllegalArgumentException.class);

		// ★일반회원(userId)은 도서등록 불가 → 예외
		assertThatThrownBy(() ->
				bookService.createBook(userId, buildBookRequest("일반유저도서", "홍길동", "소설", 1, 3.0), null)
		).isInstanceOf(IllegalArgumentException.class);

		// 단건조회
		BookResponseDto found = bookService.getBookById(created.getId());
		assertThat(found.getTitle()).isEqualTo("스프링 부트 입문");

		// 수정 (관리자만 가능)
		BookRequestDto updateDto = buildBookRequest("스프링 부트 입문(개정판)", "김영한", "IT", 20, 4.8);
		BookResponseDto updated = bookService.updateBook(adminId, created.getId(), updateDto, null);
		assertThat(updated.getTitle()).isEqualTo("스프링 부트 입문(개정판)");
		assertThat(updated.getReviewCount()).isEqualTo(20);

		// ★일반회원은 수정 불가 → 예외
		assertThatThrownBy(() ->
				bookService.updateBook(userId, created.getId(), updateDto, null)
		).isInstanceOf(IllegalArgumentException.class);

		// ★일반회원은 삭제 불가 → 예외
		assertThatThrownBy(() ->
				bookService.deleteBook(userId, created.getId())
		).isInstanceOf(IllegalArgumentException.class);

		// 삭제 (관리자만 가능)
		bookService.deleteBook(adminId, created.getId());

		// 삭제 후 재조회 불가
		assertThatThrownBy(() -> bookService.getBookById(created.getId()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@Order(2)
	@DisplayName("■ BookService - 목록조회 / 페이징 / 카테고리 / 통합검색 / 관리자별조회")
	void testBookService_QueryMethods() {

		Long adminId = createAdmin("book_admin2_" + System.nanoTime() + "@test.com", "bookAdmin2");

		bookService.createBook(adminId, buildBookRequest("이펙티브 자바", "조슈아 블로크", "IT", 30, 4.9), null);
		bookService.createBook(adminId, buildBookRequest("클린 코드", "로버트 마틴", "IT", 20, 4.6), null);
		bookService.createBook(adminId, buildBookRequest("데미안", "헤르만 헤세", "소설", 5, 4.2), null);

		// 전체조회 (최신등록순) - 정렬 자체는 BookRepository 테스트에서 이미 검증했으므로
		// 여기서는 Service 가 3건을 빠짐없이 반환하는지만 확인한다.
		List<BookResponseDto> all = bookService.getAllBooks();
		assertThat(all).hasSize(3);
		assertThat(all).extracting(BookResponseDto::getTitle)
				.containsExactlyInAnyOrder("이펙티브 자바", "클린 코드", "데미안");

		// 오라클 네이티브 페이징조회
		List<BookResponseDto> paged = bookService.getBooksPaged(1, 3);
		assertThat(paged).hasSize(3);

		// 카테고리별 조회 / 페이징
		List<BookResponseDto> itBooks = bookService.getBooksByCategory("IT");
		assertThat(itBooks).hasSize(2);
		List<BookResponseDto> itPaged = bookService.getBooksByCategoryPaged("IT", 1, 10);
		assertThat(itPaged).hasSize(2);

		// 통합검색 (제목)
		List<BookResponseDto> searched = bookService.searchBooks("title", "자바");
		assertThat(searched).hasSize(1);
		assertThat(searched.get(0).getAuthor()).isEqualTo("조슈아 블로크");

		// ★관리자별 등록도서 조회
		List<BookResponseDto> byAdmin = bookService.getBooksByAdmin(adminId);
		assertThat(byAdmin).hasSize(3);
	}


	//=====================================================================
	// 2. Sboard2Service - 작성/조회/수정/삭제 + 관리자 권한검증 + 조회수증가
	//=====================================================================
	@Test
	@Order(3)
	@DisplayName("■ Sboard2Service - CRUD : 작성(관리자전용), 상세조회(조회수증가), 수정, 삭제")
	void testSboard2Service_CRUD_AdminOnly() {

		Long adminId = createAdmin("board_admin_" + System.nanoTime() + "@test.com", "boardAdmin");
		Long userId  = createTestUser("board_user_" + System.nanoTime() + "@test.com", "boardUser");

		MockMultipartFile file = new MockMultipartFile(
				"file", "notice.pdf", "application/pdf", "notice-file-bytes".getBytes()
		);

		// ★관리자만 공지글 작성 가능 - 정상 작성
		Sboard2RequestDto createDto = buildNoticeRequest("서버 점검 안내", "금일 자정 서버점검이 진행됩니다.");
		Sboard2ResponseDto created  = sboard2Service.createNotice(adminId, createDto, file, "127.0.0.1");

		assertThat(created.getId()).isNotNull();
		assertThat(created.getBtitle()).isEqualTo("서버 점검 안내");
		assertThat(created.getAdminId()).isEqualTo(adminId);
		assertThat(created.getAdminNickname()).isEqualTo("boardAdmin");
		assertThat(created.getBfile()).isNotBlank();
		assertThat(created.getBhit()).isEqualTo(0);
		assertThat(created.getBip()).isEqualTo("127.0.0.1");
		assertThat(created.getCreatedAt()).isNotNull();

		// ★일반회원(userId)은 공지글 작성 불가 → 예외
		assertThatThrownBy(() ->
				sboard2Service.createNotice(userId, buildNoticeRequest("일반유저공지", "내용"), null, "127.0.0.1")
		).isInstanceOf(IllegalArgumentException.class);

		// 상세조회 - 조회할 때마다 조회수 +1
		Sboard2ResponseDto detail1 = sboard2Service.getNoticeDetail(created.getId());
		assertThat(detail1.getBhit()).isEqualTo(1);
		Sboard2ResponseDto detail2 = sboard2Service.getNoticeDetail(created.getId());
		assertThat(detail2.getBhit()).isEqualTo(2);

		// 수정 (관리자만 가능)
		Sboard2RequestDto updateDto = buildNoticeRequest("서버 점검 안내(연장)", "점검이 1시간 연장되었습니다.");
		Sboard2ResponseDto updated = sboard2Service.updateNotice(adminId, created.getId(), updateDto, null);
		assertThat(updated.getBtitle()).isEqualTo("서버 점검 안내(연장)");
		assertThat(updated.getBcontent()).isEqualTo("점검이 1시간 연장되었습니다.");

		// ★일반회원은 수정 불가 → 예외
		assertThatThrownBy(() ->
				sboard2Service.updateNotice(userId, created.getId(), updateDto, null)
		).isInstanceOf(IllegalArgumentException.class);

		// ★일반회원은 삭제 불가 → 예외
		assertThatThrownBy(() ->
				sboard2Service.deleteNotice(userId, created.getId())
		).isInstanceOf(IllegalArgumentException.class);

		// 삭제 (관리자만 가능)
		sboard2Service.deleteNotice(adminId, created.getId());

		// 삭제 후 재조회 불가 (조회수 증가쿼리는 대상없어 0건 영향, 이후 findById 에서 예외)
		assertThatThrownBy(() -> sboard2Service.getNoticeDetail(created.getId()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@Order(4)
	@DisplayName("■ Sboard2Service - 목록조회(페이징) / 전체갯수 / 제목검색 / 관리자별조회")
	void testSboard2Service_QueryMethods() {

		Long adminId = createAdmin("board_admin2_" + System.nanoTime() + "@test.com", "boardAdmin2");

		sboard2Service.createNotice(adminId, buildNoticeRequest("공지1 - 이용안내", "내용1"), null, "127.0.0.1");
		sboard2Service.createNotice(adminId, buildNoticeRequest("공지2 - 이용안내", "내용2"), null, "127.0.0.1");
		sboard2Service.createNotice(adminId, buildNoticeRequest("이벤트 안내", "내용3"), null, "127.0.0.1");

		// 목록조회(페이징)
		List<Sboard2ResponseDto> paged = sboard2Service.getNoticesPaged(1, 3);
		assertThat(paged).hasSize(3);

		// 전체 갯수
		assertThat(sboard2Service.getNoticeCount()).isEqualTo(3L);

		// 제목검색 / 검색+페이징
		List<Sboard2ResponseDto> searched = sboard2Service.searchByTitle("이용안내");
		assertThat(searched).hasSize(2);
		List<Sboard2ResponseDto> searchedPaged = sboard2Service.searchByTitlePaged("이용안내", 1, 10);
		assertThat(searchedPaged).hasSize(2);

		// ★관리자별 작성 공지글 조회
		List<Sboard2ResponseDto> byAdmin = sboard2Service.getNoticesByAdmin(adminId);
		assertThat(byAdmin).hasSize(3);
	}

}
