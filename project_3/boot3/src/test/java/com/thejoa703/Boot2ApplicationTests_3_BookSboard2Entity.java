package com.thejoa703;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
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

/**
 * Book / Sboard2 - BookMapper / Sboard2Mapper (MyBatis) 검증 테스트
 * ------------------------------------------------------------------------------
 * - 클래스에 @Transactional 을 걸어 자동롤백시키는 대신, 매 테스트가 끝난 뒤
 *   @AfterEach 에서 이 테스트가 생성한 데이터만 정확히 지워서 초기화.
 * - Service/Security(@PreAuthorize) 검증은 포함하지 않고, Mapper 만 검증.
 * ------------------------------------------------------------------------------
 */
@SpringBootTest
class Boot2ApplicationTests_3_BookSboard2Entity {

	@Autowired private AppUserRepository appUserRepository;
	@Autowired private BookMapper    bookMapper;
	@Autowired private Sboard2Mapper sboard2Mapper;

	private final List<Long> createdBookIds    = new ArrayList<>();
	private final List<Long> createdBoardIds   = new ArrayList<>();
	private final List<Long> createdAdminIds   = new ArrayList<>();

	private AppUser createAdmin(String provider) {
		AppUser admin = new AppUser();
		admin.setEmail("admin_" + UUID.randomUUID() + "@test.com");
		admin.setPassword("encoded-pass");
		admin.setNickname("admin_" + UUID.randomUUID().toString().substring(0, 8));
		admin.setRole("ROLE_ADMIN");
		admin.setProvider(provider);
		admin.setProviderId(provider.equals("local") ? "local" : "social-" + UUID.randomUUID());
		admin.setDeleted(false);
		appUserRepository.saveAndFlush(admin);
		createdAdminIds.add(admin.getId());
		return admin;
	}

	@AfterEach
	void tearDown() {
		createdBookIds.forEach(bookMapper::hardDelete);
		createdBoardIds.forEach(sboard2Mapper::delete);
		createdAdminIds.forEach(appUserRepository::deleteById);
	}

