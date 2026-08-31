package com.thejoa703.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 주문상품 엔티티 (ORDER_ITEMS)
 * - Orders 와 Book 은 N:M 관계이지만("한 주문에 여러 책", "한 책이 여러 주문에 포함"),
 *   quantity/price 라는 부가정보(주문 시점 스냅샷)가 있어 순수 @ManyToMany 대신
 *   중간 엔티티(OrderItem)로 명시적으로 모델링했습니다.
 *   (Orders --1:N--> OrderItem <--N:1-- Book)
 * - Book 이 FK 로 연결되어 있어서, 나중에 "이 책이 총 몇 권 팔렸는지" 같은 판매통계·
 *   베스트셀러 쿼리를 SQL 로 바로 집계할 수 있습니다.
 * - price/bookTitleSnapshot 은 **주문 시점 스냅샷**입니다. 이후 도서 가격이 바뀌거나
 *   도서가 삭제되어도 과거 주문 내역(얼마에, 무슨 제목으로 샀는지)은 그대로 보존됩니다.
 */
@Entity
@Table(name = "ORDER_ITEMS")
@Getter @Setter
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_item_seq")
	@SequenceGenerator(name = "order_item_seq", sequenceName = "ORDER_ITEM_SEQ", allocationSize = 1)
	private Long id;

	// 소속 주문 - N:1
	@ManyToOne
	@JoinColumn(name = "ORDER_ID", nullable = false)
	private Orders order;

	// 구매한 도서 - N:1
	@ManyToOne
	@JoinColumn(name = "BOOK_ID", nullable = false)
	private Book book;

	@Column(nullable = false)
	private Integer quantity; // 구매수량

	@Column(nullable = false)
	private Integer price; // 주문 시점 가격 스냅샷 (도서 가격변동과 무관하게 보존)

	@Column(name = "BOOK_TITLE_SNAPSHOT", length = 255)
	private String bookTitleSnapshot; // 주문 시점 도서명 스냅샷 (도서 삭제/제목변경과 무관하게 보존)
}
