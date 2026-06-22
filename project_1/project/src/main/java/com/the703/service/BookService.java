package com.the703.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.the703.dto.BoardDto;
import com.the703.dto.BookDto;

public interface BookService {

    public List<BookDto> findAll(int  pstartno);
	public int             findAllCnt();    

    public int insert(BookDto dto , MultipartFile file);
    
    public BookDto        detail(BookDto dto);
    
	public BookDto        editView(BookDto dto); 
	public int             edit(BookDto  dto , MultipartFile file);     

	public int             delete(BookDto  dto);

    public List<BookDto> findByCategory(String category);

    public List<BookDto> searchByTitle(String keyword);
}

