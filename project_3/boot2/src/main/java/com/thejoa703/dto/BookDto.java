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

	// 도서 등록/수정 요청 Dto (도서등록/수정은 관리자만 가능)
	@Getter @Setter @NoArgsConstructor @AllArgsConstructor
	public static class BookRequestDto {
		@NotBlank
		private String title;          // 제목

		@NotBlank
		private String author;         // 저자

		@NotBlank
		private String publisher;      // 출판사

		@NotNull
		private LocalDate publishDate; // 출간일 (yyyy-MM-dd)

		@NotBlank
		private String category;       // 카테고리

		private String ranking;        // 순위(선택)
		private Integer reviewCount;   // 리뷰수
		private Double rating;         // 평점
		private String description;    // 소개(긴 텍스트)
		private Integer pages;         // 페이지수
		private Integer price;         // 가격
		// 표지이미지(bookCover)는 MultipartFile 로 별도 전송
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
		private LocalDateTime regDate;
		private String bookCover;

		// ★도서를 등록한 관리자 정보
		private Long adminId;
		private String adminNickname;

		public static BookResponseDto fromEntity(Book book) {
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
			dto.setRegDate(book.getRegDate());
			dto.setBookCover(book.getBookCover());
			if (book.getUser() != null) {
				dto.setAdminId(book.getUser().getId());
				dto.setAdminNickname(book.getUser().getNickname());
			}
			return dto;
		}
	}
}
