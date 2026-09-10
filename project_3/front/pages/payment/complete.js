// pages/payment/complete.js
// 카카오페이 결제창에서 결제를 마치면 approval_url(=이 페이지)?orderId=&pg_token= 로
// 리다이렉트됨. 로그인 상태(JWT)가 있는 이 프론트 화면에서 백엔드 승인 API를
// AJAX 로 호출해 최종 승인 처리.
import React, { useEffect } from 'react';
import { useRouter } from 'next/router';
import { useSelector, useDispatch } from 'react-redux';
import { paymentApproveRequest, resetPaymentState, resetOrderState } from '../../reducers/orderReducer';

export default function PaymentCompletePage() {
  const router = useRouter();
  const dispatch = useDispatch();
  const { orderId, pg_token: pgToken } = router.query;
  const { currentOrder, paymentLoading, paymentError } = useSelector((state) => state.order);

  useEffect(() => {
    if (!router.isReady) return;
    if (orderId && pgToken) {
      dispatch(paymentApproveRequest({ orderId: Number(orderId), pgToken }));
    }
    return () => { dispatch(resetPaymentState()); dispatch(resetOrderState()); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [router.isReady]);

  return (
    <div className="payment-result-wrap">
      <div className="payment-result-card">
        {paymentLoading && (
          <>
            <div className="payment-result-icon">⏳</div>
            <div className="payment-result-title">결제 승인 처리중...</div>
            <p className="payment-result-desc">잠시만 기다려주세요.</p>
          </>
        )}

        {!paymentLoading && paymentError && (
          <>
            <div className="payment-result-icon">⚠️</div>
            <div className="payment-result-title">결제 승인 실패</div>
            <p className="payment-result-desc">{paymentError}</p>
            <a
              className="btn btn-primary-bs"
              onClick={(e) => { e.preventDefault(); router.push('/mypage/orders'); }}
              href="/mypage/orders"
            >
              주문내역으로 이동
            </a>
          </>
        )}

        {!paymentLoading && !paymentError && currentOrder && (
          <>
            <div className="payment-result-icon">✅</div>
            <div className="payment-result-title">결제가 완료되었습니다!</div>
            <p className="payment-result-desc">주문하신 도서 정보를 확인해주세요.</p>

            <div className="payment-order-summary">
              <div><span>주문번호</span><span>#{currentOrder.id}</span></div>
              <div><span>결제금액</span><span>{currentOrder.totalAmount?.toLocaleString()}원</span></div>
              <div><span>구매도서</span><span>{currentOrder.items?.length}권</span></div>
            </div>

            <a
              className="btn btn-primary-bs"
              onClick={(e) => { e.preventDefault(); router.push('/mypage/orders'); }}
              href="/mypage/orders"
            >
              주문내역 보기
            </a>
          </>
        )}
      </div>
    </div>
  );
}
