package com.the703.api;

import java.util.List;

import lombok.Data;

@Data
public class BookKakaoDto {
    private String title;         // 제목
    private List<String> authors; // 저자 리스트 (배열)
    private String contents;      // 소개글, 요약 
    private String url;           // 상세페이지 URL
    private String isbn;          // ISBN (10자리 또는 13자리(공백으로 구분)) → 표지 매칭 
    private String datetime;      // 출판일 (ISO8601 날짜)
    private String publisher;     // 출판사
//    private List<String> translators;  // 번역가 목록 (배열)    
    private int price;            // 정가
//    private int sale_price;	  // 판매가    
    private String thumbnail;     // 썸네일 이미지 URL (표지이미지(bookCover)) NL API와 조합할 때 가장 중요한 필드
//    private String status;      // 상태(정상판매, 품절 등)
    
    private String category;       // category
    private String ranking;        // ranking
    private Integer reviewCount;   // review_count
    private Double rating;         // rating
    private Integer pages;         // pages
    private String regDate;        // reg_date (String 처리)
        
}