	//-------------------------------------------------------------------
	// 1. BookMapper (MyBatis) - CRUD / 검색(대소문자 무시) / 페이징
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ BookMapper - CRUD / 검색(대소문자 무시) / 페이징")
	void testBookMapper() {
		AppUser admin = createAdmin("local");

		Map<String, Object> insertMap = new HashMap<>();
		insertMap.put("title", "스프링부트 완전정복");
		insertMap.put("author", "김코딩");
		insertMap.put("publisher", "코딩출판사");
		insertMap.put("publishDate", LocalDate.of(2024, 1, 1));
		insertMap.put("category", "IT");
		insertMap.put("ranking", null);
		insertMap.put("reviewCount", 0);
		insertMap.put("rating", null);
		insertMap.put("description", "아주 긴 도서설명입니다.".repeat(50)); // CLOB 대용량 문자열 확인
		insertMap.put("pages", null);
		insertMap.put("price", 20000);
		insertMap.put("bookCover", "uploads/default_book.png");
		insertMap.put("appUserId", admin.getId());
		bookMapper.insert(insertMap);
		Long bookId = (Long) insertMap.get("bookId");
		createdBookIds.add(bookId);

		// 대소문자 검증용 - 한글은 대소문자 구분이 없으므로, 영문이 섞인 도서를 하나 더 등록
		Map<String, Object> insertMapEn = new HashMap<>();
		insertMapEn.put("title", "Modern JavaScript Deep Dive");
		insertMapEn.put("author", "Kim Coding");
		insertMapEn.put("publisher", "코딩출판사");
		insertMapEn.put("publishDate", LocalDate.of(2024, 1, 1));
		insertMapEn.put("category", "IT");
		insertMapEn.put("ranking", null);
		insertMapEn.put("reviewCount", 0);
		insertMapEn.put("rating", null);
		insertMapEn.put("description", "영문 제목/저자 대소문자 검색 테스트용 도서입니다.");
		insertMapEn.put("pages", null);
		insertMapEn.put("price", 25000);
		insertMapEn.put("bookCover", "uploads/default_book.png");
		insertMapEn.put("appUserId", admin.getId());
		bookMapper.insert(insertMapEn);
		Long bookIdEn = (Long) insertMapEn.get("bookId");
		createdBookIds.add(bookIdEn);

		// 단건조회 + CLOB 저장확인
		Book book = bookMapper.findById(bookId);
		assertThat(book).isNotNull();
		assertThat(book.getDescription().length()).isGreaterThan(100);

		// 카테고리조회 + 카운트
		Map<String, Object> categoryMap = new HashMap<>();
		categoryMap.put("category", "IT");
		categoryMap.put("start", 0);
		categoryMap.put("end", 10);
		assertThat(bookMapper.findByCategory(categoryMap)).extracting(Book::getId).contains(bookId);
		assertThat(bookMapper.findCategoryCnt("IT")).isEqualTo(2);

		// 대소문자 무시 검증 - 소문자/대문자/섞어써도 전부 찾아져야 함
		Map<String, Object> searchMap = new HashMap<>();
		searchMap.put("searchType", "title");
		searchMap.put("keyword", "modern");
		assertThat(bookMapper.searchBooks(searchMap)).extracting(Book::getId).contains(bookIdEn);

		searchMap.put("keyword", "JAVASCRIPT");
		assertThat(bookMapper.searchBooks(searchMap)).extracting(Book::getId).contains(bookIdEn);

		Map<String, Object> authorSearchMap = new HashMap<>();
		authorSearchMap.put("searchType", "author");
		authorSearchMap.put("keyword", "kim");
		assertThat(bookMapper.searchBooks(authorSearchMap)).extracting(Book::getId).contains(bookIdEn);

		// 제목중복확인
		assertThat(bookMapper.existsByTitle("스프링부트 완전정복")).isTrue();
		assertThat(bookMapper.existsByTitle("존재하지않는제목입니다")).isFalse();

		// 등록한 관리자기준 조회 (2권 등록했으므로 2건)
		assertThat(bookMapper.findByAppUserId(admin.getId())).hasSize(2);

		// 전체조회(페이징) + 카운트
		Map<String, Object> pagingMap = new HashMap<>();
		pagingMap.put("start", 0);
		pagingMap.put("end", 10);
		assertThat(bookMapper.findAll(pagingMap)).extracting(Book::getId).contains(bookId);
		assertThat(bookMapper.findAllCnt()).isGreaterThanOrEqualTo(2);

		// 수정 (일부항목만)
		Map<String, Object> updateMap = new HashMap<>();
		updateMap.put("bookId", bookId);
		updateMap.put("price", 30000);
		bookMapper.update(updateMap);
		assertThat(bookMapper.findById(bookId).getPrice()).isEqualTo(30000);
		assertThat(bookMapper.findById(bookId).getAuthor()).isEqualTo("김코딩"); // 나머지값 유지확인

		// 삭제 (소프트) - 실제 행은 그대로 남고 DELETED 플래그만 세워집니다. 행 자체는
		// 여전히 존재하므로(하드삭제가 아님) createdBookIds 에서 빼면 안 됨 - 빼버리면
		// tearDown() 이 이 행을 하드삭제하지 않고 그대로 남겨두게 되어, 이 도서가 계속
		// APP_USER 를 FK 로 참조한 채 남아있다가 나중에 admin 을 지울 때 ORA-02292
		// (자식 레코드가 발견되었음) 로 실패.
		bookMapper.updateDeleted(bookId, true);
		Book afterDelete = bookMapper.findById(bookId);
		assertThat(afterDelete).isNotNull();
		assertThat(afterDelete.isDeleted()).isTrue();
		// 소프트삭제된 도서는 목록/카테고리/검색/제목중복확인에서 제외되어야 함
		assertThat(bookMapper.findAll(pagingMap)).extracting(Book::getId).doesNotContain(bookId);
		assertThat(bookMapper.existsByTitle("스프링부트 완전정복")).isFalse();
	}

