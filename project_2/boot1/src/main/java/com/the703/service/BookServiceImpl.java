package com.the703.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

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

        if(!file.isEmpty()) {
            try { fileName = upload.fileUpload(file)  ; } 
            catch (IOException e) { e.printStackTrace(); }
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

	@Override
	public int apikakaoinsert(List<BookKakaoDto> api) {
		// TODO Auto-generated method stub
		return 0;
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
