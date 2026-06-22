package com.the703.service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.the703.dao.BookMapper;
import com.the703.dto.BoardDto;
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
		map.put("end"  , 10);    	
        return bookMapper.findAll(map);
    }


    @Override
    public int insert(BookDto dto , MultipartFile file) {
		String fileName   = "the703.png";
		
		if( !file.isEmpty() ) {
			fileName   = file.getOriginalFilename();
			String uploadPath = "C:/file/";
			File       demp   = new File(uploadPath + fileName);  
			
			try { file.transferTo(demp); }   
			catch (IOException e) { e.printStackTrace(); } 
		}
		
		dto.setBookCover(fileName);

        return bookMapper.insert(dto);
    }

    @Override
    public int edit(BookDto dto , MultipartFile file) {
		int result = -1;   
		
		String fileName = "no image";  
			
			if(  !file.isEmpty()) {
				fileName = file.getOriginalFilename();
				String uploadPath = "C:/file/";
				File demp = new File(  uploadPath + fileName );
				
				try { file.transferTo(demp); }   
				catch (IOException e) { e.printStackTrace(); }
				
			}
			dto.setBookCover(fileName);
        return bookMapper.update(dto);
    }

    @Override
    public int delete(BookDto dto) {
        return bookMapper.delete( dto.getBookId()  );
    }

    @Override
    public List<BookDto> findByCategory(String category) {
        return bookMapper.findByCategory(category);
    }

    @Override
    public List<BookDto> searchByTitle(String keyword) {
        return bookMapper.searchByTitle(keyword);
    }

	@Override
	public int findAllCnt() {
		// TODO Auto-generated method stub
		return bookMapper.findAllCnt();
	}

	@Override
	public BookDto detail(BookDto dto) {	
		// TODO Auto-generated method stub
		return bookMapper.findById(dto);
	}

	@Override
	public BookDto editView(BookDto dto) {
		// TODO Auto-generated method stub
		return bookMapper.findById(dto);
	}


    	
}
