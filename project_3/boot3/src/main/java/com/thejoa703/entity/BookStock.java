package com.thejoa703.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 도서 재고 엔티티 (BOOK_STOCK)
 * - Book 과 1:1 관계이며, PK를 Book.id 와 그대로 공유합니다. 즉 BOOK_ID 컬럼 하나가
 *   PK 이면서 동시에 Book 을 가리키는 FK 역할을 합니다.
 * - 결제 승인 시 여러 사용자가 같은 도서를 동시에 구매할 수 있어 재고차감에 동시성 문제가
 *   생길 수 있습니다. version 필드로 낙관적 락을 재현합니다(BookStockMapper.updateWithVersionCheck)
 *   — 갱신 SQL에 WHERE VERSION = ? 조건을 걸어, 먼저 갱신된 쪽만 성공하고 나중 쪽은
 *   영향받은 행이 0건이 되어 충돌을 감지할 수 있습니다.
 */
@Entity
@Table(name = "BOOK_STOCK")
@Getter @Setter
public class BookStock {

	@Id
	@Column(name = "BOOK_ID")
	private Long bookId; // Book 의 PK 를 그대로 공유 (별도 시퀀스 없음)

	@Column(name = "STOCK_QUANTITY", nullable = false)
	private Integer stockQuantity = 0; // 재고수량

	@Column(name = "UPDATED_AT", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "VERSION", nullable = false)
	private Long version; // 낙관적 락 - 재고차감 동시성 제어

	@PrePersist
	@PreUpdate
	void onSave() {
		this.updatedAt = LocalDateTime.now();
		if (this.stockQuantity == null) { this.stockQuantity = 0; }
	}
}
