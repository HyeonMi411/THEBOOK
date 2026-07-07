package com.the703.dao;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import com.the703.dto.BookDto;

@Mapper
public interface BookDao {

    // ■ Oracle 페이징 조회 (OFFSET / FETCH)
    public List<BookDto> findAll(HashMap<String, Integer> map);

    // ■ 전체 개수 조회
    public int findAllCnt();

    // ■ 단건 조회
    public BookDto findById(Long bookId);

    // ■ 등록
    public int insert(BookDto book);

    // ■ 수정
    public int update(BookDto book);

    // ■ 삭제
    public int delete(Integer bookId);

    // ■ 카테고리별 조회
    public List<BookDto> findByCategory(String category);

    // ■ AJAX 도서명 검색 (LIKE 검색)
    public List<BookDto> searchByTitle(String keyword);

    // ■ 도서명 중복 검사 (정확히 일치)
    public int countByTitle(String title);
}




    //public List<BookDto> 	findAll();  
    
    // 페이징
    
    // 도서검색 (카테고리, 제목)

/*
> CRUD3  도서관리
                    사용자                                    관리자
    CREATE          X                                        도서올리기 
    READ           도서전체확인(페이징), 상세도서페이지, 도서검색(도서명중복검사) / 공통
    UPDATE         X                                         도서수정
    DELETE         X                                         도서삭제
 */ 
