// pages/mypage/orders/index.js
import React, { useEffect } from 'react';
import { useRouter } from 'next/router';
import { useSelector, useDispatch } from 'react-redux';
import { fetchMyOrdersRequest, resetOrderState } from '../../../reducers/orderReducer';
import Pagination from '../../../components/Pagination';

const STATUS_LABEL = {
  PENDING: '결제대기',
  PAID: '결제완료',
  CANCELLED: '결제취소',
  FAILED: '결제실패',
};

export default function MyOrdersPage() {
  const router = useRouter();
  const dispatch = useDispatch();
  const { orders, currentPage, totalPages, loading, error } = useSelector((state) => state.order);
  const { user } = useSelector((state) => state.auth);
  const orderList = orders || []; // ★orders 가 어떤 이유로든 배열이 아닐 때도 화면이 깨지지 않도록 방어

  useEffect(() => {
    if (!user) { router.replace('/login'); return; }
    if (!router.isReady) return;
    const { page } = router.query;
    dispatch(fetchMyOrdersRequest({ page: Number(page) || 1, size: 12 }));
    return () => { dispatch(resetOrderState()); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [router.isReady, router.query.page, user]);

  const handlePageChange = (page) => {
    router.push({ pathname: '/mypage/orders', query: { page } }, undefined, { scroll: true });
  };

  if (!user) return null;

  return (
    <div className="myorders-wrap">
      <h2 style={{ marginBottom: 20 }}>📋 내 주문내역</h2>

      {loading && <p style={{ textAlign: 'center' }}>불러오는 중...</p>}
      {error && <p style={{ color: 'red', textAlign: 'center' }}>{error}</p>}

      {!loading && orderList.length === 0 && (
        <div className="notice-empty">주문내역이 없습니다.</div>
      )}

      {orderList.map((order) => (
        <div
          key={order.id}
          className="myorder-card"
          style={{ cursor: 'pointer' }}
          onClick={() => router.push(`/mypage/orders/${order.id}`)}
        >
          <div className="myorder-card-header">
            <span className="myorder-id">주문번호 #{order.id}</span>
            <span className={`myorder-status ${order.orderStatus}`}>{STATUS_LABEL[order.orderStatus] || order.orderStatus}</span>
          </div>
          <div className="myorder-items">
            {order.items?.map((item) => item.bookTitle).join(', ')}
          </div>
          <div className="myorder-total">{order.totalAmount?.toLocaleString()}원</div>
        </div>
      ))}

      <Pagination currentPage={currentPage} totalPages={totalPages} onChange={handlePageChange} />
    </div>
  );
}
