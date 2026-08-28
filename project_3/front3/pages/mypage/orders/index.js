// pages/mypage/orders/index.js
import React, { useEffect } from 'react';
import { useRouter } from 'next/router';
import { useSelector, useDispatch } from 'react-redux';
import { fetchMyOrdersRequest, deleteOrderRequest, resetOrderState } from '../../../reducers/orderReducer';
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
    // ★AppLayout 에서 accessToken 으로 user 를 다시 불러오는 중(loadUserRequest)일 수 있으므로,
    //   진짜 비로그인(토큰 자체가 없음)일 때만 즉시 로그인 화면으로 보냅니다. 토큰은 있는데
    //   user 복원이 아직 안 끝난 경우엔 여기서 섣불리 로그인으로 튕기지 않고 기다립니다.
    const hasToken = typeof window !== 'undefined' && !!localStorage.getItem('accessToken');
    if (!user && !hasToken) { router.replace('/login'); return; }
    if (!user) return; // user 복원 대기중
    if (!router.isReady) return;
    const { page } = router.query;
    dispatch(fetchMyOrdersRequest({ page: Number(page) || 1, size: 12 }));
    return () => { dispatch(resetOrderState()); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [router.isReady, router.query.page, user]);

  const handlePageChange = (page) => {
    router.push({ pathname: '/mypage/orders', query: { page } }, undefined, { scroll: true });
  };

  // ★결제전(PENDING) 주문만 삭제 가능 - 목록 클릭(상세이동)과 이벤트가 겹치지 않도록 stopPropagation
  const handleDelete = (e, orderId) => {
    e.stopPropagation();
    if (window.confirm('이 주문을 삭제하시겠습니까?')) {
      dispatch(deleteOrderRequest(orderId));
    }
  };

  if (!user) return <div style={{ textAlign: 'center', padding: 60, color: '#999' }}>로그인 확인 중...</div>;

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
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div className="myorder-total">{order.totalAmount?.toLocaleString()}원</div>
            {/* ★모든 상태에서 삭제 가능 - 결제전(PENDING)은 실제 삭제, 결제완료/취소/실패는
                DB에는 그대로 두고 내 목록에서만 안 보이게(숨기기) 처리됩니다. */}
            <button
              type="button"
              className="myorder-delete-btn"
              onClick={(e) => handleDelete(e, order.id)}
            >
              삭제
            </button>
          </div>
        </div>
      ))}

      <Pagination currentPage={currentPage} totalPages={totalPages} onChange={handlePageChange} />
    </div>
  );
}
