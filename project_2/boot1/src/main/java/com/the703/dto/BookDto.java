package com.the703.dto;

import lombok.Data;

@Data
public class BookDto {

    // ------------------------------------
    // 📘 BOOK 테이블 매핑 필드 (Oracle)
    // ------------------------------------
    private Integer bookId;        // book_id (PK)
    private String title;          // title
    private String author;         // author
    private String publisher;      // publisher
    private String publishDate;    // publish_date (String 처리)
    private String category;       // category
    private String ranking;        // ranking
    private Integer reviewCount;   // review_count
    private Double rating;         // rating
    private String description;    // description
    private Integer pages;         // pages
    private Integer price;         // price
    private String regDate;        // reg_date (String 처리)
    private String bookCover;      // book_cover

    // ------------------------------------
    // 🔍 검색(AJAX) 기능용 필드
    // ------------------------------------
    private String keyword;        // 검색 키워드
    private String searchType;     // 검색 기준(title, author, category 등)
    private String orderBy;        // 정렬 기준(rating, reviewCount 등)

    // ------------------------------------
    // ⚠️ 도서명 중복검사(AJAX) 필드
    // ------------------------------------
    private boolean duplicate;     // true = 중복, false = 사용 가능

    // ------------------------------------
    // 📡 AJAX 응답 상태 필드
    // ------------------------------------
    private boolean success;       // 요청 성공 여부
    private String message;        // 응답 메시지
}



/*
SQL> desc book;
 이름                                      널?      유형
 ----------------------------------------- -------- ----------------------------
 BOOK_ID                                   NOT NULL NUMBER(10)
 TITLE                                     NOT NULL VARCHAR2(255)
 AUTHOR                                    NOT NULL VARCHAR2(100)
 PUBLISHER                                 NOT NULL VARCHAR2(100)
 PUBLISH_DATE                              NOT NULL DATE
 CATEGORY                                  NOT NULL VARCHAR2(50)
 RANKING                                            VARCHAR2(100)
 REVIEW_COUNT                                       NUMBER(10)
 RATING                                             NUMBER(5,2)
 DESCRIPTION                                        CLOB
 PAGES                                              NUMBER(10)
 PRICE                                              NUMBER(10)
 REG_DATE                                           TIMESTAMP(6)
 BOOK_COVER                                         VARCHAR2(300)
*/