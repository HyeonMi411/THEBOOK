package com.the703.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.the703.api.BookKakaoDto;
import com.the703.dao.BookDao;
import com.the703.dto.BookDto;
import com.the703.util.UtilUpload;

@Service
public class BookServiceImpl implements BookService {

    @Autowired private BookDao bookDao;
    @Autowired private UtilUpload upload;

    // ---------------------------------------------------------
    // 📘 전체 조회 (Oracle OFFSET/FETCH 페이징)
    // ---------------------------------------------------------
    @Override
    public List<BookDto> findAll(int pstartno) {

        HashMap<String, Object> map = new HashMap<>();
        map.put("start", (pstartno - 1) * 10);
        map.put("end", 10);

        return bookDao.findAll(map);
    }

    @Override
    public int findAllCnt() {
        return bookDao.findAllCnt();
    }

    // ---------------------------------------------------------
    // 📘 등록 (파일 업로드 포함)
    // ---------------------------------------------------------
    @Override
    public int insert(BookDto dto, MultipartFile file) {

        String fileName = "default.png";

        if (file != null && !file.isEmpty()) {
            try {
                fileName = upload.fileUpload(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        dto.setBookCover(fileName);

        return bookDao.insert(dto);
    }

    // ---------------------------------------------------------
    // 📘 수정 (파일 업로드 포함)
    // ---------------------------------------------------------
    @Override
    public int edit(BookDto dto, MultipartFile file) {

        String fileName = dto.getBookCover();

        if (file != null && !file.isEmpty()) {
            try {
                fileName = upload.fileUpload(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        dto.setBookCover(fileName);

        return bookDao.update(dto);
    }

    // ---------------------------------------------------------
    // 📘 삭제
    // ---------------------------------------------------------
    @Override
    public int delete(BookDto dto) {
        return bookDao.delete(dto);
    }

    // ---------------------------------------------------------
    // 📘 상세 조회
    // ---------------------------------------------------------
    @Override
    public BookDto detail(BookDto dto) {
        return bookDao.findById(dto);
    }

    // ---------------------------------------------------------
    // 📘 수정 화면 조회
    // ---------------------------------------------------------
    @Override
    public BookDto editView(BookDto dto) {
        return bookDao.findById(dto);
    }

    // ---------------------------------------------------------
    // ⭐ 카테고리별 조회 (핵심)
    // ---------------------------------------------------------
    @Override
    public List<BookDto> findByCategory(String category, int pstartno) {

        HashMap<String, Object> map = new HashMap<>();
        map.put("category", category);
        map.put("start", (pstartno - 1) * 10);
        map.put("end", 10);

        return bookDao.findByCategory(map);
    }

    @Override
    public int findCategoryCnt(String category) {
        return bookDao.findCategoryCnt(category);
    }

    // ---------------------------------------------------------
    // 🔍 통합 검색 (제목/저자/카테고리)
    // ---------------------------------------------------------
    @Override
    public List<BookDto> searchBooks(BookDto dto) {
        return bookDao.searchBooks(dto);
    }

    
    
  
    
    
    
    // ---------------------------------------------------------
    // 국립중앙도서관 API → BOOK 테이블 저장
    // ---------------------------------------------------------
    @Override
    public int insertFromNlApi(List<Map<String, Object>> apiResult) {

        int cnt = 0;

        for (Map<String, Object> b : apiResult) {

            BookDto dto = new BookDto();
            dto.setTitle((String) b.get("title_info"));
            dto.setAuthor((String) b.get("author_info"));
            dto.setPublisher((String) b.get("pub_info"));
            dto.setPublishDate((String) b.get("pub_year_info"));
            dto.setCategory("국립중앙도서관");
            dto.setRanking(null);
            dto.setReviewCount(0);
            dto.setRating(0.0);
            dto.setDescription((String) b.get("subject_info"));
            dto.setPages(null);
            dto.setPrice(null);
            dto.setBookCover(null);

        //    cnt += bookDao.insert(dto);
        }

        return cnt;
    }

    // ---------------------------------------------------------
    // ⚠️ 중복 검사
    // ---------------------------------------------------------
    @Override
    public boolean isDuplicate(BookDto dto) {

        List<BookDto> list = bookDao.searchBooks(dto);

        for (BookDto b : list) {
            if (b.getTitle().equals(dto.getTitle())) return true;
        }

        return false;
    }
    // ---------------------------------------------------------
    // ⚠️  Book Kakao
    // ---------------------------------------------------------
	@Override
	public int insert(List<BookKakaoDto> result) {
		int cnt = 0;

	    for (BookKakaoDto kakaoBook : result) {
	        // 1. DTO 변환 (BookKakaoDto -> BookDto)
	        BookDto bookDto = new BookDto();
	        
	        bookDto.setTitle(kakaoBook.getTitle());
	        
	        // authors 리스트를 쉼표로 연결하여 하나의 문자열로 변환
	        if (kakaoBook.getAuthors() != null && !kakaoBook.getAuthors().isEmpty()) {
	            bookDto.setAuthor(String.join(", ", kakaoBook.getAuthors()));
	        } else {
	            bookDto.setAuthor("미상");
	        }
	        
	        bookDto.setPublisher(kakaoBook.getPublisher());
	        
	        // 날짜 형식 가공 (ISO8601의 앞 10자리 YYYY-MM-DD만 추출)
	        String date = kakaoBook.getDatetime();
	        if (date != null && date.length() >= 10) {
	            bookDto.setPublishDate(date.substring(0, 10));
	        } else {
	            bookDto.setPublishDate("1900-01-01"); // 기본값 설정
	        }
	        
	        // 핵심: 출판사 정보가 null이거나 비어있으면 기본값 "미상" 처리
	        String publisher = kakaoBook.getPublisher();
	        if (publisher == null || publisher.trim().isEmpty()) {
	            bookDto.setPublisher("미상");
	        } else {
	            bookDto.setPublisher(publisher);
	        }
	        
	        // 저자도 NOT NULL이라면 동일하게 처리
	        String author = (kakaoBook.getAuthors() != null && !kakaoBook.getAuthors().isEmpty()) 
	                        ? String.join(", ", kakaoBook.getAuthors()) 
	                        : "미상";
	        bookDto.setAuthor(author);
	        
	        
	        bookDto.setCategory("카카오검색"); // 또는 적절한 기본값
	        bookDto.setRanking(kakaoBook.getRanking());
	        bookDto.setReviewCount(kakaoBook.getReviewCount() != null ? kakaoBook.getReviewCount() : 0);
	        bookDto.setRating(kakaoBook.getRating() != null ? kakaoBook.getRating() : 0.0);
	        bookDto.setDescription(kakaoBook.getContents());
	        bookDto.setPages(kakaoBook.getPages());
	        bookDto.setPrice(kakaoBook.getPrice());
	        bookDto.setBookCover(kakaoBook.getThumbnail());

	        // 2. DAO를 통해 DB 저장
	        cnt += bookDao.insert(bookDto);
	    }

	    return cnt;
	}
}
