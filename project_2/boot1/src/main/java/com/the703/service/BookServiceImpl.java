//package com.the703.service;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.List;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import com.the703.dao.BookDao;
//import com.the703.dto.BookDto;
//
//@Service
//public class BookServiceImpl implements BookService {
//
//    @Autowired
//    BookDao bookDao;
//
//    /**
//     * ■ Oracle 페이징 방식
//     * pstartno = 페이지 번호
//     * Oracle: OFFSET / FETCH 사용
//     */
//    @Override
//    public List<BookDto> findAll(int pstartno) {
//
//        int start = (pstartno - 1) * 10;   // OFFSET
//        int end   = 10;                   // FETCH NEXT
//
//        HashMap<String, Integer> map = new HashMap<>();
//        map.put("start", start);
//        map.put("end", end);
//
//        return bookDao.findAll(map);
//    }
//
//    /**
//     * ■ 전체 개수
//     */
//    @Override
//    public int findAllCnt() {
//        return bookDao.findAllCnt();
//    }
//
//    /**
//     * ■ Oracle INSERT (파일 업로드 포함)
//     * BOOK_SEQ.NEXTVAL은 Mapper.xml에서 처리
//     */
//    @Override
//    public int insert(BookDto dto, MultipartFile file) {
//
//        String fileName = "default.png";
//
//        if (file != null && !file.isEmpty()) {
//            fileName = file.getOriginalFilename();
//            String uploadPath = "C:/file/";
//            File dest = new File(uploadPath + fileName);
//
//            try {
//                file.transferTo(dest);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        dto.setBookCover(fileName);
//
//        return bookDao.insert(dto);
//    }
//
//    /**
//     * ■ Oracle UPDATE (파일 업로드 포함)
//     */
//    @Override
//    public int edit(BookDto dto, MultipartFile file) {
//
//        String fileName = dto.getBookCover(); // 기존 파일 유지
//
//        if (file != null && !file.isEmpty()) {
//            fileName = file.getOriginalFilename();
//            String uploadPath = "C:/file/";
//            File dest = new File(uploadPath + fileName);
//
//            try {
//                file.transferTo(dest);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        dto.setBookCover(fileName);
//
//        return bookDao.update(dto);
//    }
//
//    /**
//     * ■ Oracle DELETE
//     */
//    @Override
//    public int delete(BookDto dto) {
//        return bookDao.delete(dto.getBookId());
//    }
//
//    /**
//     * ■ 상세 조회
//     */
//    @Override
//    public BookDto detail(BookDto dto) {
//        return bookDao.findById(dto.getBookId());
//    }
//
//    /**
//     * ■ 수정 화면용 조회
//     */
//    @Override
//    public BookDto editView(BookDto dto) {
//        return bookDao.findById(dto.getBookId());
//    }
//
//    /**
//     * ■ 카테고리별 조회
//     */
//    @Override
//    public List<BookDto> findByCategory(String category) {
//        return bookDao.findByCategory(category);
//    }
//
//    /**
//     * ■ AJAX 제목 검색
//     */
//    @Override
//    public List<BookDto> searchByTitle(String keyword) {
//        return bookDao.searchByTitle(keyword);
//    }
//
//    /**
//     * ■ 도서명 중복 검사 (AJAX)
//     */
//    @Override
//    public boolean isTitleDuplicate(String title) {
//        return bookDao.countByTitle(title) > 0;
//    }
//}
