package com.the703.api;

import java.util.List;

import lombok.Data;

@Data
public class BookNaverDto {
    private String title;       // 제목 (HTML 태그 포함 가능)
    private String author; 		// 저자 (여러 명일 경우 쉼표로 구분된 문자열)
    private String link;        // 상세페이지 URL
    private String image;    	// 썸네일 이미지 URL (표지이미지(bookCover)) NL API와 조합할 때 가장 중요한 필드
    private int price;          // 정가
//    private int discount;		// 판매가    
    private String publisher;   // 출판사
    private String isbn;        // ISBN (10자리 또는 13자리) → 표지 매칭 
    private String description; // 소개글, 요약 
    private String pubdate;     // 출판일 (YYYYMMDD)
    
    private String category;       // category
    private String ranking;        // ranking
    private Integer reviewCount;   // review_count
    private Double rating;         // rating
    private Integer pages;         // pages
    private String regDate;        // reg_date (String 처리)
        
}
