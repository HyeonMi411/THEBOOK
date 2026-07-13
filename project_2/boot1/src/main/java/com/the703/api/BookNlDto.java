package com.the703.api;

import java.util.List;

import lombok.Data;

@Data
public class BookNlDto {
    private String title_info;        	// 제목
    private String author_info; 		// 저자 리스트 
    private String pub_info;    		// 출판사
    private String pub_year_info;     	// 출판일 (YYYY)     
    private String isbn;         		// ISBN (10자리 또는 13자리(공백으로 구분)) → 카카오/네이버 API 표지 매칭 
    private String subject_info;     	// 소개글, 요약
    private String page_info;			// 페이지
    private String price_info;          // 정가
    private String class_nm;			// 분류명
    private String controlno;			// 국립중앙도서관 고유번호
    private String reg_date;			// 등록일 
    
//    private String url;          		// 상세페이지 URL
//    private String status;       		// 상태(정상판매, 품절 등)
        
    private String ranking;        // ranking
    private Integer reviewCount;   // review_count
    private Double rating;         // rating    
    private String bookCover;      // book_cover
    
}
