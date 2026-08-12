package com.thejoa703.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.dto.BookDto.BookRequestDto;
import com.thejoa703.dto.BookDto.BookResponseDto;
import com.thejoa703.service.BookService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Book Api", description = "도서 관리 관련 API (등록/수정/삭제는 관리자 전용)")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

	private final BookService bookService;

	@Operation(summary = "도서 등록", description = "관리자(adminUserId, ROLE_ADMIN)만 도서를 등록할 수 있습니다.")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<BookResponseDto> createBook(
			@Parameter(description = "등록하는 관리자 사용자 ID") @RequestParam("adminUserId") Long adminUserId,
			@ModelAttribute BookRequestDto dto,   // multipart/form-data
			@Parameter(description = "도서 표지 이미지")
			@RequestPart(name = "cover", required = false) MultipartFile cover
	) {
		return ResponseEntity.ok(bookService.createBook(adminUserId, dto, cover));
	}

	@Operation(summary = "전체 도서 조회", description = "전체 도서 목록을 최신등록순으로 조회합니다.")
	@GetMapping
	public ResponseEntity<List<BookResponseDto>> getBooks() {
		return ResponseEntity.ok(bookService.getAllBooks());
	}

	@Operation(summary = "도서 페이징 조회", description = "Oracle ROWNUM 기반 페이징 조회입니다. (start, end 는 1부터 시작하는 순번)")
	@GetMapping("/paging")
	public ResponseEntity<List<BookResponseDto>> getBooksPaged(
			@Parameter(description = "시작 순번(1부터)") @RequestParam("start") int start,
			@Parameter(description = "끝 순번") @RequestParam("end") int end
	) {
		return ResponseEntity.ok(bookService.getBooksPaged(start, end));
	}

	@Operation(summary = "도서 단건 조회", description = "도서 ID로 단건 조회합니다.")
	@GetMapping("/{id}")
	public ResponseEntity<BookResponseDto> getBook(@PathVariable("id") Long id) {
		return ResponseEntity.ok(bookService.getBookById(id));
	}

	@Operation(summary = "카테고리별 도서 조회", description = "카테고리명으로 도서 목록을 조회합니다.")
	@GetMapping("/category/{category}")
	public ResponseEntity<List<BookResponseDto>> getBooksByCategory(
			@PathVariable("category") String category
	) {
		return ResponseEntity.ok(bookService.getBooksByCategory(category));
	}

	@Operation(summary = "카테고리별 도서 페이징 조회", description = "카테고리 + Oracle ROWNUM 페이징 조회입니다.")
	@GetMapping("/category/{category}/paging")
	public ResponseEntity<List<BookResponseDto>> getBooksByCategoryPaged(
			@PathVariable("category") String category,
			@RequestParam("start") int start,
			@RequestParam("end") int end
	) {
		return ResponseEntity.ok(bookService.getBooksByCategoryPaged(category, start, end));
	}

	@Operation(summary = "도서 통합검색", description = "제목(title)/저자(author)/카테고리(category) 기준으로 검색합니다.")
	@GetMapping("/search")
	public ResponseEntity<List<BookResponseDto>> searchBooks(
			@Parameter(description = "검색기준 (title, author, category)") @RequestParam("searchType") String searchType,
			@Parameter(description = "검색 키워드") @RequestParam("keyword") String keyword
	) {
		return ResponseEntity.ok(bookService.searchBooks(searchType, keyword));
	}

	@Operation(summary = "도서명 중복검사", description = "AJAX 도서명 중복검사용 API 입니다.")
	@GetMapping("/check-title")
	public ResponseEntity<Boolean> checkTitle(@RequestParam("title") String title) {
		return ResponseEntity.ok(bookService.existsByTitle(title));
	}

	@Operation(summary = "관리자별 등록도서 조회", description = "특정 관리자가 등록한 도서 목록을 조회합니다.")
	@GetMapping("/admin/{adminUserId}")
	public ResponseEntity<List<BookResponseDto>> getBooksByAdmin(
			@PathVariable("adminUserId") Long adminUserId
	) {
		return ResponseEntity.ok(bookService.getBooksByAdmin(adminUserId));
	}

	@Operation(summary = "도서 수정", description = "관리자(adminUserId, ROLE_ADMIN)만 도서를 수정할 수 있습니다.")
	@PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<BookResponseDto> updateBook(
			@Parameter(description = "수정하는 관리자 사용자 ID") @RequestParam("adminUserId") Long adminUserId,
			@Parameter(description = "수정할 도서 ID") @PathVariable("id") Long id,
			@ModelAttribute BookRequestDto dto,
			@Parameter(description = "변경할 도서 표지 이미지")
			@RequestPart(name = "cover", required = false) MultipartFile cover
	) {
		return ResponseEntity.ok(bookService.updateBook(adminUserId, id, dto, cover));
	}

	@Operation(summary = "도서 삭제", description = "관리자(adminUserId, ROLE_ADMIN)만 도서를 삭제할 수 있습니다.")
	@DeleteMapping("/{id}")
	public ResponseEntity<Long> deleteBook(
			@Parameter(description = "삭제하는 관리자 사용자 ID") @RequestParam("adminUserId") Long adminUserId,
			@PathVariable("id") Long id
	) {
		bookService.deleteBook(adminUserId, id);
		return ResponseEntity.ok(id);
	}
}
// 요청 : BookRequestDto ,  응답: BookResponseDto
// - POST   /api/books                        도서 등록(관리자전용)    ※기능: bookService.createBook
// - GET    /api/books                        전체 도서 조회           ※기능: bookService.getAllBooks
// - GET    /api/books/paging                 페이징 조회              ※기능: bookService.getBooksPaged
// - GET    /api/books/{id}                   단건 조회                ※기능: bookService.getBookById
// - GET    /api/books/category/{category}    카테고리별 조회          ※기능: bookService.getBooksByCategory
// - GET    /api/books/search                 통합검색                 ※기능: bookService.searchBooks
// - GET    /api/books/check-title            도서명 중복검사(AJAX)    ※기능: bookService.existsByTitle
// - GET    /api/books/admin/{adminUserId}    관리자별 등록도서 조회   ※기능: bookService.getBooksByAdmin
// - PATCH  /api/books/{id}                   도서 수정(관리자전용)    ※기능: bookService.updateBook
// - DELETE /api/books/{id}                   도서 삭제(관리자전용)    ※기능: bookService.deleteBook
