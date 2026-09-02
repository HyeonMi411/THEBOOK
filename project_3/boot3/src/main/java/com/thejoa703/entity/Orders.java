package com.thejoa703.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 주문 헤더 엔티티 (ORDERS)
 * - 테이블명을 단수 "ORDER" 가 아닌 "ORDERS" 로 한 이유: ORDER 는 Oracle/SQL 예약어라서
 *   테이블명으로 그대로 쓰면 매 쿼리마다 따옴표 처리가 필요해 충돌 위험이 있습니다.
 * - 실제 구매한 도서 목록은 OrderItem 에서 관리합니다 (Orders 1 : N OrderItem).
 * - 카카오페이 결제 승인(approve) API 의 응답 원문(JSON)은 대용량 문자열이 될 수 있어
 *   CLOB(@Lob) 으로 저장합니다. 승인 실패 원인 분석/감사(audit) 목적입니다.
 */
@Entity
@Table(name = "ORDERS")
@Getter @Setter
public class Orders {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
	@SequenceGenerator(name = "orders_seq", sequenceName = "ORDERS_SEQ", allocationSize = 1)
	private Long id;

	// 주문한 사용자 - N:1 (한 사용자가 여러 번 주문할 수 있음)
	@ManyToOne
	@JoinColumn(name = "APP_USER_ID", nullable = false)
	private AppUser user;

	@Column(name = "TOTAL_AMOUNT", nullable = false)
	private Integer totalAmount; // 총 결제금액

	@Enumerated(EnumType.STRING)
	@Column(name = "ORDER_STATUS", nullable = false, length = 20)
	private OrderStatus orderStatus = OrderStatus.PENDING; // 대기/완료/취소/실패

	@Column(name = "TID", length = 100)
	private String tid; // 카카오페이 거래 고유번호 (결제준비 응답에서 발급)

	@Lob // 대용량데이터처리 - CLOB(문자열) : 카카오페이 API 원본 응답(JSON), 감사/디버깅용
	@Column(name = "KAKAO_RESPONSE_JSON")
	private String kakaoResponseJson;

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "APPROVED_AT")
	private LocalDateTime approvedAt; // 결제 승인 완료 시각 (결제완료 전에는 null)

	// 사용자가 삭제한 결제완료/취소/실패 주문 - 회계·이력 보존을 위해 DB에는 남기고
	// 이 플래그만 true 로 바꿔서 "내 주문내역 목록"에서만 안 보이게 합니다.
	// (결제전(PENDING) 주문은 실제 거래 기록이 없으므로 이 플래그 없이 진짜로 삭제합니다)
	@Column(name = "HIDDEN_BY_USER", nullable = false, columnDefinition = "NUMBER(1) DEFAULT 0")
	private boolean hiddenByUser = false;

	// 주문에 담긴 상품들 - 조회 편의를 위한 매핑입니다. cascade 는 걸지 않고, 주문 삭제시
	// 자식(OrderItem)을 먼저 지우는 순서는 OrderService 에서 명시적으로 관리합니다.
	@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
	private List<OrderItem> items = new ArrayList<>();

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
		if (this.orderStatus == null) { this.orderStatus = OrderStatus.PENDING; }
	}
}
