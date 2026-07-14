package com.the703.api;

import lombok.Data;

@Data
public class BookNlDto {

    // ------------------------------------
    // 📚 국립중앙도서관 API 기본 응답 필드
    // ------------------------------------
    private String title_info;        // 제목
    private String author_info;       // 저자
    private String pub_info;          // 출판사
    private String pub_year_info;     // 발행년도
    private String isbn;              // ISBN
    private String id;                // 국립중앙도서관 고유번호
    private String image_url;         // 표지 이미지 URL
    private String reg_date;          // 등록일
    private String kdc_name_1s;       // KDC 분류명

    // ------------------------------------
    // 📌 API 기본 응답에는 없지만 확장 가능 필드
    // ------------------------------------
    private String subject_info;      // 소개글 (상세조회 API 필요)
    private String page_info;         // 페이지 수
    private String price_info;        // 가격

    // ------------------------------------
    // 📘 최종 표지 이미지 처리용 필드
    // ------------------------------------
    private String bookCover;

    public String getBookCover() {
        if (this.image_url == null || this.image_url.equals("http://cover.nl.go.kr/")) {
            return "/images/the703.png"; // 기본 이미지
        }
        return this.image_url;
    }
}
