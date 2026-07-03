package com.the703.dao;

import com.the703.dto.BookDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookDao {

    public List<BookDto> findAll();

    public BookDto findById(@Param("bookId") Long bookId);

    public int insertBook(BookDto book);

    public int updateBook(BookDto book);

    public int deleteBook(@Param("bookId") Long bookId);
}


