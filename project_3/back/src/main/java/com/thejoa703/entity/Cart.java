package com.thejoa703.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 장바구니 헤더 엔티티 (CART)
 * - 사용자당 장바구니는 1개뿐이므로 AppUser 와 1:1 관계임.
 * - 실제 담긴 도서 목록은 CartItem 에서 관리.
 */
@Entity
@Table(name = "CART")
@Getter @Setter
public class Cart {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cart_seq")
	@SequenceGenerator(name = "cart_seq", sequenceName = "CART_SEQ", allocationSize = 1)
	private Long id;

	// 장바구니 주인 (사용자당 1개) - 1:1
	@OneToOne
	@JoinColumn(name = "APP_USER_ID", nullable = false, unique = true)
	private AppUser user;

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
	}
}
