package com.the703.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.the703.dao.BookDao;
import com.the703.dto.BookDto;

@Service
public class BookServiceImpl implements BookService {

    @Autowired 
    BookDao bookDao;

    /**
     * Oracle 페이징 방식
     * MySQL: start = (page-1)*10, end = 10
     * Oracle: start = (page-1)*10 + 1, end = page*10
     */
    @Override
    public List<BookDto> findAll(int pstartno) {

        int start = (pstartno - 1) * 10 + 1;   // ROWNUM 시작
        int end   = pstartno * 10;            // ROWNUM 끝

        HashMap<String, Integer> map = new HashMap<>();
        map.put("start", start);
        map.put("end", end);

        return bookDao.findAll(map);
    }

    /**
     * Oracle INSERT
     * BOOK_SEQ.NEXTVAL 사용
     */
    @Override
    public int insert(BookDto dto, MultipartFile file) {

        String fileName = "the703.png";

        if (!file.isEmpty()) {
            fileName = file.getOriginalFilename();
            String uploadPath = "C:/file/";
            File dest = new File(uploadPath + fileName);

            try {
                file.transferTo(dest);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        dto.setBookCover(fileName);

        // Oracle에서는 Mapper.xml에서 BOOK_SEQ.NEXTVAL 사용
        return bookDao.insert(dto);
    }

    /**
     * Oracle UPDATE
     */
    @Override
    public int edit(BookDto dto, MultipartFile file) {

        String fileName = dto.getBookCover(); // 기존 이미지 유지

        if (!file.isEmpty()) {
            fileName = file.getOriginalFilename();
            String uploadPath = "C:/file/";
            File dest = new File(uploadPath + fileName);

            try {
                file.transferTo(dest);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        dto.setBookCover(fileName);

        return bookDao.update(dto);
    }

    /**
     * Oracle DELETE
     */
    @Override
    public int delete(BookDto dto) {
        return bookDao.delete(dto.getBookId());
    }

    @Override
    public List<BookDto> findByCategory(String category) {
        return bookDao.findByCategory(category);
    }

    @Override
    public List<BookDto> searchByTitle(String keyword) {
        return bookDao.searchByTitle(keyword);
    }

    @Override
    public int findAllCnt() {
        return bookDao.findAllCnt();
    }

    @Override
    public BookDto detail(BookDto dto) {
        return bookDao.findById(dto);
    }

    @Override
    public BookDto editView(BookDto dto) {
        return bookDao.findById(dto);
    }
}
