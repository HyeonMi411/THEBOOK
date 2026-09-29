package com.thejoa703.event;

/** 결제 승인이 끝난 뒤(트랜잭션 커밋 후) 알림(이메일/문자)을 보내기 위한 이벤트 */
public record OrderPaidEvent(Long orderId) {
}
