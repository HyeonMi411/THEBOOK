package com.thejoa703.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * 도서 재고 엔티티 (BOOK_STOCK)
 * - Book 과 1:1 관계이며, PK를 Book.id 와 공유(@MapsId). 즉 BOOK_ID 컬럼 하나가
 *   PK 이면서 동시에 Book 을 가리키는 FK 역할.
 * - 결제 승인 시 여러 사용자가 같은 도서를 동시에 구매할 수 있어 재고차감에 동시성 문제가
 *   생길 수 있음. @Version(낙관적 락)으로 대응 — 두 트랜잭션이 동시에 같은 재고를
 *   차감하려 하면, 먼저 커밋된 쪽만 성공하고 나중 쪽은 OptimisticLockException 이 발생해서
 *   재시도하거나 실패 처리할 수 있음. 결제승인처럼 즉시 확정이 필요한 임계구간은
 *   BookStockRepository.findByIdForUpdate() 의 비관적 락(FOR UPDATE)으로 추가 보강.
 */
@Entity
@Table(name = "BOOK_STOCK")
@Getter @Setter
public class BookStock {

	@Id
	@Column(name = "BOOK_ID")
	private Long bookId; // Book 의 PK 를 그대로 공유 (별도 시퀀스 없음)

	@OneToOne
	@MapsId // 위 bookId 필드값을 Book 엔티티의 PK 에서 그대로 가져와 채웁니다.
	@JoinColumn(name = "BOOK_ID")
	private Book book;

	@Column(name = "STOCK_QUANTITY", nullable = false)
	private Integer stockQuantity = 0; // 재고수량

	@Column(name = "UPDATED_AT", nullable = false)
	private LocalDateTime updatedAt;

	@Version // 낙관적 락 - 재고차감 동시성 제어 (JPA 가 버전 체크/증가를 자동으로 처리)
	@Column(name = "VERSION", nullable = false)
	private Long version;

	@PrePersist
	@PreUpdate
	void onSave() {
		this.updatedAt = LocalDateTime.now();
		if (this.stockQuantity == null) { this.stockQuantity = 0; }
	}
}
