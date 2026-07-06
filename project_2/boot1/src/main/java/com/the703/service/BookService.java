package com.the703.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.the703.dto.BookDto;

public interface BookService {

    /**
     * Oracle 페이징 조회
     * pstartno = 페이지 시작 번호 (ROWNUM 시작값)
     * Oracle에서는 LIMIT 대신 ROWNUM BETWEEN 사용
     */
    public List<BookDto> findAll(int pstartno);

    /**
     * 전체 개수 조회 (COUNT(*))
     */
    public int findAllCnt();

    /**
     * Oracle INSERT
     * BOOK_SEQ.NEXTVAL 사용
     * 파일 업로드 포함
     */
    public int insert(BookDto dto, MultipartFile file);

    /**
     * 단건 상세 조회
     */
    public BookDto detail(BookDto dto);

    /**
     * 수정 화면용 조회
     */
    public BookDto editView(BookDto dto);

    /**
     * Oracle UPDATE
     * 파일 업로드 포함
     */
    public int edit(BookDto dto, MultipartFile file);

    /**
     * Oracle DELETE
     */
    public int delete(BookDto dto);

    /**
     * 카테고리별 조회
     */
    public List<BookDto> findByCategory(String category);

    /**
     * 제목 검색 (LIKE '%' || keyword || '%')
     */
    public List<BookDto> searchByTitle(String keyword);
}
