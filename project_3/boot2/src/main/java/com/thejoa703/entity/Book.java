package com.thejoa703.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

@Entity
@Table(name = "BOOK")
@Getter @Setter @NoArgsConstructor
@AllArgsConstructor @Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_seq")
    @SequenceGenerator(name = "book_seq", sequenceName = "BOOK_SEQ", allocationSize = 1)
    @Column(name = "BOOK_ID")
    private Long bookId;

    @Column(name = "TITLE", length = 255, nullable = false)
    private String title;

    @Column(name = "AUTHOR", length = 100, nullable = false)
    private String author;

    @Column(name = "PUBLISHER", length = 100, nullable = false)
    private String publisher;

    @Column(name = "PUBLISH_DATE", nullable = false)
    private LocalDate publishDate;

    @Column(name = "CATEGORY", length = 50, nullable = false)
    private String category;

    @Column(name = "RANKING", length = 100)
    private String ranking;

    @Column(name = "REVIEW_COUNT")
    private Integer reviewCount;

    @Column(name = "RATING", precision = 5) //, scale = 2
    private Double rating;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "PAGES")
    private Integer pages;

    @Column(name = "PRICE")
    private Integer price;

    @Column(name = "REG_DATE")
    private LocalDateTime regDate;

    @Column(name = "BOOK_COVER", length = 300)
    private String bookCover;

    @PrePersist
    void onCreate() {
        if (this.regDate == null) {
            this.regDate = LocalDateTime.now();
        }
    }

    // ★ 여러 유저가 여러 책을 "찜(좋아요)" 할 수 있다  (다대다)
    // - 주인(owning) 쪽은 AppUser.likedBooks 이며, 여기는 mappedBy로 읽기전용 반대편(inverse)
    // - 조인테이블: BOOK_LIKE(APP_USER_ID, BOOK_ID)
    @Builder.Default
    @ManyToMany(mappedBy = "likedBooks")
    private List<AppUser> likedByUsers = new ArrayList<>();
}
