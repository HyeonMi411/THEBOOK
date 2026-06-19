package com.the703.service;

import java.util.List;

import com.the703.dto.BookDto;

public interface BookService {

    public List<BookDto> findAll(int  pstartno);

    public BookDto findById(Integer bookId);

    public int insert(BookDto book);

    public int update(BookDto book);

    public int delete(Integer bookId);

    public List<BookDto> findByCategory(String category);

    public List<BookDto> searchByTitle(String keyword);
}

