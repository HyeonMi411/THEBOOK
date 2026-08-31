package com.thejoa703.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.dto.BookDto.BestsellerBookDto;
import com.thejoa703.dto.BookDto.BookRequestDto;
import com.thejoa703.dto.BookDto.BookResponseDto;
import com.thejoa703.dto.BookDto.StockUpdateRequestDto;
import com.thejoa703.api.BookNlDto;
import com.thejoa703.dto.PageResponseDto;
import com.thejoa703.service.AuthUserJwtService;
import com.thejoa703.service.BookService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Book Api", description = "도서 관련 API (등록/수정/삭제는 ROLE_ADMIN 전용)")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

	private final BookService        bookService;
	private final AuthUserJwtService authUserJwtService; // ###

	@Operation(summary = "도서 전체조회(페이징)", description = "page(1부터)/size 파라미터로 12개씩 페이징 조회, category 로 필터링 가능")
	@GetMapping
	public ResponseEntity<PageResponseDto<BookResponseDto>> getBooks(
			@Parameter(description = "페이지 번호(1부터)") @RequestParam(name = "page", defaultValue = "1") int page,
			@Parameter(description = "페이지당 개수") @RequestParam(name = "size", defaultValue = "12") int size,
			@Parameter(description = "카테고리 필터(선택)") @RequestParam(name = "category", required = false) String category
	) {
		return ResponseEntity.ok(bookService.getAllBooksPaged(page, size, category));
	}

	@Operation(summary = "도서 단건조회")
	@GetMapping("/{id}")
	public ResponseEntity<BookResponseDto> getBook(
			@Parameter(description = "도서 ID") @PathVariable("id") Long id
	) {
		return ResponseEntity.ok(bookService.getBook(id));
	}

	@Operation(summary = "도서 제목검색")
	@GetMapping("/search")
	public ResponseEntity<List<BookResponseDto>> search(
			@Parameter(description = "검색 키워드") @RequestParam("keyword") String keyword
	) {
		return ResponseEntity.ok(bookService.searchByTitle(keyword));
	}

	@Operation(
			summary = "베스트셀러(판매량 TOP 10) 조회 (전체공개)",
			description = "결제완료(PAID) 주문 기준으로 누적 판매량이 많은 순서대로 TOP 10 도서를 반환합니다. "
					+ "Redis 에 10분간 캐싱되며, 결제가 새로 완료되면 캐시가 즉시 무효화되어 다음 조회 때 최신 랭킹으로 다시 계산됩니다."
	)
	@GetMapping("/bestsellers")
	public ResponseEntity<List<BestsellerBookDto>> getBestsellers() {
		return ResponseEntity.ok(bookService.getBestsellers());
	}

	@Operation(summary = "도서등록 (ROLE_ADMIN 전용)", description = "로그인한 관리자 계정으로 도서를 등록합니다.")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<BookResponseDto> createBook(
			Authentication authentication,
			@Valid @ModelAttribute BookRequestDto dto, // multipart/form-data
			@Parameter(description = "도서표지 이미지") @RequestPart(name = "cover", required = false) MultipartFile cover
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		return ResponseEntity.ok(bookService.createBook(userId, dto, cover));
	}

	@Operation(summary = "도서수정 (ROLE_ADMIN 전용)")
	@PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<BookResponseDto> updateBook(
			@Parameter(description = "수정할 도서 ID") @PathVariable("id") Long id,
			@Valid @ModelAttribute BookRequestDto dto,
			@Parameter(description = "교체할 표지 이미지") @RequestPart(name = "cover", required = false) MultipartFile cover
	) {
		return ResponseEntity.ok(bookService.updateBook(id, dto, cover));
	}

	@Operation(
			summary = "도서 재고 수정 (ROLE_ADMIN 전용)",
			description = "재고가 없으면 새로 만들고, 있으면 값을 갱신합니다. 결제 기능(장바구니/주문/결제)을 "
					+ "Swagger 에서 테스트하려면 먼저 이 API로 원하는 도서에 재고를 채워주세요."
	)
	@PatchMapping("/{id}/stock")
	public ResponseEntity<BookResponseDto> updateStock(
			@Parameter(description = "재고를 수정할 도서 ID") @PathVariable("id") Long id,
			@Valid @RequestBody StockUpdateRequestDto dto
	) {
		return ResponseEntity.ok(bookService.updateStock(id, dto));
	}

	@Operation(summary = "도서삭제 (ROLE_ADMIN 전용)")
	@DeleteMapping("/{id}")
	public ResponseEntity<Long> deleteBook(
			@Parameter(description = "삭제할 도서 ID") @PathVariable("id") Long id
	) {
		bookService.deleteBook(id);
		return ResponseEntity.ok(id);
	}

	@Operation(
			summary = "카카오 도서검색 후 자동등록 (ROLE_ADMIN 전용)",
			description = "검색어로 카카오 도서검색 API(dapi.kakao.com)를 호출해 결과를 자동으로 DB에 저장합니다. "
					+ "이미 등록된 제목은 건너뜁니다. 검색버튼을 누르면 카카오 API에서 도서를 가져와 자동으로 "
					+ "DB에 저장한 후, 도서 목록을 다시 조회하시면 됩니다."
	)
	@PostMapping("/kakao-insert")
	public ResponseEntity<Map<String, Object>> kakaoInsert(
			Authentication authentication,
			@Parameter(description = "검색할 도서명") @RequestParam(name = "search") String search
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		int insertedCount = bookService.insertFromKakao(search, userId);
		return ResponseEntity.ok(Map.of("search", search, "insertedCount", insertedCount));
	}

	@Operation(
			summary = "국립중앙도서관 도서검색 (전체공개)",
			description = "키워드 또는 KDC 분류명으로 국립중앙도서관 오픈API를 검색합니다. DB에는 저장하지 않고 검색결과만 반환합니다."
	)
	@GetMapping("/national-library/search")
	public ResponseEntity<List<BookNlDto>> searchNationalLibrary(
			@Parameter(description = "검색어(키워드 또는 KDC 분류명)") @RequestParam(name = "keyword") String keyword,
			@Parameter(description = "페이지 번호(1부터)") @RequestParam(name = "page", defaultValue = "1") int page
	) {
		return ResponseEntity.ok(bookService.searchNationalLibrary(keyword, page));
	}

	@Operation(
			summary = "국립중앙도서관 검색결과 저장 (ROLE_ADMIN 전용)",
			description = "국립중앙도서관 검색결과 중 선택한 도서 1권을 DB에 저장합니다. 이미 등록된 제목이면 저장이 거부됩니다."
	)
	@PostMapping("/national-library/save")
	public ResponseEntity<BookResponseDto> saveNationalLibraryBook(
			Authentication authentication,
			@RequestBody BookNlDto nlBook
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		return ResponseEntity.ok(bookService.saveNationalLibraryBook(nlBook, userId));
	}
}
