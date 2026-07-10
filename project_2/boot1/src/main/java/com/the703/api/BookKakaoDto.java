package com.the703.api;

import java.util.List;

import lombok.Data;

@Data
public class BookKakaoDto {
    private String title;        // 제목
    private String contents;     // 소개글
    private String url;          // 상세페이지 URL
    private String isbn;         // ISBN
    private String datetime;     // 출판일
    private List<String> authors; // 저자 리스트
    private List<String> translators; // 번역가 리스트
    private String publisher;    // 출판사
    private int price;           // 정가
    private int salePrice;       // 판매가
    private String thumbnail;    // 썸네일 이미지 URL
    private String status;       // 상태(정상판매, 품절 등)
    //######
}



//1) kakaoDto