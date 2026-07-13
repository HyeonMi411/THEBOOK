package com.the703.service;

//import java.util.HashMap;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.the703.api.BookKakaoDto;
import com.the703.dto.BookDto;

public interface BookService {

    // ---------------------------------------------------------
    // 📘 전체 조회 (페이징)
    // ---------------------------------------------------------
    public List<BookDto> findAll(int pstartno);
    public int findAllCnt();


    // ---------------------------------------------------------
    // 📘 등록 (파일 포함)
    // ---------------------------------------------------------
    public int insert(BookDto dto, MultipartFile file);


    // ---------------------------------------------------------
    // 📘 상세 조회
    // ---------------------------------------------------------
    public BookDto detail(BookDto dto);


    // ---------------------------------------------------------
    // 📘 수정 화면 조회
    // ---------------------------------------------------------
    public BookDto editView(BookDto dto);


    // ---------------------------------------------------------
    // 📘 수정 (파일 포함)
    // ---------------------------------------------------------
    public int edit(BookDto dto, MultipartFile file);


    // ---------------------------------------------------------
    // 📘 삭제
    // ---------------------------------------------------------
    public int delete(BookDto dto);


    // ---------------------------------------------------------
    // 🔍 카테고리별 조회
    // ---------------------------------------------------------
//    public List<BookDto> findByCategory(String category);


    // ---------------------------------------------------------
    // 🔍 제목 검색 (AJAX)
    // ---------------------------------------------------------
//    public List<BookDto> searchByTitle(String keyword);


    // ---------------------------------------------------------
    // 🔍 통합 검색 (제목/저자/카테고리) — AJAX
    // ---------------------------------------------------------
    public List<BookDto> searchBooks(BookDto dto);
	public int apikakaoinsert(List<BookKakaoDto> api);
    // 국립중앙도서관 API → DB 저장
    int insertFromNlApi(List<java.util.Map<String, Object>> apiResult);

    public boolean isDuplicate(BookDto dto);
//    {
//
//        // 제목 또는 ISBN이 같은 책이 있으면 중복 처리
//        List<BookDto> list = bookDao.searchBooks(dto);
//
//        for (BookDto b : list) {
//            if (b.getTitle().equals(dto.getTitle())) return true;
//            if (dto.getIsbn() != null && dto.getIsbn().equals(b.getIsbn())) return true;
//        }
//
//        return false;
//    }
    
    // ---------------------------------------------------------
    // ⚠️ 도서명 중복검사 (AJAX)
    // ---------------------------------------------------------
//    public int checkTitleDuplicate(String title);
}
