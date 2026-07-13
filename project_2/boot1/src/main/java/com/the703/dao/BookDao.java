package com.the703.dao;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.the703.dto.BookDto;

@Mapper
public interface BookDao {

    // ------------------------------------
    // 📘 기본 CRUD
    // ------------------------------------

    // 전체 조회 (페이징 포함)
    public List<BookDto> findAll(HashMap<String, Object> map);

    // 전체 개수
    public int findAllCnt();

    // 단건 조회
    public BookDto findById(BookDto dto);

    // 등록
    public int insert(BookDto book);

    // 수정
    public int update(BookDto book);

    // 삭제
    public int delete(BookDto dto);


    // ------------------------------------
    // 🔍 카테고리별 조회
    // ------------------------------------
//    public List<BookDto> findByCategory(BookDto dto);


    // ------------------------------------
    // 🔍 제목 검색 (AJAX)
    // ------------------------------------
//    public List<BookDto> searchByTitle(BookDto dto);


    // ------------------------------------
    // 🔍 통합 검색 (제목 + 저자 + 카테고리)
    //     → AJAX 검색창에서 사용
    // ------------------------------------
    public List<BookDto> searchBooks(BookDto dto);

    public List<BookDto> findByIsbn(String isbn);

    // ------------------------------------
    // ⚠️ 도서명 중복검사 (AJAX)
    // ------------------------------------
//    public int checkTitleDuplicate(BookDto dto);


    // ------------------------------------
    // 🔽 정렬 옵션 (평점, 리뷰수 등)
    // ------------------------------------
//    public List<BookDto> findAllOrder(BookDto dto);
}
