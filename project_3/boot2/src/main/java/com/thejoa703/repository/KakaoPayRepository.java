package com.thejoa703.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.KakaoPay;
import com.thejoa703.entity.KakaoPay.PaymentStatus;

@Repository										// Entity , PK
public interface KakaoPayRepository extends JpaRepository<KakaoPay, Long> {

	// tid(결제 고유번호)로 단건조회 - 결제승인/조회 콜백때 사용
	Optional<KakaoPay> findByTid(String tid);

	// 가맹점 주문번호로 단건조회
	Optional<KakaoPay> findByPartnerOrderId(String partnerOrderId);

	// 특정 회원의 결제내역 전체조회 (최신순)
	List<KakaoPay> findByUser_IdOrderByRegDateDesc(Long appUserId);

	// 특정 도서의 결제내역 전체조회
	List<KakaoPay> findByBook_BookId(Long bookId);

	// 특정 회원 + 결제상태로 조회  (예: 승인완료 내역만)
	List<KakaoPay> findByUser_IdAndPaymentStatus(Long appUserId, PaymentStatus paymentStatus);

	// 결제상태별 전체조회
	List<KakaoPay> findByPaymentStatus(PaymentStatus paymentStatus);

	// 특정회원이 특정도서를 결제(구매)한 이력 존재여부 (구매완료 여부 체크)
	boolean existsByUser_IdAndBook_BookIdAndPaymentStatus(Long appUserId, Long bookId, PaymentStatus paymentStatus);

	// 특정 회원의 결제건수 집계
	long countByUser_IdAndPaymentStatus(Long appUserId, PaymentStatus paymentStatus);

	// 결제내역 페이징 (Oracle ROWNUM) - 특정회원 기준
	@Query(
			value = "SELECT * FROM ( " +
					"SELECT k.*, ROWNUM AS rnum " +
					"FROM (SELECT * FROM KAKAO_PAY WHERE APP_USER_ID = :userId ORDER BY REG_DATE DESC) k " +
					") " +
					"WHERE rnum BETWEEN :start AND :end",
			nativeQuery = true
	)
	List<KakaoPay> findMyPaymentsWithPaging(@Param("userId") Long userId,
											 @Param("start") int start, @Param("end") int end);
}

/*
(1) 사용할수 있는 기본 SQL
	1. CREATE : save       - insert into  kakao_pay ( 컬럼1, 컬럼2,,,)  values (?,?,,,,)
	2. READ   : findAll    - select * from kakao_pay
				findById   - select * from kakao_pay where kakao_pay_id=?
	3. UPDATE : save       - update  테이블명  set  컬럼1=?,,,, where kakao_pay_id=?
	4. DELETE : deleteById - delete from kakao_pay where kakao_pay_id=?

(2) 검색       : findBy필드명
(3) 복잡한 sql : @Query
*/
