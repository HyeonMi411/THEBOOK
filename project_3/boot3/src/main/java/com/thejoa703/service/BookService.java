package com.thejoa703.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.api.ApiKakaoBook;
import com.thejoa703.api.BookKakaoDto;
import com.thejoa703.api.BookNlDto;
import com.thejoa703.api.NlBookApiService;
import com.thejoa703.dto.BookDto.BookRequestDto;
import com.thejoa703.dto.BookDto.BookResponseDto;
import com.thejoa703.dto.BookDto.StockUpdateRequestDto;
import com.thejoa703.dto.PageResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Book;
import com.thejoa703.entity.BookStock;
import com.thejoa703.exception.ResourceNotFoundException;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.BookRepository;
import com.thejoa703.repository.BookStockRepository;
import com.thejoa703.util.FileStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // ##
public class BookService {

	private final BookRepository      bookRepository;
	private final BookStockRepository bookStockRepository; // ★재고 관리
	private final AppUserRepository   appUserRepository;
	private final FileStorageService  fileStorageService; // 표지이미지 업로드처리
	private final ApiKakaoBook        apiKakaoBook;        // ★카카오 도서검색 API
	private final NlBookApiService    nlBookApiService;    // ★국립중앙도서관 도서검색 API

	private static final int DEFAULT_PAGE_SIZE = 12; // ★화면에 12개씩

	// 1. 전체조회 (최신순) - 목록 전체(비페이징, 내부용/구버전 호환용)
	public List<BookResponseDto> getAllBooks() {
		return bookRepository.findAllByOrderByIdDesc().stream()
				.map(BookResponseDto::from)
				.collect(Collectors.toList());
	}	

	// 1-1. ★전체조회 - 페이징(화면 12개씩) + 카테고리 선택필터
	public PageResponseDto<BookResponseDto> getAllBooksPaged(int page, int size, String category) {
		int currentPage = Math.max(page, 1);       // 1보다 작으면 1페이지로 보정
		int pageSize     = size > 0 ? size : DEFAULT_PAGE_SIZE;
		Pageable pageable = PageRequest.of(currentPage - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));

		Page<Book> result = (category != null && !category.isBlank())
				? bookRepository.findByCategoryOrderByIdDesc(category, pageable)
				: bookRepository.findAllByOrderByIdDesc(pageable);

		List<BookResponseDto> content = result.getContent().stream()
				.map(BookResponseDto::from)
				.collect(Collectors.toList());

		return new PageResponseDto<>(content, currentPage, pageSize, result.getTotalElements(), result.getTotalPages());
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

