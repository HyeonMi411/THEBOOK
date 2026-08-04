package com.thejoa703.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "BOOK")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Book {

	@Id		// jakarta.persistence.Id;
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_seq")
	@SequenceGenerator(name = "book_seq", sequenceName = "BOOK_SEQ", allocationSize = 1)
	@Column(name = "BOOK_ID")
	Long bookId;

	// ★한 회원(관리자)이 여러권의 책을 등록할 수 있다.  (Book : AppUser = N : 1)
	@ManyToOne
	@JoinColumn(name = "APP_USER_ID", nullable = false)
	AppUser user;			// 등록자(관리자)		@ManyToOne		AppUser

	@Column(name = "TITLE", nullable = false, length = 255)
	String title;			// 도서명

	@Column(name = "AUTHOR", nullable = false, length = 100)
	String author;			// 저자

	@Column(name = "PUBLISHER", nullable = false, length = 100)
	String publisher;		// 출판사

	@Column(name = "PUBLISH_DATE", nullable = false)
	LocalDate publishDate;	// 출간일

	@Column(name = "CATEGORY", nullable = false, length = 50)
	String category;		// 분류

	@Column(name = "RANKING", length = 100)
	String ranking;			// 순위(카테고리/베스트셀러 등)

	@Column(name = "REVIEW_COUNT")
	Integer reviewCount;	// 리뷰 수

	@Column(name = "RATING", precision = 5, scale = 2)
	BigDecimal rating;		// 평점

	@Lob
	@Column(name = "DESCRIPTION")
	String description;	// 책 소개 ( 긴텍스트 )

	@Column(name = "PAGES")
	Integer pages;			// 페이지 수

	@Column(name = "PRICE")
	Integer price;			// 가격

	@Column(name = "REG_DATE", nullable = false)
	LocalDateTime regDate;	// 등록일시

	@Column(name = "BOOK_COVER", length = 300)
	String bookCover;		// 표지 이미지 경로/URL

	@PrePersist
	void onCreate() {
		this.regDate = LocalDateTime.now();
		if (this.reviewCount == null) this.reviewCount = 0;
		if (this.rating == null) this.rating = BigDecimal.ZERO;
	}

	@PreUpdate
	void onUpdate() {
		// REG_DATE 는 최초 등록일시를 유지 (수정시 변경하지 않음)
	}

	// ★한권의 책은 여러번 결제(구매)될수 있다.	(Book : KakaoPay = 1 : N)
	// 1. mappedBy = "book"          KakaoPay 엔티티의 book 필드와 연결 - 읽기만 가능 / 수정 x
	// 2. cascade = CascadeType.ALL  Book 변화(생성,수정,삭제 등)와 연결된 KakaoPay 에 반영
	// 3. orphanRemoval = true       도서 삭제시 결제내역도 함께 정리
	@Builder.Default
	@OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
	List<KakaoPay> payments = new ArrayList<>();	// 이 책의 결제(구매) 내역들

	// ★여러 회원이 여러 도서를 찜(위시리스트) 할 수 있다.	(Book : AppUser = N : N)
	@Builder.Default
	@ManyToMany
	@JoinTable(name = "BOOK_WISH",
		joinColumns = @JoinColumn(name = "BOOK_ID"),
		inverseJoinColumns = @JoinColumn(name = "APP_USER_ID")
	)
	List<AppUser> wishUsers = new ArrayList<>();	// 이 책을 찜한 회원들
}
