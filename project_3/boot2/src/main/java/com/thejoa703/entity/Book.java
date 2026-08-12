package com.thejoa703.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "BOOK")
@Getter @Setter @NoArgsConstructor
public class Book {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_seq")
	@SequenceGenerator(name = "book_seq", sequenceName = "BOOK_SEQ", allocationSize = 1)
	@Column(name = "BOOK_ID")
	private Long id;

	@Column(length = 255, nullable = false)
	private String title;          // 도서 제목

	@Column(length = 100, nullable = false)
	private String author;         // 도서 저자명 (글쓴이 = 작가, AppUser 아님)

	@Column(length = 100, nullable = false)
	private String publisher;      // 출판사

	@Column(name = "PUBLISH_DATE", nullable = false)
	private LocalDate publishDate; // 출간일

	@Column(length = 50, nullable = false)
	private String category;       // 카테고리

	@Column(length = 100)
	private String ranking;        // 순위(선택)

	@Column(name = "REVIEW_COUNT")
	private Integer reviewCount;   // 리뷰수

	@Column
	private Double rating;         // 평점

	@Lob // 대용량데이터처리 - CLOB(문자열)
	@Column
	private String description;    // 도서 소개(긴 텍스트)

	@Column
	private Integer pages;         // 페이지수

	@Column
	private Integer price;         // 가격

	@Column(name = "REG_DATE")
	private LocalDateTime regDate; // 등록일시

	@Column(name = "BOOK_COVER", length = 300)
	private String bookCover;      // 표지 이미지 경로(파일)

	// ★도서 등록은 관리자(ROLE_ADMIN)만 가능 -> 등록한 관리자와 연결
	@ManyToOne
	@JoinColumn(name = "APP_USER_ID", nullable = false)
	private AppUser user;          // 이 책을 등록한 관리자(AppUser)

	@PrePersist
	void onCreate() {
		this.regDate = LocalDateTime.now();
	}
}
