package com.thejoa703.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.dto.BookDto.BookRequestDto;
import com.thejoa703.dto.BookDto.BookResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Book;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.BookRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 더티체킹, 읽기전용 트랜잭션
public class BookService {

	private final BookRepository    bookRepository;
	private final AppUserRepository appUserRepository;   // 찜하기(ManyToMany) 처리용

	// 1. 오라클 네이티브 페이징 - 카테고리별 조회
	public List<BookResponseDto> getBooksByCategory(String category, int start, int end) {
		return bookRepository.findByCategoryWithPaging(category, start, end).stream()
				.map(BookResponseDto::from)
				.collect(Collectors.toList());
	}

	// 2. 오라클 네이티브 페이징 - 제목/저자 통합검색
	public List<BookResponseDto> searchBooks(String keyword, int start, int end) {
		return bookRepository.searchBooksWithPaging(keyword, start, end).stream()
				.map(BookResponseDto::from)
				.collect(Collectors.toList());
	}

	// 3. 단건조회
	public BookResponseDto getBookById(Long bookId) {
		Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도서입니다. ID:" + bookId));
		return BookResponseDto.from(book);
	}

	// 4. 도서 등록
	@Transactional
	public BookResponseDto createBook(BookRequestDto dto) {
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
		book.setBookCover(dto.getBookCover());
		return BookResponseDto.from(bookRepository.save(book));
	}

	// 5. 도서 수정 (더티체킹으로 update 쿼리 반영)
	@Transactional
	public BookResponseDto updateBook(Long bookId, BookRequestDto dto) {
		Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도서입니다. ID:" + bookId));

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
		book.setBookCover(dto.getBookCover());
		return BookResponseDto.from(book);  // save() 호출없이 update 쿼리 반영
	}

	// 6. 도서 삭제
	@Transactional
	public void deleteBook(Long bookId) {
		Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도서입니다. ID:" + bookId));
		bookRepository.delete(book);
	}

	// 7. 찜(좋아요) 등록  - AppUser <-> Book  ManyToMany
	@Transactional
	public void likeBook(Long userId, Long bookId) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. ID:" + userId));
		Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도서입니다. ID:" + bookId));

		boolean alreadyLiked = user.getLikedBooks().stream()
				.anyMatch(b -> b.getBookId().equals(bookId));
		if (!alreadyLiked) {
			user.getLikedBooks().add(book);
		}
	}

	// 8. 찜(좋아요) 취소
	@Transactional
	public void unlikeBook(Long userId, Long bookId) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. ID:" + userId));
		user.getLikedBooks().removeIf(b -> b.getBookId().equals(bookId));
	}

	// 9. 찜(좋아요) 많이받은 책 랭킹 TOP N
	public List<BookResponseDto> getTopLikedBooks(int topN) {
		return bookRepository.findTopLikedBooks(topN).stream()
				.map(BookResponseDto::from)
				.collect(Collectors.toList());
	}
}
