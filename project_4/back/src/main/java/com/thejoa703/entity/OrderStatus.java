package com.thejoa703.entity;

/**
 * 주문 상태
 * - PENDING   : 결제대기 (주문 생성 직후, 카카오페이 결제준비/진행 중)
 * - PAID      : 결제완료 (카카오페이 승인 성공, 재고 차감 완료)
 * - CANCELLED : 결제취소 (사용자가 결제창에서 취소)
 * - FAILED    : 결제실패 (카카오페이 결제 중 오류/거절)
 */
public enum OrderStatus {
    PENDING,
    PAID,
    CANCELLED,
    FAILED
}
