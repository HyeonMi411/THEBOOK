package com.the703.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.the703.api.BookKakaoDto;
import com.the703.dto.BookDto;

public interface BookService {

    // 전체 조회
    public List<BookDto> findAll(int pstartno);
    public int findAllCnt();

    // 등록  ( 수동으로 crud)
    public int insert(BookDto dto, MultipartFile file);

    // 상세 조회
    public BookDto detail(BookDto dto);

    // 수정 화면
    public BookDto editView(BookDto dto);

    // 수정
    public int edit(BookDto dto, MultipartFile file);

    // 삭제
    public int delete(BookDto dto);

    // 카테고리별 조회
    public List<BookDto> findByCategory(String category, int pstartno);
    public int findCategoryCnt(String category);

    // 통합 검색
    public List<BookDto> searchBooks(BookDto dto);

    // ⭐ 국립중앙도서관 API → BOOK 테이블 저장
    public int insertFromNlApi(List<java.util.Map<String, Object>> apiResult);

    // 중복 검사
    public boolean isDuplicate(BookDto dto);
     
    // kakao api  → book 테이블저장
	public int insert(List<BookKakaoDto> result);
}
