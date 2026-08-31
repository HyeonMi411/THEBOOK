package com.thejoa703.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	// ★주문한 사용자 - N:1 (한 사용자가 여러 번 주문할 수 있음)
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

	@Lob // ★대용량데이터처리 - CLOB(문자열) : 카카오페이 API 원본 응답(JSON), 감사/디버깅용
	@Column(name = "KAKAO_RESPONSE_JSON")
	private String kakaoResponseJson;

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "APPROVED_AT")
	private LocalDateTime approvedAt; // 결제 승인 완료 시각 (결제완료 전에는 null)

	// ★사용자가 "삭제"한 결제완료/취소/실패 주문 - 실제 DB 레코드는 회계·이력 보존을 위해
	//   남겨두고, 이 플래그만 true 로 바꿔서 "내 주문내역 목록"에서만 안 보이게 합니다.
	//   (결제전(PENDING) 주문은 실제 거래 기록이 없으므로 이 플래그 없이 진짜로 삭제합니다)
	// ★columnDefinition 으로 DEFAULT 0(false) 을 명시했습니다. 이게 없으면 Hibernate 가
	//   생성하는 ALTER TABLE 문이 "ALTER TABLE ORDERS ADD HIDDEN_BY_USER NUMBER(1) NOT NULL"
	//   형태가 되는데, ORDERS 테이블에 이미 주문 데이터가 있는 상태에서는 Oracle 이
	//   "기본값 없이 NOT NULL 컬럼을 추가"하는 걸 거부합니다(기존 행들이 채울 값이 없으므로).
	//   ddl-auto:update 는 이런 개별 DDL 실패를 대체로 로그 경고만 남기고 넘어가기 때문에,
	//   애플리케이션은 정상적으로 뜨는데 실제로는 컬럼이 안 만들어져서 "ORA-00904: 부적합한
	//   식별자" 에러가 나중에야 발견되는 문제가 있었습니다. columnDefinition 에 DEFAULT 를
	//   명시하면 Hibernate 가 "ALTER TABLE ORDERS ADD HIDDEN_BY_USER NUMBER(1) DEFAULT 0
	//   NOT NULL" 형태로 DDL을 생성해서, 기존 행에도 안전하게 기본값이 채워지며 컬럼이
	//   추가됩니다 - 서버를 완전히 재시작하기만 하면 자동으로 반영됩니다.
	@Column(name = "HIDDEN_BY_USER", nullable = false, columnDefinition = "NUMBER(1) DEFAULT 0")
	private boolean hiddenByUser = false;

	// ★주문에 담긴 상품들 - 1:N (Orders 삭제시 상품도 함께 삭제)
	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
		if (this.orderStatus == null) { this.orderStatus = OrderStatus.PENDING; }
	}
}
