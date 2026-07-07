package com.the703.dto;

import lombok.Data;

@Data
public class BookDto {

    private Long bookId;            // NUMBER(10)
    private String title;           // VARCHAR2(255)
    private String author;          // VARCHAR2(100)
    private String publisher;       // VARCHAR2(100)
    private String publishDate;  // DATE
    private String category;        // VARCHAR2(50)
    private String ranking;         // VARCHAR2(100)
    private Integer reviewCount;    // NUMBER(10)
    private Double rating;          // NUMBER(10,2)
    private String description;     // CLOB
    private Integer pages;          // NUMBER(10)
    private Integer price;          // NUMBER(10)
    private String regDate;  // TIMESTAMP
    private String bookCover;       // VARCHAR2(300)
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
 RATING                                             NUMBER(10,2)
 DESCRIPTION                                        CLOB
 PAGES                                              NUMBER(10)
 PRICE                                              NUMBER(10)
 REG_DATE                                           TIMESTAMP(6)
 BOOK_COVER                                         VARCHAR2(300)
*/
