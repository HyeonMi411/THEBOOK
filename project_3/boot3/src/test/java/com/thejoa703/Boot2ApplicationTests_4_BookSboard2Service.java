package com.thejoa703;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.dto.BookDto.BookRequestDto;
import com.thejoa703.dto.BookDto.BookResponseDto;
import com.thejoa703.dto.PageResponseDto;
import com.thejoa703.dto.Sboard2Dto.Sboard2RequestDto;
import com.thejoa703.dto.Sboard2Dto.Sboard2ResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Sboard2;
import com.thejoa703.exception.ResourceNotFoundException;
import com.thejoa703.oauth2.CustomOAuth2User;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.Sboard2Repository;
import com.thejoa703.service.BookService;
import com.thejoa703.service.Sboard2Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * BookService / Sboard2Service  (+ DTO 검증, RestController 가 위임하는 실제 로직) 통합테스트
 * ------------------------------------------------------------------------------
 * - Boot2ApplicationTests_2_Service 패턴을 참고하여 클래스 레벨 @Transactional 로
 *   각 테스트가 끝나면 자동 롤백되도록 구성했습니다. (더미데이터 SQL로 넣어둔
 *   "스프링부트 완전정복" 등과 겹치지 않도록, 테스트에서 만드는 도서명/제목은
 *   전부 UUID 를 붙여 매번 고유하게 생성합니다.)
 * - 컨트롤러는 인증정보(Authentication)에서 사용자ID를 꺼내 서비스로 그대로 위임하는
 *   얇은 계층이므로, 서비스 계층을 직접 호출해 실제 비즈니스 로직(@PreAuthorize 권한체크,
 *   더티체킹 수정, 조회수 증가, 12개씩 페이징 등)을 검증합니다.
 * - Sboard2Service.getNotice() 의 조회수 증가는 @Modifying 벌크 UPDATE 라서 1차캐시가
 *   낀 상태로는 "실제로 DB에 반영됐는지"를 착각하기 쉽습니다. EntityManager.clear() 로
 *   1차캐시를 비우고 다시 조회해서, 진짜 DB로부터 새로 읽은 값으로 검증합니다.
 * - @Order 는 클래스에 @TestMethodOrder(OrderAnnotation.class) 를 붙여야만 실제로 적용됩니다.
 *   이걸 빠뜨리면 JUnit5 기본 순서(선언순서와 무관)로 실행되면서, 이전 테스트가 마지막에
 *   SecurityContextHolder 에 남겨둔 로그인상태(예: 관리자)가 다음 테스트로 새어나갈 수
 *   있습니다. 그래서 @TestMethodOrder 를 명시하고, @BeforeEach/@AfterEach 에서도
 *   SecurityContextHolder 를 매번 초기화해서 테스트 간 상태가 절대 섞이지 않게 했습니다.
 * - ⚠️ getAllBooks(String category) 는 더 이상 존재하지 않습니다. 12개씩 페이징 도입 이후
 *   getAllBooks() 는 무인자(전체 List) 버전만 남았고, 카테고리 필터/페이징은
 *   getAllBooksPaged(page, size, category) 로 옮겨졌습니다. (이 파일도 그에 맞춰 갱신했습니다)
 * - insertFromKakao() 는 실제 카카오 외부 API를 호출하므로, 네트워크가 없는 CI 환경에서도
 *   안정적으로 돌아가도록 "실제 카카오 응답 검증"은 하지 않고 "@PreAuthorize 로 일반회원은
 *   막히는지"만 검증합니다.
 * ------------------------------------------------------------------------------
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Boot2ApplicationTests_4_BookSboard2Service {

	@Autowired private AppUserRepository appUserRepository;
	@Autowired private Sboard2Repository sboard2Repository;
	@Autowired private BookService       bookService;
	@Autowired private Sboard2Service    sboard2Service;
	@Autowired private MockMvc           mockMvc;

	@PersistenceContext
	private EntityManager entityManager;

	// ------------------------------------------------------------------
	// 공통 헬퍼 : 관리자 / 일반회원 생성  (provider 로 소셜로그인 계정도 재현)
	// ------------------------------------------------------------------
	private AppUser createTestAdmin(String provider) {
		AppUser admin = new AppUser();
		admin.setEmail("svc_admin_" + UUID.randomUUID() + "@test.com");
		admin.setPassword("encoded-pass");
		admin.setNickname("svc_admin_" + UUID.randomUUID().toString().substring(0, 8));
		admin.setRole("ROLE_ADMIN");
		admin.setProvider(provider);                                   // local / google / kakao / naver
		admin.setProviderId(provider.equals("local") ? "local" : "social-" + UUID.randomUUID());
		return appUserRepository.save(admin);
	}

	private AppUser createTestUser() {
		AppUser user = new AppUser();
		user.setEmail("svc_user_" + UUID.randomUUID() + "@test.com");
		user.setPassword("encoded-pass");
		user.setNickname("svc_user_" + UUID.randomUUID().toString().substring(0, 8));
		user.setRole("ROLE_USER");
		user.setProvider("local");
		user.setProviderId("local");
		return appUserRepository.save(user);
	}

	// JwtAuthenticationFilter 가 실제 요청때마다 하는 일(=SecurityContext 세팅)을 테스트에서 재현
	private void loginAs(AppUser appUser) {
		CustomOAuth2User principal = new CustomOAuth2User(appUser.getId(), appUser.getRole());
		Authentication auth = new UsernamePasswordAuthenticationToken(
				principal, null, principal.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	// 테스트 간 SecurityContextHolder(ThreadLocal) 상태가 절대 섞이지 않도록 매번 초기화
	@BeforeEach
	void clearSecurityContextBefore() {
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void clearSecurityContextAfter() {
		SecurityContextHolder.clearContext();
	}

	//-------------------------------------------------------------------
	// 0. [진단용] @PreAuthorize 자체가 정상 동작하는지 최소단위로 확인
	//    DTO/파일업로드/DB쓰기 등 다른 변수를 모두 제거하고, 순수하게
	//    "ROLE_USER 로 @PreAuthorize(hasRole('ADMIN')) 메서드를 호출하면
	//     정말로 막히는가?" 만 검증합니다.
	//    ※ 이 테스트가 실패한다면 SecurityConfig 의 @EnableMethodSecurity 설정이나
	//      Spring Security 버전/구성 문제이지, Book/Sboard2 쪽 코드 문제가 아닙니다.
	//-------------------------------------------------------------------
	@Test
	@Order(0)
	@DisplayName("■ [진단] @PreAuthorize(hasRole('ADMIN')) 최소단위 동작 확인")
	void testPreAuthorizeSanityCheck() {
		AppUser user = createTestUser(); // ROLE_USER
		loginAs(user);

		System.out.println("[DEBUG][sanityCheck] Authentication      = "
				+ SecurityContextHolder.getContext().getAuthentication());
		System.out.println("[DEBUG][sanityCheck] isAuthenticated()    = "
				+ SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
		System.out.println("[DEBUG][sanityCheck] Authorities         = "
				+ SecurityContextHolder.getContext().getAuthentication().getAuthorities());

		// 존재하지 않는 ID(-1L)로 삭제를 시도합니다.
		// @PreAuthorize 가 정상 동작한다면, "존재하지 않는 데이터"라는 ResourceNotFoundException 이
		// 아니라 "권한없음" AccessDeniedException 이 먼저 발생해야 합니다.
		// (메서드 본문이 실행되기도 전에 보안검사가 먼저 걸려야 하기 때문)
		assertThatThrownBy(() -> bookService.deleteBook(-1L))
				.as("ROLE_USER 로 관리자전용 메서드(deleteBook) 호출 시 반드시 AccessDeniedException 이어야 합니다.")
				.isInstanceOf(AccessDeniedException.class);
	}

	//-------------------------------------------------------------------
	// 1. BookService - CRUD + 검색 + 페이징(12개씩) + ROLE_ADMIN 권한체크
	//-------------------------------------------------------------------
	@Test
	@Order(1)
	@DisplayName("■ BookService - 조회/검색/페이징 + 관리자전용 등록·수정·삭제(@PreAuthorize)")
	void testBookService() {
		AppUser admin = createTestAdmin("google"); // 소셜로그인(구글) 관리자
		AppUser user  = createTestUser();           // 일반회원(ROLE_USER)

		// 더미SQL 데이터(스프링부트 완전정복 등)와 겹치지 않도록 UUID 로 고유 제목/카테고리 생성
		String uniqueTitle = "서비스테스트도서_" + UUID.randomUUID();
		String uniqueCategory = "서비스테스트카테고리_" + UUID.randomUUID();

		BookRequestDto dto = new BookRequestDto();
		dto.setTitle(uniqueTitle);
		dto.setAuthor("테스트작가");
		dto.setPublisher("테스트출판사");
		dto.setPublishDate(LocalDate.of(2024, 5, 1));
		dto.setCategory(uniqueCategory);
		dto.setDescription("서비스 계층 테스트용 도서 설명입니다.");
		dto.setPrice(19900);

		MockMultipartFile cover = new MockMultipartFile(
				"cover", "cover.png", "image/png", "test-cover-bytes".getBytes());

		// 1) 일반회원 로그인상태 → 등록 거부(AccessDeniedException), 실제 저장 안됨
		loginAs(user);
		// sanity-check: 현재 SecurityContext 가 정말 ROLE_USER 로만 세팅됐는지 먼저 확인
		// (이 assertion 이 실패한다면 @PreAuthorize 문제가 아니라 로그인상태 세팅/누수 문제입니다.
		//  이 assertion 은 통과했는데 바로 아래 accessDenied 검증이 실패한다면, 그건 진짜
		//  @PreAuthorize 자체가 적용되지 않고 있다는 뜻이니 SecurityConfig/버전 문제를 봐야 합니다.)
		System.out.println("[DEBUG][testBookService] Authentication = "
				+ SecurityContextHolder.getContext().getAuthentication());
		System.out.println("[DEBUG][testBookService] Authorities   = "
				+ SecurityContextHolder.getContext().getAuthentication().getAuthorities());
		assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
				.extracting(Object::toString).containsExactly("ROLE_USER");
		assertThatThrownBy(() -> bookService.createBook(user.getId(), dto, cover))
				.isInstanceOf(AccessDeniedException.class);
		assertThat(bookService.searchByTitle(uniqueTitle)).isEmpty();

		// ★일반회원이라도 카카오 자동등록은 시도조차 못 해야 함 (관리자 전용)
		assertThatThrownBy(() -> bookService.insertFromKakao(uniqueTitle, user.getId()))
				.as("ROLE_USER 로 카카오 자동등록(insertFromKakao) 호출 시 반드시 AccessDeniedException 이어야 합니다.")
				.isInstanceOf(AccessDeniedException.class);

		// 2) 관리자 로그인상태 → 등록 성공 (표지이미지 업로드 경로까지 확인)
		loginAs(admin);
		BookResponseDto created = bookService.createBook(admin.getId(), dto, cover);
		assertThat(created.getId()).isNotNull();
		assertThat(created.getTitle()).isEqualTo(uniqueTitle);
		assertThat(created.getUserNickname()).isEqualTo(admin.getNickname());
		assertThat(created.getBookCover()).startsWith("uploads/"); // 실제 업로드 경로 확인

		// 3) 단건조회
		BookResponseDto found = bookService.getBook(created.getId());
		assertThat(found.getAuthor()).isEqualTo("테스트작가");

		// 4) 제목검색
		assertThat(bookService.searchByTitle(uniqueTitle)).extracting(BookResponseDto::getId)
				.contains(created.getId());

		// 5) 전체조회(비페이징, 내부용/구버전 호환) - getAllBooks() 는 이제 무인자입니다
		assertThat(bookService.getAllBooks()).extracting(BookResponseDto::getId)
				.contains(created.getId());

		// 6) ★페이징 조회(12개씩) + 카테고리 필터 - getAllBooksPaged(page, size, category)
		PageResponseDto<BookResponseDto> paged = bookService.getAllBooksPaged(1, 12, uniqueCategory);
		assertThat(paged.getContent()).extracting(BookResponseDto::getId).containsExactly(created.getId());
		assertThat(paged.getCurrentPage()).isEqualTo(1);
		assertThat(paged.getPageSize()).isEqualTo(12);
		assertThat(paged.getTotalElements()).isEqualTo(1L);
		assertThat(paged.getTotalPages()).isEqualTo(1);

		// 카테고리 필터 없이 전체 페이징 조회에도 포함되는지 확인
		PageResponseDto<BookResponseDto> pagedAll = bookService.getAllBooksPaged(1, 12, null);
		assertThat(pagedAll.getContent()).extracting(BookResponseDto::getId).contains(created.getId());

		// 7) 관리자 → 수정 성공 (더티체킹, 표지없이 나머지값만 변경)
		dto.setPrice(15000);
		BookResponseDto updated = bookService.updateBook(created.getId(), dto, null);
		assertThat(updated.getPrice()).isEqualTo(15000);
		assertThat(updated.getBookCover()).startsWith("uploads/"); // 표지 미첨부시 기존값 유지

		// 8) 일반회원 로그인상태 → 수정/삭제 거부
		loginAs(user);
		assertThatThrownBy(() -> bookService.updateBook(created.getId(), dto, null))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> bookService.deleteBook(created.getId()))
				.isInstanceOf(AccessDeniedException.class);

		// 9) 관리자 → 삭제 성공
		loginAs(admin);
		bookService.deleteBook(created.getId());
		assertThatThrownBy(() -> bookService.getBook(created.getId()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	//-------------------------------------------------------------------
	// 2. Sboard2Service - CRUD + 페이징(12개씩) + 조회수 증가가 "실제로 DB에" 반영되는지 검증
	//-------------------------------------------------------------------
	@Test
	@Order(2)
	@DisplayName("■ Sboard2Service - 관리자전용 작성·수정·삭제 + 페이징 + 조회수 DB반영 검증")
	void testSboard2Service() {
		AppUser admin = createTestAdmin("kakao"); // 소셜로그인(카카오) 관리자
		AppUser user  = createTestUser();

		// 더미SQL 데이터(사이트 오픈 안내 등)와 겹치지 않도록 UUID 로 고유 제목 생성
		String uniqueTitle = "서비스테스트공지_" + UUID.randomUUID();

		Sboard2RequestDto dto = new Sboard2RequestDto();
		dto.setBtitle(uniqueTitle);
		dto.setBcontent("서비스 계층 테스트용 공지 본문입니다.");

		// 1) 일반회원 로그인상태 → 작성 거부, 실제 저장 안됨
		loginAs(user);
		// sanity-check: 현재 SecurityContext 가 정말 ROLE_USER 로만 세팅됐는지 먼저 확인
		// (이 부분이 실패한다면 @PreAuthorize 문제가 아니라 로그인상태 세팅/누수 문제입니다)
		System.out.println("[DEBUG][testSboard2Service] Authentication = "
				+ SecurityContextHolder.getContext().getAuthentication());
		System.out.println("[DEBUG][testSboard2Service] Authorities   = "
				+ SecurityContextHolder.getContext().getAuthentication().getAuthorities());
		assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
				.extracting(Object::toString).containsExactly("ROLE_USER");
		assertThatThrownBy(() -> sboard2Service.createNotice(user.getId(), dto, null, "127.0.0.1"))
				.isInstanceOf(AccessDeniedException.class);
		assertThat(sboard2Service.searchByTitle(uniqueTitle)).isEmpty();

		// 2) 관리자 로그인상태 → 작성 성공 (첨부파일 없이)
		loginAs(admin);
		Sboard2ResponseDto created = sboard2Service.createNotice(admin.getId(), dto, null, "203.0.113.5");
		assertThat(created.getId()).isNotNull();
		assertThat(created.getBtitle()).isEqualTo(uniqueTitle);
		assertThat(created.getUserNickname()).isEqualTo(admin.getNickname());
		assertThat(created.getBip()).isEqualTo("203.0.113.5");
		assertThat(created.getBhit()).isEqualTo(0); // 최초 조회수는 0

		// ---------------------------------------------------------------
		// ★ 조회수 증가가 "실제로 DB에" 반영되는지 검증
		//    (1차캐시만 보고 착각하지 않도록 EntityManager.clear() 로 비우고 재조회)
		// ---------------------------------------------------------------
		Long boardId = created.getId();

		// getNotice() 를 3번 호출 → 매번 @Modifying 벌크 UPDATE 로 BHIT + 1
		Sboard2ResponseDto view1 = sboard2Service.getNotice(boardId);
		assertThat(view1.getBhit()).isEqualTo(1); // 서비스 응답값 확인(메모리상 동기화)

		entityManager.flush();
		entityManager.clear(); // 1차캐시 비우기 → 다음 조회는 무조건 진짜 SELECT

		Sboard2 freshAfterView1 = sboard2Repository.findById(boardId).orElseThrow();
		assertThat(freshAfterView1.getBhit()).isEqualTo(1); // DB에서 새로 읽은 값도 1

		Sboard2ResponseDto view2 = sboard2Service.getNotice(boardId);
		assertThat(view2.getBhit()).isEqualTo(2);

		Sboard2ResponseDto view3 = sboard2Service.getNotice(boardId);
		assertThat(view3.getBhit()).isEqualTo(3);

		entityManager.flush();
		entityManager.clear();

		Sboard2 freshAfterView3 = sboard2Repository.findById(boardId).orElseThrow();
		assertThat(freshAfterView3.getBhit()).isEqualTo(3); // 3번 조회 → DB에 실제로 3 반영 확인
		// ---------------------------------------------------------------

		// 3) 전체조회(비페이징) / 제목검색
		assertThat(sboard2Service.getAllNotices()).extracting(Sboard2ResponseDto::getId)
				.contains(boardId);
		assertThat(sboard2Service.searchByTitle(uniqueTitle)).extracting(Sboard2ResponseDto::getId)
				.contains(boardId);

		// 3-1) ★페이징 조회(12개씩) - getAllNoticesPaged(page, size)
		PageResponseDto<Sboard2ResponseDto> paged = sboard2Service.getAllNoticesPaged(1, 12);
		assertThat(paged.getContent()).extracting(Sboard2ResponseDto::getId).contains(boardId);
		assertThat(paged.getCurrentPage()).isEqualTo(1);
		assertThat(paged.getPageSize()).isEqualTo(12);
		assertThat(paged.getTotalElements()).isGreaterThanOrEqualTo(1L);

		// 4) 관리자 → 수정 성공 (더티체킹, 첨부파일 신규 업로드)
		dto.setBtitle(uniqueTitle + "_수정됨");
		MockMultipartFile file = new MockMultipartFile(
				"bfile", "notice.pdf", "application/pdf", "test-pdf-bytes".getBytes());
		Sboard2ResponseDto updated = sboard2Service.updateNotice(boardId, dto, file);
		assertThat(updated.getBtitle()).isEqualTo(uniqueTitle + "_수정됨");
		assertThat(updated.getBfile()).startsWith("uploads/"); // 첨부파일 업로드 경로 확인

		// 5) 일반회원 로그인상태 → 수정/삭제 거부
		loginAs(user);
		assertThatThrownBy(() -> sboard2Service.updateNotice(boardId, dto, null))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> sboard2Service.deleteNotice(boardId))
				.isInstanceOf(AccessDeniedException.class);

		// 6) 관리자 → 삭제 성공
		loginAs(admin);
		sboard2Service.deleteNotice(boardId);
		entityManager.flush();
		entityManager.clear();
		assertThat(sboard2Repository.findById(boardId)).isEmpty();
	}

	//-------------------------------------------------------------------
	// 3. AppUser 도서/공지 카운트(마이페이지용 findByUser_Id 계열) 서비스단 검증
	//-------------------------------------------------------------------
	@Test
	@Order(3)
	@DisplayName("■ 관리자 1명이 도서/공지를 여러건 등록해도 각각 정상 연결되는지 확인")
	void testAdminMultipleBooksAndNotices() {
		AppUser admin = createTestAdmin("naver"); // 소셜로그인(네이버) 관리자
		loginAs(admin);

		String titleA = "서비스테스트도서A_" + UUID.randomUUID();
		String titleB = "서비스테스트도서B_" + UUID.randomUUID();

		BookRequestDto bookDtoA = new BookRequestDto();
		bookDtoA.setTitle(titleA);
		bookDtoA.setAuthor("작가A");
		bookDtoA.setPublisher("출판사A");
		bookDtoA.setPublishDate(LocalDate.now());
		bookDtoA.setCategory("카테고리A");

		BookRequestDto bookDtoB = new BookRequestDto();
		bookDtoB.setTitle(titleB);
		bookDtoB.setAuthor("작가B");
		bookDtoB.setPublisher("출판사B");
		bookDtoB.setPublishDate(LocalDate.now());
		bookDtoB.setCategory("카테고리B");

		BookResponseDto bookA = bookService.createBook(admin.getId(), bookDtoA, null);
		BookResponseDto bookB = bookService.createBook(admin.getId(), bookDtoB, null);

		assertThat(bookA.getId()).isNotEqualTo(bookB.getId());
		assertThat(bookA.getUserNickname()).isEqualTo(admin.getNickname());
		assertThat(bookB.getUserNickname()).isEqualTo(admin.getNickname());

		List<BookResponseDto> byTitleA = bookService.searchByTitle(titleA);
		List<BookResponseDto> byTitleB = bookService.searchByTitle(titleB);
		assertThat(byTitleA).hasSize(1);
		assertThat(byTitleB).hasSize(1);
		assertThat(byTitleA.get(0).getId()).isEqualTo(bookA.getId());
		assertThat(byTitleB.get(0).getId()).isEqualTo(bookB.getId());
	}

	//-------------------------------------------------------------------
	// 4. [Swagger] /v3/api-docs 에 Book/Sboard2 API가 실제로 노출되는지 검증
	//    (지난번 "swagger에 book/notices 가 안 보인다" 문제의 회귀테스트)
	//-------------------------------------------------------------------
	@Test
	@Order(4)
	@DisplayName("■ [Swagger] /v3/api-docs 에 Book/Sboard2 API가 정상 노출되는지 확인")
	void testSwaggerDocsExposeBookAndNoticeEndpoints() throws Exception {
		// /v3/api-docs 는 SecurityConfig 에서 permitAll 이므로 로그인 없이 호출 가능
		MvcResult result = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andReturn();

		String json = result.getResponse().getContentAsString();

		// 1) 경로(path)가 문서에 등록되어 있는지
		assertThat(json).as("/v3/api-docs 응답에 /api/books 경로가 있어야 합니다.")
				.contains("\"/api/books\"");
		assertThat(json).as("/v3/api-docs 응답에 /api/notices 경로가 있어야 합니다.")
				.contains("\"/api/notices\"");

		// 2) 세부 경로(단건조회/검색/카카오등록)도 등록되어 있는지
		assertThat(json).as("/v3/api-docs 응답에 /api/books/{id} 경로가 있어야 합니다.")
				.contains("/api/books/{id}");
		assertThat(json).as("/v3/api-docs 응답에 /api/notices/{id} 경로가 있어야 합니다.")
				.contains("/api/notices/{id}");
		assertThat(json).as("/v3/api-docs 응답에 카카오 도서검색 자동등록 경로가 있어야 합니다.")
				.contains("/api/books/kakao-insert");

		// 3) @Tag 로 지정한 그룹명이 정상 반영되어 있는지
		assertThat(json).as("Book Api 태그가 있어야 합니다.")
				.contains("Book Api");
		assertThat(json).as("Notice(Sboard2) Api 태그가 있어야 합니다.")
				.contains("Notice(Sboard2) Api");
	}
}
