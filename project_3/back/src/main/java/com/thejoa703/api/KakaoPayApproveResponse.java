package com.thejoa703.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * 카카오페이 결제승인(approve) API 응답을 담는 Dto
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoPayApproveResponse {
    private String aid;               // 요청 고유번호
    private String tid;               // 결제 고유번호
    private String cid;               // 가맹점 코드
    private String status;            // 결제상태 (SUCCESS_PAYMENT 등)
    private String partner_order_id;  // 가맹점 주문번호 (우리 Orders.id)
    private String partner_user_id;   // 가맹점 회원 id (우리 AppUser.id)
    private String payment_method_type; // 결제수단 (CARD, MONEY 등)
    private String item_name;
    private Integer quantity;
    private String created_at;        // 결제 준비 시각
    private String approved_at;       // 결제 승인 시각
}
