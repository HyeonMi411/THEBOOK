package com.thejoa703.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
@AllArgsConstructor @Builder
public class Book {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_seq")
	@SequenceGenerator(name = "book_seq", sequenceName = "BOOK_SEQ", allocationSize = 1)
	@Column(name = "BOOK_ID")
	private Long id;

	@Column(length = 255, nullable = false)
	private String title;

	@Column(length = 100, nullable = false)
	private String author;

	@Column(length = 100, nullable = false)
	private String publisher;

	@Column(name = "PUBLISH_DATE", nullable = false)
	private java.time.LocalDate publishDate;

	@Column(length = 50, nullable = false)
	private String category;

	@Column(length = 100)
	private String ranking;

	@Column(name = "REVIEW_COUNT")
	private Integer reviewCount;

	@Column(precision = 5, scale = 2)
	private Double rating;

	@Lob
	@Column
	private String description;   // CLOB

	@Column
	private Integer pages;

	@Column
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
}
