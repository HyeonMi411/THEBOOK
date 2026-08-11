package com.thejoa703.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.thejoa703.entity.Book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class BookDto {

    // 등록/수정 요청 Dto
    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class BookRequestDto {
        @NotBlank
        private String title;
        @NotBlank
        private String author;
        @NotBlank
        private String publisher;
        @NotNull
        private LocalDate publishDate;
        @NotBlank
        private String category;
        private String ranking;
        private Integer reviewCount;
        private Double  rating;
        private String  description;
        private Integer pages;
        private Integer price;
        private String  bookCover;
    }

    // 응답 Dto
    @Getter @Setter @NoArgsConstructor
    public static class BookResponseDto {
        private Long   bookId;
        private String title;
        private String author;
        private String publisher;
        private LocalDate publishDate;
        private String category;
        private String ranking;
        private Integer reviewCount;
        private Double  rating;
        private String  description;
        private Integer pages;
        private Integer price;
        private LocalDateTime regDate;
        private String  bookCover;
        private Long    likedCount;   // 찜(좋아요) 수 - AppUser ManyToMany 집계

        public static BookResponseDto from(Book book) {
            BookResponseDto dto = new BookResponseDto();
            dto.setBookId(book.getBookId());
            dto.setTitle(book.getTitle());
            dto.setAuthor(book.getAuthor());
            dto.setPublisher(book.getPublisher());
            dto.setPublishDate(book.getPublishDate());
            dto.setCategory(book.getCategory());
            dto.setRanking(book.getRanking());
            dto.setReviewCount(book.getReviewCount());
            dto.setRating(book.getRating());
            dto.setDescription(book.getDescription());
            dto.setPages(book.getPages());
            dto.setPrice(book.getPrice());
            dto.setRegDate(book.getRegDate());
            dto.setBookCover(book.getBookCover());
            dto.setLikedCount( book.getLikedByUsers() != null ? (long) book.getLikedByUsers().size() : 0L );
            return dto;
        }

        public BookResponseDto(Book book) {
            this.bookId = book.getBookId();
            this.title = book.getTitle();
            this.author = book.getAuthor();
            this.publisher = book.getPublisher();
            this.publishDate = book.getPublishDate();
            this.category = book.getCategory();
            this.ranking = book.getRanking();
            this.reviewCount = book.getReviewCount();
            this.rating = book.getRating();
            this.description = book.getDescription();
            this.pages = book.getPages();
            this.price = book.getPrice();
            this.regDate = book.getRegDate();
            this.bookCover = book.getBookCover();
        }
    }
}
