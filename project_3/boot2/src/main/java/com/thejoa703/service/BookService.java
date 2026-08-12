package com.thejoa703.service;

import java.util.List;
import java.util.stream.Collectors;

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
@Transactional(readOnly = true)  // 조회는 읽기전용 / 등록·수정·삭제는 메서드에 @Transactional 재선언
public class BookService {

	private final BookRepository     bookRepository;
	private final AppUserRepository  appUserRepository;
	private final FileStorageService fileStorageService;   // 도서 표지 업로드처리

	// ------------------------------------------------------------
	// ★관리자 권한 검증 공통 메서드 - 도서등록/수정/삭제는 관리자만 가능
	// ------------------------------------------------------------
	private AppUser validateAdmin(Long adminUserId) {
		AppUser user = appUserRepository.findById(adminUserId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다. ID:" + adminUserId));

		if (!"ROLE_ADMIN".equals(user.getRole())) {
			throw new IllegalArgumentException("도서 등록/수정/삭제는 관리자만 가능합니다.");
		}
		return user;
	}

	// 1. 전체조회 (최신등록순)
	public List<BookResponseDto> getAllBooks() {
		return bookRepository.findAllByOrderByRegDateDesc().stream()
				.map(BookResponseDto::fromEntity)
				.collect(Collectors.toList());
	}

	// 2. 오라클 네이티브 페이징조회
	public List<BookResponseDto> getBooksPaged(int start, int end) {
		return bookRepository.findBooksWithPaging(start, end).stream()
				.map(BookResponseDto::fromEntity)
				.collect(Collectors.toList());
	}

	// 3. 단건조회
	public BookResponseDto getBookById(Long id) {
		Book book = bookRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 도서입니다. ID:" + id));
		return BookResponseDto.fromEntity(book);
	}

	// 4. 카테고리별 조회
	public List<BookResponseDto> getBooksByCategory(String category) {
		return bookRepository.findByCategory(category).stream()
				.map(BookResponseDto::fromEntity)
				.collect(Collectors.toList());
	}

	// 5. 카테고리별 페이징 조회
	public List<BookResponseDto> getBooksByCategoryPaged(String category, int start, int end) {
		return bookRepository.findBooksByCategoryWithPaging(category, start, end).stream()
				.map(BookResponseDto::fromEntity)
				.collect(Collectors.toList());
	}

	// 6. 통합검색 (제목/저자/카테고리)
	public List<BookResponseDto> searchBooks(String searchType, String keyword) {
		return bookRepository.searchBooks(searchType, keyword).stream()
				.map(BookResponseDto::fromEntity)
				.collect(Collectors.toList());
	}

	// 7. 도서명 중복검사(AJAX)
	public boolean existsByTitle(String title) {
		return bookRepository.existsByTitle(title);
	}

	// 8. ★관리자가 등록한 도서 목록
	public List<BookResponseDto> getBooksByAdmin(Long adminUserId) {
		return bookRepository.findByUser_Id(adminUserId).stream()
				.map(BookResponseDto::fromEntity)
				.collect(Collectors.toList());
	}

	// ------------------------------------------------------------
	// 9. 등록 (관리자 전용)
	// ------------------------------------------------------------
	@Transactional
	public BookResponseDto createBook(Long adminUserId, BookRequestDto dto, MultipartFile cover) {
		AppUser admin = validateAdmin(adminUserId);

		if (bookRepository.existsByTitle(dto.getTitle())) {
			throw new IllegalArgumentException("이미 등록된 도서명입니다.");
		}

		Book book = new Book();
		book.setTitle(dto.getTitle());
		book.setAuthor(dto.getAuthor());
		book.setPublisher(dto.getPublisher());
		book.setPublishDate(dto.getPublishDate());
		book.setCategory(dto.getCategory());
		book.setRanking(dto.getRanking());
		book.setReviewCount(dto.getReviewCount());
		book.setRating(dto.getRating());
		book.setDescription(dto.getDescription());
		book.setPages(dto.getPages());
		book.setPrice(dto.getPrice());
		book.setBookCover(cover != null && !cover.isEmpty()
				? fileStorageService.upload(cover)
				: "uploads/default_book_cover.png");
		book.setUser(admin);   // ★등록한 관리자와 연결

		return BookResponseDto.fromEntity(bookRepository.save(book));
	}

	// ------------------------------------------------------------
	// 10. 수정 (관리자 전용, 더티체킹)
	// ------------------------------------------------------------
	@Transactional
	public BookResponseDto updateBook(Long adminUserId, Long bookId, BookRequestDto dto, MultipartFile cover) {
		validateAdmin(adminUserId);

		Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 도서입니다. ID:" + bookId));

		book.setTitle(dto.getTitle());
		book.setAuthor(dto.getAuthor());
		book.setPublisher(dto.getPublisher());
		book.setPublishDate(dto.getPublishDate());
		book.setCategory(dto.getCategory());
		book.setRanking(dto.getRanking());
		book.setReviewCount(dto.getReviewCount());
		book.setRating(dto.getRating());
		book.setDescription(dto.getDescription());
		book.setPages(dto.getPages());
		book.setPrice(dto.getPrice());

		if (cover != null && !cover.isEmpty()) {
			book.setBookCover(fileStorageService.upload(cover));
		}
		return BookResponseDto.fromEntity(book);   // 저장메서드를 따로 호출하지 않아도 update 쿼리 반영(더티체킹)
	}

	// ------------------------------------------------------------
	// 11. 삭제 (관리자 전용)
	// ------------------------------------------------------------
	@Transactional
	public void deleteBook(Long adminUserId, Long bookId) {
		validateAdmin(adminUserId);

		if (!bookRepository.existsById(bookId)) {
			throw new ResourceNotFoundException("존재하지 않는 도서입니다. ID:" + bookId);
		}
		bookRepository.deleteById(bookId);
	}
}
