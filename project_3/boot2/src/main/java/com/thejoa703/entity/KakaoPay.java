package com.thejoa703.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "KAKAO_PAY")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class KakaoPay {

	@Id		// jakarta.persistence.Id;
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kakao_pay_seq")
	@SequenceGenerator(name = "kakao_pay_seq", sequenceName = "KAKAO_PAY_SEQ", allocationSize = 1)
	@Column(name = "KAKAO_PAY_ID")
	Long kakaoPayId;

	// ★한 회원은 여러번 결제할 수 있다.	(KakaoPay : AppUser = N : 1)
	// 기존 partnerUserId(String) 대신 AppUser 를 직접 참조하도록 변경
	@ManyToOne
	@JoinColumn(name = "APP_USER_ID", nullable = false)
	AppUser user;			// 결제(구매)한 회원		@ManyToOne		AppUser

	// ★한권의 책은 여러번 결제될 수 있다.	(KakaoPay : Book = N : 1)
	// 기존 orderId(Long), itemName(String) 대신 Book 을 직접 참조하도록 변경
	@ManyToOne
	@JoinColumn(name = "BOOK_ID", nullable = false)
	Book book;				// 결제(구매)된 도서		@ManyToOne		Book

	@Column(name = "TID", length = 100)
	String tid;				// 결제 준비(ready) 응답으로 받는 결제 고유번호

	@Column(name = "PARTNER_ORDER_ID", nullable = false, length = 100)
	String partnerOrderId;	// 가맹점 주문번호

	@Column(name = "QUANTITY")
	Integer quantity;		// 상품 수량

	@Column(name = "TOTAL_AMOUNT")
	Integer totalAmount;	// 총 결제 금액

	@Column(name = "TAX_FREE_AMOUNT")
	Integer taxFreeAmount;	// 비과세 금액

	@Column(name = "VAT_AMOUNT")
	Integer vatAmount;		// 부가세 금액

	@Column(name = "PAYMENT_METHOD_TYPE", length = 20)
	String paymentMethodType;	// CARD, MONEY 등 결제 수단

	@Enumerated(EnumType.STRING)
	@Column(name = "PAYMENT_STATUS", length = 20, nullable = false)
	PaymentStatus paymentStatus;	// READY, APPROVED, CANCELED, FAILED

	@Column(name = "CID", length = 20)
	String cid;				// 가맹점 코드 (테스트: TC0ONETIME)

	@Column(name = "PG_TOKEN", length = 300)
	String pgToken;			// 결제승인 요청에 필요한 토큰 (approve 리다이렉트 시 전달)

	@Column(name = "APPROVED_AT")
	LocalDateTime approvedAt;	// 결제 승인 시각 (카카오페이 응답값)

	@Column(name = "CANCELED_AT")
	LocalDateTime canceledAt;	// 결제 취소 시각

	@Column(name = "CANCEL_AMOUNT")
	Integer cancelAmount;		// 취소 금액

	@Column(name = "FAIL_REASON", length = 500)
	String failReason;			// 결제 실패/취소 사유

	@Column(name = "CARD_INFO", length = 300)
	String cardInfo;			// 카드사, 카드종류 등 결제수단 상세 (JSON 문자열로 저장 가능)

	@Column(name = "REG_DATE", nullable = false)
	LocalDateTime regDate;		// 결제 준비 요청 등록일시

	@Column(name = "MOD_DATE")
	LocalDateTime modDate;		// 최종 수정일시(승인/취소 등 상태 변경 시)

	@PrePersist
	void onCreate() {
		this.regDate = LocalDateTime.now();
		this.modDate = LocalDateTime.now();
		if (this.paymentStatus == null) this.paymentStatus = PaymentStatus.READY;
		if (this.quantity == null) this.quantity = 1;
	}

	@PreUpdate
	void onUpdate() {
		this.modDate = LocalDateTime.now();
	}

	public enum PaymentStatus {
		READY,		// 결제 준비 (tid 발급 완료, 승인 대기)
		APPROVED,	// 결제 승인 완료
		CANCELED,	// 결제 취소
		FAILED		// 결제 실패
	}
}
