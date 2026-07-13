package com.the703.service;

import java.io.IOException;
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
    @Autowired private UtilUpload  upload;

    // ---------------------------------------------------------
    // 📘 전체 조회 (Oracle OFFSET/FETCH 페이징)
    // ---------------------------------------------------------
    @Override
    public List<BookDto> findAll(int pstartno) {

        HashMap<String, Object> map = new HashMap<>();
        map.put("start", (pstartno - 1) * 10);   // OFFSET
        map.put("end", 10);                      // FETCH NEXT

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


    // 카카오 API로 받은 도서 목록 저장 (필요 시)
//    @Override
//    public int apikakaoinsert(List<BookKakaoDto> api) {
//        int cnt = 0; 
//        for (BookKakaoDto k : api) { 
//            BookDto dto = new BookDto();
//            dto.setTitle(k.getTitle());
//            dto.setAuthors(k.getAuthors());  
//       
//            dto.setPublisher(k.getPublisher());
//            dto.setPublishDate(k.getDatetime().substring(0, 10));
//            dto.setCategory("API");
//            dto.setPrice(k.getPrice());            
//            dto.setDescription(k.getContents());
//            cnt += bookDao.insert(dto);
//        }
//        return cnt;
//    }
    @Override
    public int apikakaoinsert(List<BookKakaoDto> api) {
    	
    	System.out.println("................." + api);
    	
        int cnt = 0;
        for (BookKakaoDto k : api) {
            BookDto dto = new BookDto();
            
            // 1. 필수값 매핑 및 타입 처리
            dto.setTitle(k.getTitle());
            
            // authors는 List<String>이므로 DB의 VARCHAR2(100)에 맞게 콤마로 연결
            String authorStr = (k.getAuthors() != null) ? String.join(", ", k.getAuthors()) : "Unknown";
            // 길이에 따른 자르기 (DB 컬럼이 100자이므로 제한 필요)
            dto.setAuthor(authorStr.length() > 100 ? authorStr.substring(0, 97) + "..." : authorStr);
            
            dto.setPublisher(k.getPublisher() != null ? k.getPublisher() : "Unknown");
            
            // 2. 날짜 처리 (ISO 8601 문자열 -> DB Date)
            // 카카오 API 날짜는 보통 "2023-01-01T00:00:00.000+09:00" 형태이므로 substring(0,10)으로 잘라 사용
            String dateStr = (k.getDatetime() != null && k.getDatetime().length() >= 10) ? k.getDatetime().substring(0, 10) : "1900-01-01";
            // dto.setPublishDate 타입이 String이라면 바로 넣고, Date 객체라면 SimpleDateFormat으로 변환 후 set
            dto.setPublishDate(dateStr); 
            
            // 3. 기타 항목
            dto.setCategory("API");
            dto.setPrice(k.getPrice());
            dto.setDescription(k.getContents());
            dto.setBookCover(k.getThumbnail());
            
            // DB insert 호출
            cnt += bookDao.insert(dto);
        }
        return cnt;
    }
    

    // ---------------------------------------------------------
    // 📘 수정 (파일 업로드 포함)
    // ---------------------------------------------------------
    @Override
    public int edit(BookDto dto, MultipartFile file) {

        String fileName = dto.getBookCover();

        if(!file.isEmpty()) {
            try {fileName = upload.fileUpload(file)  ; } 
            catch (IOException e) { e.printStackTrace(); }
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


//    // ---------------------------------------------------------
//    // 🔍 카테고리별 조회
//    // ---------------------------------------------------------
//    @Override
//    public List<BookDto> findByCategory(String category) {
//        BookDto dto = new BookDto();
//        dto.setCategory(category);
//        return bookDao.findByCategory(dto);
//    }


//    // ---------------------------------------------------------
//    // 🔍 제목 검색 (AJAX)
//    // ---------------------------------------------------------
//    @Override
//    public List<BookDto> searchByTitle(String keyword) {
//        BookDto dto = new BookDto();
//        dto.setKeyword(keyword);
//        return bookDao.searchByTitle(dto);
//    }


    // ---------------------------------------------------------
    // 🔍 통합 검색 (제목/저자/카테고리) — AJAX
    // ---------------------------------------------------------
    @Override
    public List<BookDto> searchBooks(BookDto dto) {
        return bookDao.searchBooks(dto);
    }

    // 국립중앙도서관 API 결과 → BOOK 테이블 저장
    @Override
    public int insertFromNlApi(List<Map<String, Object>> apiResult) {

        int cnt = 0;

        for (Map<String, Object> b : apiResult) {

            BookDto dto = new BookDto();
            dto.setTitle((String) b.get("title_info"));
            dto.setAuthor((String) b.get("author_info"));
            dto.setPublisher((String) b.get("pub_info"));
            dto.setPublishDate((String) b.get("pub_year_info")); // YYYY
            dto.setCategory("국립중앙도서관");
            dto.setRanking(null);
            dto.setReviewCount(0);
            dto.setRating(0.0);
            dto.setDescription((String) b.get("subject_info"));
            dto.setPages(null);
            dto.setPrice(null);
            dto.setBookCover(null);

            cnt += bookDao.insert(dto);
        }

        return cnt;
    }
    
    public boolean isDuplicate(BookDto dto) {

        // 제목 또는 ISBN이 같은 책이 있으면 중복 처리
        List<BookDto> list = bookDao.searchBooks(dto);

        for (BookDto b : list) {
            if (b.getTitle().equals(dto.getTitle())) return true;
          //  if (dto.getIsbn() != null && dto.getIsbn().equals(b.getIsbn())) return true;
        }

        return false;
    }
    

//    // ---------------------------------------------------------
//    // ⚠️ 도서명 중복검사 (AJAX)
//    // ---------------------------------------------------------
//    @Override
//    public int checkTitleDuplicate(String title) {
//        BookDto dto = new BookDto();
//        dto.setTitle(title);
//        return bookDao.checkTitleDuplicate(dto);
//    }
}
