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

	// 도서 등록/수정 요청 Dto
	@Getter @Setter @AllArgsConstructor @NoArgsConstructor
	public static class BookRequestDto {
		@NotBlank(message = "도서명은 필수입니다.")
		private String title;

		@NotBlank(message = "저자는 필수입니다.")
		private String author;

		@NotBlank(message = "출판사는 필수입니다.")
		private String publisher;

		@NotNull(message = "출간일은 필수입니다.")
		private LocalDate publishDate;

		@NotBlank(message = "카테고리는 필수입니다.")
		private String category;

		private String ranking;
		private Integer reviewCount;
		private Double rating;
		private String description; // CLOB
		private Integer pages;
		private Integer price;
	}

	// 도서 응답 Dto
	@Getter @Setter @NoArgsConstructor
	public static class BookResponseDto {
		private Long id;
		private String title;
		private String author;
		private String publisher;
		private LocalDate publishDate;
		private String category;
		private String ranking;
		private Integer reviewCount;
		private Double rating;
		private String description;
		private Integer pages;
		private Integer price;
		private String bookCover;
		private LocalDateTime regDate;
		private String userNickname; // 등록한 관리자 닉네임

		public static BookResponseDto from(Book book) {
			BookResponseDto dto = new BookResponseDto();
			dto.setId(book.getId());
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
			dto.setBookCover(book.getBookCover());
			dto.setRegDate(book.getRegDate());
			if (book.getUser() != null) { dto.setUserNickname(book.getUser().getNickname()); }
			return dto;
		}
	}
}
