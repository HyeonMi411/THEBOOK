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

	@Column(name = "PUBLISH_DATE") // 출판일은 카카오/국립중앙도서관 자동수집 시 원본에 값이 없거나
	// 파싱에 실패할 수 있어서 nullable 로 뒀습니다. 예전에는 이럴 때 임의로
	// 1900-01-01 을 채워넣었는데, 이건 "정말 1900년에 나온 책"인지 "출판일을 못
	// 구했는지" 구분이 안 되는 매직넘버라 잘못된 정보였습니다. 지금은 null 로 두고
	// 화면에서 "출판일 미상"으로 명확히 표시합니다. (관리자가 직접 등록할 때는
	// BookRequestDto.publishDate 가 @NotNull 이라 여전히 필수 입력입니다)
	// 기존 DB에 이미 이 컬럼이 NOT NULL 로 만들어져 있었다면, ddl-auto:update 가
	// 이 제약을 자동으로 풀어주지 못할 수 있어서, SchemaAutoFixRunner(config 패키지)가
	// 서버 기동시 직접 확인해서 자동으로 고쳐줍니다. 별도 수동 SQL 실행이 필요 없습니다.
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

	// 소프트 삭제 - 관리자가 "삭제"해도 실제 DB 레코드는 남겨둡니다. CART_ITEM/ORDER_ITEMS 가
	// BOOK_ID 를 FK 로 참조하고 있어서, 장바구니에 담겼거나 한 번이라도 주문된 도서를 실제로
	// 하드 삭제하면 FK 제약조건 위반(ORA-02292)이 발생합니다. columnDefinition 으로 DEFAULT 0
	// 을 명시해서, 이미 도서 데이터가 있는 테이블에도 안전하게 컬럼이 추가되도록 했습니다.
	@Column(name = "DELETED", nullable = false, columnDefinition = "NUMBER(1) DEFAULT 0")
	private boolean deleted = false;

	@PrePersist
	void onCreate() {
		this.regDate = LocalDateTime.now();
		if (this.reviewCount == null) { this.reviewCount = 0; }
	}

	@PreUpdate
	void onUpdate() {
		// REG_DATE 는 최초등록시점 그대로 유지 (수정시간 별도관리 필요시 UPDATED_AT 컬럼 추가)
	}

	// 도서등록은 관리자만 가능하다 - 등록한 관리자 (AppUser)
	@ManyToOne
	@JoinColumn(name = "APP_USER_ID", nullable = false)
	private AppUser user;

	// 이 도서의 재고 - MyBatis 는 자동 로딩을 안 해주므로, BookMapper.xml 의
	// resultMap(association)이 BOOK_STOCK 을 JOIN 해서 직접 채워줍니다.
	private BookStock stock;
}
