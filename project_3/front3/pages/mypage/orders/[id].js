// pages/mypage/orders/[id].js
// 주문내역 목록(/mypage/orders)에서 특정 주문을 클릭했을 때 보여주는 상세 화면입니다.
// PENDING(결제대기) 상태인 주문은 여기서 바로 결제 이어하기가 가능하도록
// /order/checkout 으로 이동하는 버튼도 함께 보여줍니다.
import React, { useEffect } from 'react';
import { useRouter } from 'next/router';
import { useSelector, useDispatch } from 'react-redux';
import { fetchOrderDetailRequest, resetOrderState } from '../../../reducers/orderReducer';
import BookCoverImage from '../../../components/BookCoverImage';

const STATUS_LABEL = {
  PENDING: '결제대기',
  PAID: '결제완료',
  CANCELLED: '결제취소',
  FAILED: '결제실패',
};

export default function MyOrderDetailPage() {
  const router = useRouter();
  const { id } = router.query;
  const dispatch = useDispatch();

  const { currentOrder, loading, error } = useSelector((state) => state.order);
  const { user } = useSelector((state) => state.auth);

  useEffect(() => {
    if (!user) { router.replace('/login'); return; }
    if (!id) return;
    dispatch(fetchOrderDetailRequest(Number(id)));
    return () => { dispatch(resetOrderState()); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, user]);

  if (!user) return null;
  if (loading || !currentOrder) {
    return <div className="myorders-wrap">{error ? <p style={{ color: 'red' }}>{error}</p> : '불러오는 중...'}</div>;
  }

  return (
    <div className="myorders-wrap">
      <div className="cart-header">
        <h2>📦 주문 상세</h2>
      </div>

      <div className="checkout-card">
        <div className="myorder-card-header" style={{ marginBottom: 16 }}>
          <span className="myorder-id">주문번호 #{currentOrder.id}</span>
          <span className={`myorder-status ${currentOrder.orderStatus}`}>
            {STATUS_LABEL[currentOrder.orderStatus] || currentOrder.orderStatus}
          </span>
        </div>

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

        {currentOrder.orderStatus === 'PENDING' && (
          <button
            type="button"
            className="kakaopay-btn"
            onClick={() => router.push(`/order/checkout?orderId=${currentOrder.id}`)}
          >
            🟡 이어서 결제하기
          </button>
        )}

        <div className="btn-area" style={{ marginTop: 20 }}>
          <a
            className="btn btn-outline"
            onClick={(e) => { e.preventDefault(); router.push('/mypage/orders'); }}
            href="/mypage/orders"
          >
            목록으로
          </a>
        </div>
      </div>
    </div>
  );
}
