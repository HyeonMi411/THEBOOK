package com.the703.dto;

import lombok.Data;
//import java.time.LocalDate;
//import java.time.LocalDateTime;

@Data
public class BookDto {

    private Integer bookId;        // book_id (PK)
    private String title;          // title
    private String author;         // author
    private String publisher;      // publisher
    private String publishDate; // publish_date
    private String category;       // category
    private String ranking;        // ranking
    private Integer reviewCount;   // review_count
    private Double rating;         // rating
    private String description;    // description
    private Integer pages;         // pages
    private Integer price;         // price
    private String regDate; // reg_date
    private String bookCover;      // book_cover
}



/*
mysql> desc book;
+--------------+--------------+------+-----+-------------------+-------------------+
| Field        | Type         | Null | Key | Default           | Extra             |
+--------------+--------------+------+-----+-------------------+-------------------+
| book_id      | int          | NO   | PRI | NULL              | auto_increment    |
| title        | varchar(255) | NO   |     | NULL              |                   |
| author       | varchar(100) | NO   |     | NULL              |                   |
| publisher    | varchar(100) | NO   |     | NULL              |                   |
| publish_date | date         | NO   |     | NULL              |                   |
| category     | varchar(50)  | NO   |     | NULL              |                   |
| ranking      | varchar(100) | YES  |     | NULL              |                   |
| review_count | int          | YES  |     | 0                 |                   |
| rating       | double       | YES  |     | NULL              |                   |
| description  | text         | YES  |     | NULL              |                   |
| pages        | int          | YES  |     | NULL              |                   |
| price        | int          | YES  |     | NULL              |                   |
| reg_date     | datetime     | YES  |     | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| book_cover   | varchar(300) | YES  |     | NULL              |                   |
+--------------+--------------+------+-----+-------------------+-------------------+
14 rows in set (0.00 sec)

*/