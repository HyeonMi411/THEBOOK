package com.thejoa703.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.dto.BookDto.BookRequestDto;
import com.thejoa703.dto.BookDto.BookResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Book;
import com.thejoa703.exception.ResourceNotFoundException;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.BookRepository;
import com.thejoa703.util.FileStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // ##
public class BookService {

	private final BookRepository     bookRepository;
	private final AppUserRepository  appUserRepository;
	private final FileStorageService fileStorageService; // 표지이미지 업로드처리

	// 1. 전체조회 (카테고리 필터)
	public List<BookResponseDto> getAllBooks(String category) {
		List<Book> books = (category != null && !category.isBlank())
				? bookRepository.findByCategoryOrderByIdDesc(category)
				: bookRepository.findAll();
		return books.stream().map(BookResponseDto::from).collect(Collectors.toList());
	}

	// 2. 단건조회
	public BookResponseDto getBook(Long id) {
		Book book = bookRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 도서입니다. ID : " + id));
		return BookResponseDto.from(book);
	}

	// 3. 제목검색
	public List<BookResponseDto> searchByTitle(String keyword) {
		return bookRepository.findByTitleContainingOrderByIdDesc(keyword).stream()
				.map(BookResponseDto::from)
				.collect(Collectors.toList());
	}

	// 4. 도서등록 ( ★관리자 전용 )
	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public BookResponseDto createBook(Long userId, BookRequestDto dto, MultipartFile cover) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다. ID : " + userId));

		Book book = new Book();
		book.setTitle(dto.getTitle());
		book.setAuthor(dto.getAuthor());
		book.setPublisher(dto.getPublisher());
		book.setPublishDate(dto.getPublishDate());
		book.setCategory(dto.getCategory());
		book.setRanking(dto.getRanking());
		book.setReviewCount(dto.getReviewCount() != null ? dto.getReviewCount() : 0);
		book.setRating(dto.getRating());
		book.setDescription(dto.getDescription());
		book.setPages(dto.getPages());
		book.setPrice(dto.getPrice());
		book.setBookCover(
				cover != null && !cover.isEmpty()
						? fileStorageService.upload(cover)
						: "uploads/default_book.png"
		);
		book.setUser(user);

		return BookResponseDto.from(bookRepository.save(book));
	}

	// 5. 도서수정 ( ★관리자 전용 - 더티체킹으로 update 반영 )
	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public BookResponseDto updateBook(Long bookId, BookRequestDto dto, MultipartFile cover) {
		Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 도서입니다. ID : " + bookId));

		book.setTitle(dto.getTitle());
		book.setAuthor(dto.getAuthor());
		book.setPublisher(dto.getPublisher());
		book.setPublishDate(dto.getPublishDate());
		book.setCategory(dto.getCategory());
		book.setRanking(dto.getRanking());
		if (dto.getReviewCount() != null) { book.setReviewCount(dto.getReviewCount()); }
		book.setRating(dto.getRating());
		book.setDescription(dto.getDescription());
		book.setPages(dto.getPages());
		book.setPrice(dto.getPrice());

		if (cover != null && !cover.isEmpty()) {
			book.setBookCover(fileStorageService.upload(cover));
		}
		return BookResponseDto.from(book); // 더티체킹(Dirty Checking)으로 자동 update
	}

	// 6. 도서삭제 ( ★관리자 전용 )
	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public void deleteBook(Long bookId) {
		if (!bookRepository.existsById(bookId)) {
			throw new ResourceNotFoundException("존재하지 않는 도서입니다. ID : " + bookId);
		}
		bookRepository.deleteById(bookId);
	}
}
