package com.the703.dao;

import java.util.HashMap;
import java.util.List;

//import org.apache.ibatis.annotations.Mapper;

import com.the703.dto.BookDto;

@Mapper
public interface BookMapper {

    // 전체 조회
	public List<BookDto> findAll(HashMap<String,Integer> map);
	public   int              findAllCnt(); 
    // 단건 조회
	public BookDto findById(BookDto dto);

    // 등록
	public int insert(BookDto book);

    // 수정
	public int update(BookDto book);

    // 삭제
	public int delete(Integer bookId);

    // 카테고리별 조회
	public List<BookDto> findByCategory(String category);

    // 제목 검색
	public List<BookDto> searchByTitle(String keyword);
}
