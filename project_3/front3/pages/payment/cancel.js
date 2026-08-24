// pages/payment/cancel.js
// 카카오페이 결제창에서 사용자가 취소하면 cancel_url(=이 페이지)?orderId= 로 리다이렉트됩니다.
import React, { useEffect } from 'react';
import { useRouter } from 'next/router';
import { useSelector, useDispatch } from 'react-redux';
import { paymentCancelRequest, resetPaymentState } from '../../reducers/orderReducer';

export default function PaymentCancelPage() {
  const router = useRouter();
  const dispatch = useDispatch();
  const { orderId } = router.query;
  const { paymentLoading, paymentError } = useSelector((state) => state.order);

  useEffect(() => {
    if (!router.isReady || !orderId) return;
    dispatch(paymentCancelRequest(Number(orderId)));
    return () => { dispatch(resetPaymentState()); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [router.isReady, orderId]);

  return (
    <div className="payment-result-wrap">
      <div className="payment-result-card">
        <div className="payment-result-icon">🚫</div>
        <div className="payment-result-title">결제를 취소했습니다</div>
        <p className="payment-result-desc">
          {paymentLoading ? '처리중...' : (paymentError || '주문이 취소 처리되었습니다.')}
        </p>
        <a
          className="btn btn-primary-bs"
          onClick={(e) => { e.preventDefault(); router.push('/cart'); }}
          href="/cart"
        >
          장바구니로 돌아가기
        </a>
      </div>
    </div>
  );
}