	// 7. ★카카오 도서검색 API에서 가져와 DB에 자동저장 ( ★관리자 전용 )
	//    boot1(the703) 의 BookController.kakaoinsert() + BookServiceImpl.insert() 를 그대로 재현했습니다.
	//    (검색버튼을 누르면 카카오 API 에서 도서를 가져와 자동으로 DB에 저장한 후, 도서 목록 페이지로 이동)
	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public int insertFromKakao(String search, Long adminUserId) {
		AppUser admin = appUserRepository.findById(adminUserId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다. ID : " + adminUserId));

		List<BookKakaoDto> kakaoBooks = apiKakaoBook.getBooks(search);
		int cnt = 0;

		for (BookKakaoDto kakaoBook : kakaoBooks) {
			String title = kakaoBook.getTitle();
			if (title == null || title.isBlank()) { continue; }

			// 이미 등록된 제목이면 중복저장 방지 (boot1 원본에는 없던 안전장치를 추가했습니다)
			if (bookRepository.existsByTitle(title)) { continue; }

			Book book = new Book();
			book.setTitle(title);

			String author = (kakaoBook.getAuthors() != null && !kakaoBook.getAuthors().isEmpty())
					? String.join(", ", kakaoBook.getAuthors())
					: "미상";
			book.setAuthor(author);

			String publisher = kakaoBook.getPublisher();
			book.setPublisher((publisher == null || publisher.isBlank()) ? "미상" : publisher);

			// 날짜 형식 가공 (ISO8601의 앞 10자리 YYYY-MM-DD만 추출)
			String date = kakaoBook.getDatetime();
			LocalDate publishDate;
			try {
				publishDate = (date != null && date.length() >= 10)
						? LocalDate.parse(date.substring(0, 10))
						: LocalDate.of(1900, 1, 1);
			} catch (Exception e) {
				publishDate = LocalDate.of(1900, 1, 1); // 파싱 실패시 기본값
			}
			book.setPublishDate(publishDate);

			book.setCategory("카카오검색");
			book.setDescription(kakaoBook.getContents());
			book.setPrice(kakaoBook.getPrice());
			book.setBookCover(
					(kakaoBook.getThumbnail() != null && !kakaoBook.getThumbnail().isBlank())
							? kakaoBook.getThumbnail()
							: "uploads/default_book.png"
			);
			book.setUser(admin); // 등록한 관리자로 저장

			bookRepository.save(book);
			cnt++;
		}

		return cnt;
	}

	// 8. ★국립중앙도서관 도서검색 ( 조회전용, DB에 저장하지 않음 - 누구나 검색 가능 )
	//    boot1(the703) 의 BookController.searchNl()/searchNlCategory() 를 그대로 재현했습니다.
	//    (목록화면에서 키워드 또는 KDC 분류로 검색 → 화면에 결과를 보여줌)
	public List<BookNlDto> searchNationalLibrary(String keyword, int page) {
		return nlBookApiService.search(keyword, page);
	}

	// 9. ★국립중앙도서관 검색결과 중 선택한 도서 1권을 DB에 저장 ( ★관리자 전용 )
	//    boot1(the703) 의 BookController.saveFromApi() (/book/save) 를 그대로 재현했습니다.
	//    (상세페이지에서 "저장" 버튼을 누르면 그 도서만 DB에 저장)
	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public BookResponseDto saveNationalLibraryBook(BookNlDto nlBook, Long adminUserId) {
		AppUser admin = appUserRepository.findById(adminUserId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다. ID : " + adminUserId));

		String title = nlBook.getTitle_info();
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("도서 제목이 없어 저장할 수 없습니다.");
		}
		// 이미 등록된 제목이면 저장 거부 (boot1 원본에는 없던 안전장치를 추가했습니다)
		if (bookRepository.existsByTitle(title)) {
			throw new IllegalStateException("이미 등록된 도서입니다: " + title);
		}

		Book book = new Book();
		book.setTitle(title);
		book.setAuthor(blankToDefault(nlBook.getAuthor_info(), "미상"));
		book.setPublisher(blankToDefault(nlBook.getPub_info(), "미상"));
		book.setPublishDate(parseNlPublishDate(nlBook.getPub_year_info()));
		book.setCategory(blankToDefault(nlBook.getKdc_name_1s(), "국립중앙도서관검색"));
		book.setDescription(nlBook.getSubject_info());
		book.setBookCover(nlBook.getBookCover() != null ? nlBook.getBookCover() : "uploads/default_book.png");
		book.setUser(admin); // 저장한 관리자로 등록

		return BookResponseDto.from(bookRepository.save(book));
	}

	// 국립중앙도서관 pub_year_info("2021" 등 문자열)를 LocalDate 로 안전하게 변환
	private LocalDate parseNlPublishDate(String pubYearInfo) {
		try {
			if (pubYearInfo == null || pubYearInfo.isBlank()) {
				return LocalDate.of(1900, 1, 1);
			}
			String digits = pubYearInfo.replaceAll("[^0-9]", "");
			if (digits.length() < 4) {
				return LocalDate.of(1900, 1, 1);
			}
			int year = Integer.parseInt(digits.substring(0, 4));
			return LocalDate.of(year, 1, 1);
		} catch (Exception e) {
			return LocalDate.of(1900, 1, 1); // 파싱 실패시 기본값
		}
	}

	private String blankToDefault(String value, String defaultValue) {
		return (value == null || value.isBlank()) ? defaultValue : value;
	}

	// ★재고 수정 ( 관리자 전용 ) - BookStock 이 아직 없으면 새로 만들고, 있으면 값만 갱신
	//   Swagger 에서 결제기능(장바구니/주문/결제)을 테스트하려면 재고가 있어야 하므로,
	//   테스트 전에 이 API로 원하는 도서의 재고를 먼저 세팅해주세요.
	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public BookResponseDto updateStock(Long bookId, StockUpdateRequestDto dto) {
		Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 도서입니다. ID : " + bookId));

		BookStock stock = bookStockRepository.findById(bookId).orElse(null);
		if (stock == null) {
			stock = new BookStock();
			stock.setBook(book); // ★같은 트랜잭션 안이므로 book 이 매니지드 상태 - @MapsId 안전
			stock.setStockQuantity(dto.getStockQuantity());
			bookStockRepository.save(stock);
			book.setStock(stock); // ★양방향 동기화
		} else {
			stock.setStockQuantity(dto.getStockQuantity());
			bookStockRepository.save(stock);
		}

		return BookResponseDto.from(book);
	}
}
