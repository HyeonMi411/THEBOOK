package com.thejoa703.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.api.ApiKakaoBook;
import com.thejoa703.api.BookKakaoDto;
import com.thejoa703.api.BookNlDto;
import com.thejoa703.api.NlBookApiService;
import com.thejoa703.dto.BookDto.BestsellerBookDto;
import com.thejoa703.dto.BookDto.BookRequestDto;
import com.thejoa703.dto.BookDto.BookResponseDto;
import com.thejoa703.dto.BookDto.StockUpdateRequestDto;
import com.thejoa703.dto.PageResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Book;
import com.thejoa703.entity.BookStock;
import com.thejoa703.exception.ResourceNotFoundException;
import com.thejoa703.mapper.BookMapper;
import com.thejoa703.mapper.OrderItemMapper;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.BookStockRepository;
import com.thejoa703.util.FileStorageService;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

	private final BookMapper              bookMapper;
	private final BookStockRepository     bookStockRepository;
	private final AppUserRepository       appUserRepository;
	private final OrderItemMapper         orderItemMapper;
	private final RedisTemplate<String, Object> redisTemplate; // 베스트셀러 캐싱용
	private final FileStorageService      fileStorageService;
	private final ApiKakaoBook            apiKakaoBook;
	private final NlBookApiService        nlBookApiService;

	private static final int DEFAULT_PAGE_SIZE = 12;
	private static final int BESTSELLER_TOP_N = 10;
	private static final String BESTSELLER_CACHE_KEY = "book:bestsellers:top10";
	private static final long BESTSELLER_CACHE_TTL_SECONDS = 600; // 10분

	public List<BookResponseDto> getAllBooks() {
		Map<String, Object> params = new HashMap<>();
		params.put("start", 0);
		params.put("end", Integer.MAX_VALUE);
		return bookMapper.findAll(params).stream()
				.map(BookResponseDto::from)
				.collect(Collectors.toList());
	}

	public PageResponseDto<BookResponseDto> getAllBooksPaged(int page, int size, String category) {
		int currentPage = Math.max(page, 1);
		int pageSize = size > 0 ? size : DEFAULT_PAGE_SIZE;
		int start = (currentPage - 1) * pageSize;

		List<Book> books;
		int totalElements;
		if (category != null && !category.isBlank()) {
			Map<String, Object> params = new HashMap<>();
			params.put("category", category);
			params.put("start", start);
			params.put("end", pageSize);
			books = bookMapper.findByCategory(params);
			totalElements = bookMapper.findCategoryCnt(category);
		} else {
			Map<String, Object> params = new HashMap<>();
			params.put("start", start);
			params.put("end", pageSize);
			books = bookMapper.findAll(params);
			totalElements = bookMapper.findAllCnt();
		}

		List<BookResponseDto> content = books.stream()
				.map(BookResponseDto::from)
				.collect(Collectors.toList());
		int totalPages = (int) Math.ceil((double) totalElements / pageSize);

		return new PageResponseDto<>(content, currentPage, pageSize, totalElements, totalPages);
	}

	public BookResponseDto getBook(Long id) {
		Book book = bookMapper.findById(id);
		if (book == null || book.isDeleted()) {
			throw new ResourceNotFoundException("존재하지 않는 도서입니다. ID : " + id);
		}
		return BookResponseDto.from(book);
	}

	public List<BookResponseDto> searchByTitle(String keyword) {
		String cleaned = cleanKeyword(keyword);
		Map<String, Object> params = new HashMap<>();
		params.put("searchType", "title");
		params.put("keyword", cleaned);
		return bookMapper.searchBooks(params).stream()
				.map(BookResponseDto::from)
				.collect(Collectors.toList());
	}

	private String cleanKeyword(String keyword) {
		if (keyword == null) { return ""; }
		return keyword.trim().replaceAll("\\s+", " ");
	}

	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public BookResponseDto createBook(Long userId, BookRequestDto dto, MultipartFile cover) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다. ID : " + userId));

		Map<String, Object> params = new HashMap<>();
		params.put("title", dto.getTitle());
		params.put("author", dto.getAuthor());
		params.put("publisher", dto.getPublisher());
		params.put("publishDate", dto.getPublishDate());
		params.put("category", dto.getCategory());
		params.put("ranking", dto.getRanking());
		params.put("reviewCount", dto.getReviewCount() != null ? dto.getReviewCount() : 0);
		params.put("rating", dto.getRating());
		params.put("description", dto.getDescription());
		params.put("pages", dto.getPages());
		params.put("price", dto.getPrice());
		params.put("bookCover", cover != null && !cover.isEmpty()
				? fileStorageService.upload(cover)
				: "uploads/default_book.png");
		params.put("appUserId", user.getId());

		bookMapper.insert(params); // selectKey 로 채번된 bookId 가 params 에 다시 채워짐
		Long newId = (Long) params.get("bookId");
		return getBook(newId);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public BookResponseDto updateBook(Long bookId, BookRequestDto dto, MultipartFile cover) {
		Book book = bookMapper.findById(bookId);
		if (book == null) {
			throw new ResourceNotFoundException("존재하지 않는 도서입니다. ID : " + bookId);
		}

		Map<String, Object> params = new HashMap<>();
		params.put("bookId", bookId);
		params.put("title", dto.getTitle());
		params.put("author", dto.getAuthor());
		params.put("publisher", dto.getPublisher());
		params.put("publishDate", dto.getPublishDate());
		params.put("category", dto.getCategory());
		params.put("ranking", dto.getRanking());
		params.put("reviewCount", dto.getReviewCount());
		params.put("rating", dto.getRating());
		params.put("description", dto.getDescription());
		params.put("pages", dto.getPages());
		params.put("price", dto.getPrice());
		if (cover != null && !cover.isEmpty()) {
			params.put("bookCover", fileStorageService.upload(cover));
		}

		bookMapper.update(params);
		return getBook(bookId);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public void deleteBook(Long bookId) {
		Book book = bookMapper.findById(bookId);
		if (book == null || book.isDeleted()) {
			throw new ResourceNotFoundException("존재하지 않는 도서입니다. ID : " + bookId);
		}
		// 소프트 삭제 - 실제로 지우지 않고 DELETED 플래그만 세웁니다. CART_ITEM/ORDER_ITEMS 가
		// BOOK_ID 를 FK 로 참조하고 있어서, 장바구니에 담겼거나 한 번이라도 주문된 도서를 하드
		// 삭제하면 FK 제약조건 위반(ORA-02292)이 발생합니다. 재고(BOOK_STOCK)도 그대로 둡니다
		// (판매내역 통계 등에서 필요할 수 있고, 삭제된 도서는 목록/검색에서 어차피 제외됩니다).
		bookMapper.updateDeleted(bookId, true);
	}

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
			if (bookMapper.existsByTitle(title)) { continue; }

			String author = (kakaoBook.getAuthors() != null && !kakaoBook.getAuthors().isEmpty())
					? String.join(", ", kakaoBook.getAuthors())
					: "미상";

			String publisher = kakaoBook.getPublisher();

			String date = kakaoBook.getDatetime();
			// 파싱에 실패하면 null 로 둡니다("출판일 미상"). 예전에는 1900-01-01 같은
			// 매직넘버를 채워넣었는데, 이건 실제 1900년도 도서와 구분이 안 되는 잘못된
			// 정보라 그냥 비워두는 게 정직합니다.
			LocalDate publishDate = null;
			try {
				if (date != null && date.length() >= 10) {
					publishDate = LocalDate.parse(date.substring(0, 10));
				}
			} catch (Exception e) {
				publishDate = null;
			}

			Map<String, Object> params = new HashMap<>();
			params.put("title", title);
			params.put("author", author);
			params.put("publisher", (publisher == null || publisher.isBlank()) ? "미상" : publisher);
			params.put("publishDate", publishDate);
			params.put("category", "카카오검색");
			params.put("ranking", null);
			params.put("reviewCount", 0);
			params.put("rating", null);
			params.put("description", kakaoBook.getContents());
			params.put("pages", null);
			params.put("price", kakaoBook.getPrice());
			params.put("bookCover",
					(kakaoBook.getThumbnail() != null && !kakaoBook.getThumbnail().isBlank())
							? kakaoBook.getThumbnail()
							: "uploads/default_book.png");
			params.put("appUserId", admin.getId());

			bookMapper.insert(params);
			cnt++;
		}

		return cnt;
	}

	public List<BookNlDto> searchNationalLibrary(String keyword, int page) {
		return nlBookApiService.search(keyword, page);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public BookResponseDto saveNationalLibraryBook(BookNlDto nlBook, Long adminUserId) {
		AppUser admin = appUserRepository.findById(adminUserId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다. ID : " + adminUserId));

		String title = nlBook.getTitle_info();
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("도서 제목이 없어 저장할 수 없습니다.");
		}
		if (bookMapper.existsByTitle(title)) {
			throw new IllegalStateException("이미 등록된 도서입니다: " + title);
		}

		Map<String, Object> params = new HashMap<>();
		params.put("title", title);
		params.put("author", blankToDefault(nlBook.getAuthor_info(), "미상"));
		params.put("publisher", blankToDefault(nlBook.getPub_info(), "미상"));
		params.put("publishDate", parseNlPublishDate(nlBook.getPub_year_info()));
		params.put("category", blankToDefault(nlBook.getKdc_name_1s(), "국립중앙도서관검색"));
		params.put("ranking", null);
		params.put("reviewCount", 0);
		params.put("rating", null);
		params.put("description", nlBook.getSubject_info());
		params.put("pages", null);
		params.put("price", null);
		params.put("bookCover", nlBook.getBookCover() != null ? nlBook.getBookCover() : "uploads/default_book.png");
		params.put("appUserId", admin.getId());

		bookMapper.insert(params);
		Long newId = (Long) params.get("bookId");
		return getBook(newId);
	}

	// 국립중앙도서관 원본의 출판연도 정보가 없거나 파싱에 실패하면 null 을 반환합니다
	// ("출판일 미상"). 1900-01-01 같은 매직넘버는 실제 1900년도 도서와 구분이 안 되는
	// 잘못된 정보라 쓰지 않습니다.
	private LocalDate parseNlPublishDate(String pubYearInfo) {
		try {
			if (pubYearInfo == null || pubYearInfo.isBlank()) {
				return null;
			}
			String digits = pubYearInfo.replaceAll("[^0-9]", "");
			if (digits.length() < 4) {
				return null;
			}
			int year = Integer.parseInt(digits.substring(0, 4));
			return LocalDate.of(year, 1, 1);
		} catch (Exception e) {
			return null;
		}
	}

	private String blankToDefault(String value, String defaultValue) {
		return (value == null || value.isBlank()) ? defaultValue : value;
	}

	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public BookResponseDto updateStock(Long bookId, StockUpdateRequestDto dto) {
		Book book = bookMapper.findById(bookId);
		if (book == null) {
			throw new ResourceNotFoundException("존재하지 않는 도서입니다. ID : " + bookId);
		}

		BookStock stock = bookStockRepository.findById(bookId).orElse(null);
		if (stock == null) {
			BookStock newStock = new BookStock();
			newStock.setBook(book); // @MapsId - book 의 PK 가 그대로 BookStock 의 PK 로 채워짐
			newStock.setStockQuantity(dto.getStockQuantity());
			bookStockRepository.save(newStock);
		} else {
			stock.setStockQuantity(dto.getStockQuantity());
			try {
				// saveAndFlush 로 즉시 반영시켜서, 낙관적 락(@Version) 충돌을 이 시점에 바로 감지합니다.
				bookStockRepository.saveAndFlush(stock);
			} catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
				throw new IllegalStateException("다른 요청이 먼저 재고를 변경했습니다. 다시 시도해주세요.");
			}
		}

		return getBook(bookId);
	}

	// 베스트셀러(판매량 TOP 10) 조회 - Redis 캐시 우선, 없으면 DB 집계 후 캐싱
	// 결제완료(PAID) 주문만 집계하므로 결제전/취소/실패 주문은 랭킹에 영향을 주지 않습니다.
	@SuppressWarnings("unchecked")
	public List<BestsellerBookDto> getBestsellers() {
		List<BestsellerBookDto> cached = (List<BestsellerBookDto>) redisTemplate.opsForValue().get(BESTSELLER_CACHE_KEY);
		if (cached != null) {
			return cached;
		}

		List<Map<String, Object>> rows = orderItemMapper.findBestSellerBookIds(BESTSELLER_TOP_N);
		List<BestsellerBookDto> result = new ArrayList<>();
		int rank = 1;
		for (Map<String, Object> row : rows) {
			Long bookId = ((Number) row.get("BOOK_ID")).longValue();
			Long soldQuantity = ((Number) row.get("TOTAL_QTY")).longValue();
			Book book = bookMapper.findById(bookId);
			if (book == null || book.isDeleted()) { continue; } // 집계 이후 삭제된 도서는 건너뜀

			BestsellerBookDto dto = new BestsellerBookDto();
			dto.setRank(rank++);
			dto.setSoldQuantity(soldQuantity);
			dto.setBook(BookResponseDto.from(book));
			result.add(dto);
		}

		redisTemplate.opsForValue().set(BESTSELLER_CACHE_KEY, result, BESTSELLER_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
		return result;
	}

	// 결제가 새로 완료되면(PaymentService.approve) 캐시가 낡아지므로 여기서 무효화합니다.
	// TTL(10분)이 지나면 자동으로도 사라지지만, 결제 직후 바로 최신 랭킹을 반영하기 위해
	// 명시적으로 지웁니다.
	public void evictBestsellerCache() {
		redisTemplate.delete(BESTSELLER_CACHE_KEY);
	}
}
