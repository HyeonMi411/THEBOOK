package com.thejoa703.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

	@Operation(summary = "도서 전체조회", description = "category 파라미터로 필터링 조회 가능")
	@GetMapping
	public ResponseEntity<List<BookResponseDto>> getBooks(
			@Parameter(description = "카테고리") @RequestParam(required = false) String category
	) {
		return ResponseEntity.ok(bookService.getAllBooks(category));
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

	@Operation(summary = "도서삭제 (ROLE_ADMIN 전용)")
	@DeleteMapping("/{id}")
	public ResponseEntity<Long> deleteBook(
			@Parameter(description = "삭제할 도서 ID") @PathVariable("id") Long id
	) {
		bookService.deleteBook(id);
		return ResponseEntity.ok(id);
	}
}