	//-------------------------------------------------------------------
	// 2. Sboard2Mapper (MyBatis) - CRUD / 조회수 / 검색(대소문자 무시) / 페이징
	//-------------------------------------------------------------------
	@Test
	@DisplayName("■ Sboard2Mapper - CRUD / 조회수 / 검색(대소문자 무시) / 페이징")
	void testSboard2Mapper() {
		AppUser admin = createAdmin("kakao"); // 소셜로그인(카카오) 관리자로 등록해도 문제없는지 확인

		Map<String, Object> insertMap = new HashMap<>();
		insertMap.put("btitle", "긴급 공지사항");
		insertMap.put("bcontent", "공지사항 본문내용입니다.".repeat(100)); // CLOB 대용량 문자열 확인
		insertMap.put("bfile", null);
		insertMap.put("bip", "127.0.0.1");
		insertMap.put("appUserId", admin.getId());
		sboard2Mapper.insert(insertMap);
		Long boardId = (Long) insertMap.get("id");
		createdBoardIds.add(boardId);

		// 대소문자 검증용 - 영문 제목의 공지사항을 하나 더 등록
		Map<String, Object> insertMapEn = new HashMap<>();
		insertMapEn.put("btitle", "System Maintenance Notice");
		insertMapEn.put("bcontent", "영문 제목 대소문자 검색 테스트용 공지사항입니다.");
		insertMapEn.put("bfile", null);
		insertMapEn.put("bip", "127.0.0.1");
		insertMapEn.put("appUserId", admin.getId());
		sboard2Mapper.insert(insertMapEn);
		Long boardIdEn = (Long) insertMapEn.get("id");
		createdBoardIds.add(boardIdEn);

		// 단건조회 + CLOB 저장확인
		Sboard2 board = sboard2Mapper.selectById(boardId);
		assertThat(board.getBtitle()).isEqualTo("긴급 공지사항");
		assertThat(board.getBhit()).isEqualTo(0);
		assertThat(board.getBcontent().length()).isGreaterThan(100);

		// 전체조회(최신순)
		assertThat(sboard2Mapper.selectAll()).extracting(Sboard2::getId).contains(boardId);

		// 대소문자 무시 검증 - 소문자/대문자 섞어써도 찾아져야 함
		assertThat(sboard2Mapper.searchByTitle("긴급")).extracting(Sboard2::getId).contains(boardId);
		assertThat(sboard2Mapper.searchByTitle("maintenance")).extracting(Sboard2::getId).contains(boardIdEn);
		assertThat(sboard2Mapper.searchByTitle("SYSTEM")).extracting(Sboard2::getId).contains(boardIdEn);

		// 작성한 관리자기준 조회 (2건 작성했으므로 2건)
		assertThat(sboard2Mapper.findByAppUserId(admin.getId())).hasSize(2);

		// 조회수 +1
		sboard2Mapper.updateHit(boardId);
		assertThat(sboard2Mapper.selectById(boardId).getBhit()).isEqualTo(1);

		// 전체조회(페이징) + 카운트
		Map<String, Object> pagingMap = new HashMap<>();
		pagingMap.put("start", 0);
		pagingMap.put("end", 10);
		assertThat(sboard2Mapper.selectPaging(pagingMap)).extracting(Sboard2::getId).contains(boardId);
		assertThat(sboard2Mapper.selectCnt()).isGreaterThanOrEqualTo(2);

		// 수정 (일부항목만)
		Map<String, Object> updateMap = new HashMap<>();
		updateMap.put("id", boardId);
		updateMap.put("btitle", "수정된 공지사항");
		sboard2Mapper.update(updateMap);
		assertThat(sboard2Mapper.selectById(boardId).getBtitle()).isEqualTo("수정된 공지사항");

		// 삭제
		sboard2Mapper.delete(boardId);
		assertThat(sboard2Mapper.selectById(boardId)).isNull();
		createdBoardIds.remove(boardId);
	}
}
