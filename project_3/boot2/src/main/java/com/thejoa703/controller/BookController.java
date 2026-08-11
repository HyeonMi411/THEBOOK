package com.thejoa703.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thejoa703.dto.BookDto.BookRequestDto;
import com.thejoa703.dto.BookDto.BookResponseDto;
import com.thejoa703.service.BookService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/jpa/books")
@RequiredArgsConstructor
public class BookController {

	private final BookService bookService;

	// GET  /api/jpa/books/category/{category}?start=1&end=10   카테고리별 페이징조회
	@GetMapping("/category/{category}")
	public ResponseEntity<List<BookResponseDto>> getBooksByCategory(
			@PathVariable("category") String category,
			@RequestParam(name = "start", defaultValue = "1") int start,
			@RequestParam(name = "end", defaultValue = "10") int end) {
		return ResponseEntity.ok(bookService.getBooksByCategory(category, start, end));
	}

	// GET  /api/jpa/books/search?keyword=자바&start=1&end=10     제목/저자 통합검색
	@GetMapping("/search")
	public ResponseEntity<List<BookResponseDto>> searchBooks(
			@RequestParam("keyword") String keyword,
			@RequestParam(name = "start", defaultValue = "1") int start,
			@RequestParam(name = "end", defaultValue = "10") int end) {
		return ResponseEntity.ok(bookService.searchBooks(keyword, start, end));
	}

	// GET  /api/jpa/books/ranking/liked?topN=5                   찜많은순 랭킹
	@GetMapping("/ranking/liked")
	public ResponseEntity<List<BookResponseDto>> getTopLikedBooks(
			@RequestParam(name = "topN", defaultValue = "5") int topN) {
		return ResponseEntity.ok(bookService.getTopLikedBooks(topN));
	}

	// GET  /api/jpa/books/{id}                                   단건조회
	@GetMapping("/{id}")
	public ResponseEntity<BookResponseDto> getBook(@PathVariable("id") Long id) {
		return ResponseEntity.ok(bookService.getBookById(id));
	}

	// POST /api/jpa/books                                        도서등록
	@PostMapping
	public ResponseEntity<BookResponseDto> createBook(@RequestBody BookRequestDto dto) {
		return ResponseEntity.ok(bookService.createBook(dto));
	}

	// PATCH /api/jpa/books/{id}                                  도서수정
	@PatchMapping("/{id}")
	public ResponseEntity<BookResponseDto> updateBook(
			@PathVariable("id") Long id,
			@RequestBody BookRequestDto dto) {
		return ResponseEntity.ok(bookService.updateBook(id, dto));
	}

	// DELETE /api/jpa/books/{id}                                 도서삭제
	@DeleteMapping("/{id}")
	public ResponseEntity<Long> deleteBook(@PathVariable("id") Long id) {
		bookService.deleteBook(id);
		return ResponseEntity.ok(id);
	}

	// POST /api/jpa/books/{id}/like?userId=1                     찜(좋아요) 등록
	@PostMapping("/{id}/like")
	public ResponseEntity<Void> likeBook(
			@PathVariable("id") Long bookId,
			@RequestParam("userId") Long userId) {
		bookService.likeBook(userId, bookId);
		return ResponseEntity.ok().build();
	}

	// DELETE /api/jpa/books/{id}/like?userId=1                   찜(좋아요) 취소
	@DeleteMapping("/{id}/like")
	public ResponseEntity<Void> unlikeBook(
			@PathVariable("id") Long bookId,
			@RequestParam("userId") Long userId) {
		bookService.unlikeBook(userId, bookId);
		return ResponseEntity.ok().build();
	}
}
// - GET    /api/jpa/books/category/{category}   카테고리별 페이징조회  ※기능: bookService.getBooksByCategory
// - GET    /api/jpa/books/search                제목/저자 통합검색     ※기능: bookService.searchBooks
// - GET    /api/jpa/books/ranking/liked         찜많은순 랭킹          ※기능: bookService.getTopLikedBooks
// - GET    /api/jpa/books/{id}                  도서 단건조회          ※기능: bookService.getBookById
// - POST   /api/jpa/books                       도서등록               ※기능: bookService.createBook
// - PATCH  /api/jpa/books/{id}                  도서수정               ※기능: bookService.updateBook
// - DELETE /api/jpa/books/{id}                  도서삭제               ※기능: bookService.deleteBook
// - POST   /api/jpa/books/{id}/like             찜(좋아요) 등록         ※기능: bookService.likeBook
// - DELETE /api/jpa/books/{id}/like             찜(좋아요) 취소         ※기능: bookService.unlikeBook
