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


    // ---------------------------------------------------------
    // ⚠️ 도서명 중복검사 (AJAX)
    // ---------------------------------------------------------
//    public int checkTitleDuplicate(String title);
}
