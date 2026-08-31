// pages/order/checkout.js
// 장바구니 선택항목(?cartItemIds=1,2,3) 또는 바로구매로 만들어진 주문(?orderId=)을
// 확인하고, 카카오페이 결제를 요청하는 화면입니다.
import React, { useEffect } from 'react';
import { useRouter } from 'next/router';
import { useSelector, useDispatch } from 'react-redux';
import {
  createOrderRequest, fetchOrderDetailRequest, resetOrderState,
  paymentReadyRequest, resetPaymentState,
} from '../../reducers/orderReducer';
import BookCoverImage from '../../components/BookCoverImage';

export default function CheckoutPage() {
  const router = useRouter();
  const dispatch = useDispatch();
  const { cartItemIds, orderId } = router.query;

  const { currentOrder, loading, error, paymentLoading, paymentError, redirectUrl } = useSelector((state) => state.order);
  const { user } = useSelector((state) => state.auth);

  useEffect(() => {
    const hasToken = typeof window !== 'undefined' && !!localStorage.getItem('accessToken');
    if (!user && !hasToken) { router.replace('/login'); return; }
    if (!user) return; // user 복원 대기중 (AppLayout 의 loadUserRequest)
    if (!router.isReady) return;

    if (orderId) {
      // 바로구매: 이미 생성된 주문을 조회
      dispatch(fetchOrderDetailRequest(Number(orderId)));
    } else if (cartItemIds) {
      // 장바구니 결제: 선택한 항목으로 새 주문 생성
      const ids = cartItemIds.split(',').map(Number);
      dispatch(createOrderRequest({ cartItemIds: ids }));
    } else {
      router.replace('/cart');
    }

    return () => { dispatch(resetOrderState()); dispatch(resetPaymentState()); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [router.isReady, user]);

  // 결제준비 성공 → 카카오페이 결제창(redirectUrl)으로 이동
  useEffect(() => {
    if (redirectUrl) {
      window.location.href = redirectUrl;
    }
  }, [redirectUrl]);

  const handlePay = () => {
    if (!currentOrder) return;
    dispatch(paymentReadyRequest(currentOrder.id));
  };

  if (!user) return <div className="checkout-wrap" style={{ textAlign: 'center', color: '#999' }}>로그인 확인 중...</div>;
  if (loading || !currentOrder) return <div className="checkout-wrap">주문 준비중...</div>;
  if (error) return <div className="checkout-wrap" style={{ color: 'red' }}>{error}</div>;

  return (
    <div className="checkout-wrap">
      <h2 style={{ marginBottom: 20 }}>📦 주문/결제 확인</h2>

      <div className="checkout-card">
        {currentOrder.items.map((item) => (
          <div key={item.id} className="checkout-item-row">
            <div className="checkout-item-cover">
              <BookCoverImage
                src={item.bookCover
                  ? (item.bookCover.startsWith('http') ? item.bookCover : `http://localhost:8080/${item.bookCover}`)
                  : null}
                alt={item.bookTitle}
                iconSize={18}
              />
            </div>
            <div>
              <div className="checkout-item-title">{item.bookTitle}</div>
              <div className="checkout-item-meta">{item.price?.toLocaleString()}원 × {item.quantity}권</div>
            </div>
            <div className="checkout-item-price">
              {(item.price * item.quantity).toLocaleString()}원
            </div>
          </div>
        ))}

        <div className="checkout-total-row">
          <span>총 결제금액</span>
          <span>{currentOrder.totalAmount?.toLocaleString()}원</span>
        </div>

        <button type="button" className="kakaopay-btn" onClick={handlePay} disabled={paymentLoading}>
          {paymentLoading ? '결제 준비중...' : '🟡 카카오페이로 결제하기'}
        </button>
        {paymentError && <p style={{ color: 'red', marginTop: 10 }}>{paymentError}</p>}
      </div>
    </div>
  );
}
