package com.thejoa703.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.KakaoPay;

@Mapper
public interface KakaoPayMapper {

	// tid(결제 고유번호)로 단건조회
	KakaoPay findByTid(String tid);

	// 가맹점 주문번호로 단건조회
	KakaoPay findByPartnerOrderId(String partnerOrderId);

	// 특정 회원의 결제내역 전체조회(최신순)
	List<KakaoPay> findByUserId(Long appUserId);

	// 특정 도서의 결제내역 전체조회
	List<KakaoPay> findByBookId(Long bookId);

	// 결제상태별 조회 (READY, APPROVED, CANCELED, FAILED)
	List<KakaoPay> findByPaymentStatus(String paymentStatus);

	// 결제내역 페이징 - 특정 회원 기준
	List<KakaoPay> findMyPaymentsWithPaging(Map<String, Object> params);	// params: userId, start, end

	// 등록 (결제 준비/ready 단계)
	int insertKakaoPay(KakaoPay kakaoPay);

	// 결제승인 처리 (tid 기준 상태/승인시각 갱신)
	int approvePayment(Map<String, Object> params);	// params: tid, approvedAt

	// 결제취소 처리
	int cancelPayment(Map<String, Object> params);		// params: tid, canceledAt, cancelAmount

	// 전체 수정
	int updateKakaoPay(KakaoPay kakaoPay);

	// 삭제
	int deleteKakaoPay(Long kakaoPayId);
}
