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
 * 장바구니 항목 엔티티 (CART_ITEM)
 * - 하나의 장바구니(Cart)에는 여러 도서가 담길 수 있고, 같은 도서도 여러 장바구니에
 *   담길 수 있는 다대다(N:M) 관계이지만, "담은 수량(quantity)"이라는 부가정보가 있어서
 *   순수 @ManyToMany 대신 중간 엔티티(CartItem)로 명시적으로 풀어서 모델링했습니다.
 *   (Cart --1:N--> CartItem <--N:1-- Book, 결과적으로 Cart-Book 은 N:M 관계와 동일합니다)
 */
@Entity
@Table(name = "CART_ITEM")
@Getter @Setter
public class CartItem {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cart_item_seq")
	@SequenceGenerator(name = "cart_item_seq", sequenceName = "CART_ITEM_SEQ", allocationSize = 1)
	private Long id;

	// 소속 장바구니 - N:1
	@ManyToOne
	@JoinColumn(name = "CART_ID", nullable = false)
	private Cart cart;

	// 담긴 도서 - N:1
	@ManyToOne
	@JoinColumn(name = "BOOK_ID", nullable = false)
	private Book book;

	@Column(nullable = false)
	private Integer quantity; // 담은 수량
}
