package com.the703.dao;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.the703.api.BookKakaoDto;
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
    public int insert(BookDto dto);
    public int insert_kakao(BookDto dto);

    // 수정
    public int update(BookDto book);

    // 삭제
    public int delete(BookDto dto);


    // ------------------------------------
    // ⭐ 카테고리별 조회 (Oracle 페이징)
    // ------------------------------------
    public List<BookDto> findByCategory(HashMap<String, Object> map);

    public int findCategoryCnt(String category);


    // ------------------------------------
    // 🔍 통합 검색 (제목 + 저자 + 카테고리)
    // ------------------------------------
    public List<BookDto> searchBooks(BookDto dto);

    // ISBN 조회
    public List<BookDto> findByIsbn(String isbn);
}
