// pages/payment/fail.js
// 카카오페이 결제 중 오류/거절 등으로 실패하면 fail_url(=이 페이지)?orderId= 로 리다이렉트됨.
import React, { useEffect } from 'react';
import { useRouter } from 'next/router';
import { useSelector, useDispatch } from 'react-redux';
import { paymentFailRequest, resetPaymentState } from '../../reducers/orderReducer';

export default function PaymentFailPage() {
  const router = useRouter();
  const dispatch = useDispatch();
  const { orderId } = router.query;
  const { paymentLoading, paymentError } = useSelector((state) => state.order);

  useEffect(() => {
    if (!router.isReady || !orderId) return;
    dispatch(paymentFailRequest(Number(orderId)));
    return () => { dispatch(resetPaymentState()); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [router.isReady, orderId]);

  return (
    <div className="payment-result-wrap">
      <div className="payment-result-card">
        <div className="payment-result-icon">⚠️</div>
        <div className="payment-result-title">결제에 실패했습니다</div>
        <p className="payment-result-desc">
          {paymentLoading ? '처리중...' : (paymentError || '결제 진행 중 문제가 발생했습니다. 다시 시도해주세요.')}
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
