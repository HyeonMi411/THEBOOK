package com.thejoa703.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 도서 엔티티 (BOOK)
 * - DESCRIPTION 컬럼은 CLOB(대용량 문자열) 이므로 @Lob 처리
 * - 도서등록/수정/삭제는 관리자(ROLE_ADMIN)만 가능하도록 등록자(AppUser)와 연결
 */
@Entity
@Table(name = "BOOK")
@Getter @Setter
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
	private LocalDate publishDate;

	@Column(length = 50, nullable = false)
	private String category;

	@Column(length = 100)
	private String ranking;

	@Column(name = "REVIEW_COUNT")
	private Integer reviewCount = 0;

	@Column
	private Double rating;

	@Lob // 대용량데이터처리 - CLOB(문자열) : 도서 상세설명
	private String description;

	@Column
	private Integer pages;

	@Column
	private Integer price;

	@Column(name = "REG_DATE", nullable = false)
	private LocalDateTime regDate;

	@Column(name = "BOOK_COVER", length = 300)
	private String bookCover; // 표지이미지 경로 (실제 파일은 /uploads 에 저장, 경로 문자열만 컬럼에 저장)

	@PrePersist
	void onCreate() {
		this.regDate = LocalDateTime.now();
		if (this.reviewCount == null) { this.reviewCount = 0; }
	}

	@PreUpdate
	void onUpdate() {
		// REG_DATE 는 최초등록시점 그대로 유지 (수정시간 별도관리 필요시 UPDATED_AT 컬럼 추가)
	}

	// ★도서등록은 관리자만 가능하다 - 등록한 관리자 (AppUser)
	@ManyToOne
	@JoinColumn(name = "APP_USER_ID", nullable = false)
	private AppUser user;

	// ★이 도서의 재고 (1:1, BookStock 이 주인/PK공유 - 여기서는 조회 편의를 위한 역방향 매핑)
	@OneToOne(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
	private BookStock stock;
}
