package com.the703.service;

import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.the703.dao.BookMapper;
import com.the703.dto.BookDto;

//import java.net.InetAddress;
//import java.net.UnknownHostException;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class BookServiceImpl  implements BookService{
 
   @Autowired BookMapper bookMapper;

    @Override
    public List<BookDto> findAll(int  pstartno) {
		HashMap<String,Integer> map = new HashMap<>();
		map.put("start", (pstartno-1)*10);  
		map.put("end"  ,  10);    	
        return bookMapper.findAll(map);
    }

    @Override
    public BookDto findById(Integer bookId) {
        return bookMapper.findById(bookId);
    }

    @Override
    public int insert(BookDto book) {
        return bookMapper.insert(book);
    }

    @Override
    public int update(BookDto book) {
        return bookMapper.update(book);
    }

    @Override
    public int delete(Integer bookId) {
        return bookMapper.delete(bookId);
    }

    @Override
    public List<BookDto> findByCategory(String category) {
        return bookMapper.findByCategory(category);
    }

    @Override
    public List<BookDto> searchByTitle(String keyword) {
        return bookMapper.searchByTitle(keyword);
    }
    	
}
