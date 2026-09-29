package com.thejoa703.api;

import java.util.List;

import lombok.Data;

/**
 * 카카오 도서검색 API(https://dapi.kakao.com/v3/search/book) 응답을 담는 Dto
 * boot1(the703) 의 api/BookKakaoDto.java 를 그대로 재현했음.
 */
@Data
public class BookKakaoDto {
    private String title;         // 제목
    private List<String> authors; // 저자 리스트
    private String contents;      // 소개글, 요약
    private String url;           // 상세페이지 URL
    private String isbn;          // ISBN
    private String datetime;      // 출판일 (ISO8601)
    private String publisher;     // 출판사
    private int price;            // 정가
    private String thumbnail;     // 썸네일(표지) 이미지 URL
}
