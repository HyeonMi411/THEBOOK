// pages/cart/index.js
import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { useSelector, useDispatch } from 'react-redux';
import {
  fetchCartRequest, updateCartItemRequest, removeCartItemRequest,
  toggleSelectItem, selectAllItems, clearSelection,
} from '../../reducers/cartReducer';
import BookCoverImage from '../../components/BookCoverImage';

export default function CartPage() {
  const router = useRouter();
  const dispatch = useDispatch();
  const { items, totalAmount, loading, error, selectedIds = [] } = useSelector((state) => state.cart);
  const { user } = useSelector((state) => state.auth);

  useEffect(() => {
    const hasToken = typeof window !== 'undefined' && !!localStorage.getItem('accessToken');
    if (!user && !hasToken) {
      router.replace('/login');
      return;
    }
    if (!user) return; // user 복원 대기중 (AppLayout 의 loadUserRequest)
    dispatch(fetchCartRequest());
  }, [dispatch, user, router]);

  useEffect(() => {
    // 목록 갱신시 기본으로 전체선택
    if (items.length > 0) dispatch(selectAllItems());
    else dispatch(clearSelection());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items.length]);

  const handleQuantityChange = (itemId, currentQty, delta, stockQuantity) => {
    const next = currentQty + delta;
    if (next < 1) return;
    if (next > stockQuantity) {
      alert(`재고가 부족합니다. (현재 재고 : ${stockQuantity}권)`);
      return;
    }
    dispatch(updateCartItemRequest({ itemId, quantity: next }));
  };

  const handleRemove = (itemId) => {
    if (window.confirm('장바구니에서 삭제하시겠습니까?')) {
      dispatch(removeCartItemRequest(itemId));
    }
  };

  const selectedItems = items.filter((item) => selectedIds.includes(item.id));
  const selectedTotal = selectedItems.reduce((sum, item) => sum + item.subtotal, 0);

  const handleCheckout = () => {
    if (selectedIds.length === 0) {
      alert('결제할 도서를 선택해주세요.');
      return;
    }
    // 체크박스 자체를 막아뒀지만, 혹시 모를 상태 불일치에 대비해 한 번 더 방어.
    const hasDeletedSelected = items.some((item) => selectedIds.includes(item.id) && item.bookDeleted);
    if (hasDeletedSelected) {
      alert('판매가 중단된 도서가 포함되어 있어 주문할 수 없습니다. 장바구니에서 삭제해주세요.');
      return;
    }
    router.push({ pathname: '/order/checkout', query: { cartItemIds: selectedIds.join(',') } });
  };

  if (!user) return <div className="cart-wrap" style={{ textAlign: 'center', color: '#999' }}>로그인 확인 중...</div>;

  return (
    <div className="cart-wrap">
      <div className="cart-header">
        <h2>🛒 장바구니</h2>
      </div>

      {loading && <p style={{ textAlign: 'center' }}>불러오는 중...</p>}
      {error && <p style={{ color: 'red', textAlign: 'center' }}>{error}</p>}

      {!loading && items.length === 0 ? (
        <div className="cart-empty">
          <div className="cart-empty-icon">🛒</div>
          장바구니가 비어있습니다.
          <div style={{ marginTop: 16 }}>
            <a
              className="btn btn-primary-bs"
              onClick={(e) => { e.preventDefault(); router.push('/books'); }}
              href="/books"
            >
              도서 둘러보기
            </a>
          </div>
        </div>
      ) : (
        <>
          <table className="cart-table">
            <thead>
              <tr>
                <th style={{ width: 40 }}>
                  <input
                    type="checkbox"
                    checked={selectedIds.length === items.length && items.length > 0}
                    onChange={(e) => (e.target.checked ? dispatch(selectAllItems()) : dispatch(clearSelection()))}
                  />
                </th>
                <th style={{ textAlign: 'left' }}>도서</th>
                <th>수량</th>
                <th>금액</th>
                <th>삭제</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id} style={item.bookDeleted ? { opacity: 0.6 } : undefined}>
                  <td>
                    <input
                      type="checkbox"
                      checked={selectedIds.includes(item.id)}
                      disabled={item.bookDeleted}
                      onChange={() => dispatch(toggleSelectItem(item.id))}
                    />
                  </td>
                  <td>
                    <div className="cart-item-info">
                      <div className="cart-item-cover">
                        <BookCoverImage
                          src={item.bookCover
                            ? (item.bookCover.startsWith('http') ? item.bookCover : `http://localhost:8080/${item.bookCover}`)
                            : null}
                          alt={item.bookTitle}
                          iconSize={20}
                        />
                      </div>
                      <div>
                        <div className="cart-item-title">{item.bookTitle}</div>
                        {item.bookDeleted && (
                          <div className="cart-item-stock-warn">
                            ⛔ 판매가 중단된 도서입니다. 삭제해주세요.
                          </div>
                        )}
                        {!item.bookDeleted && item.quantity > item.stockQuantity && (
                          <div className="cart-item-stock-warn">
                            ⚠ 재고 부족 (현재 재고: {item.stockQuantity}권)
                          </div>
                        )}
                      </div>
                    </div>
                  </td>
                  <td>
                    <div className="qty-control">
                      <button type="button" className="qty-btn" onClick={() => handleQuantityChange(item.id, item.quantity, -1, item.stockQuantity)}>−</button>
                      <span>{item.quantity}</span>
                      <button
                        type="button"
                        className="qty-btn"
                        disabled={item.bookDeleted}
                        onClick={() => handleQuantityChange(item.id, item.quantity, 1, item.stockQuantity)}
                      >
                        +
                      </button>
                    </div>
                  </td>
                  <td>{item.subtotal?.toLocaleString()}원</td>
                  <td>
                    <button type="button" className="qty-btn" onClick={() => handleRemove(item.id)}>✕</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="cart-summary">
            <div className="cart-summary-row">
              <span>선택한 도서 ({selectedItems.length}권)</span>
              <span>{selectedTotal.toLocaleString()}원</span>
            </div>
            <div className="cart-summary-total">
              <span>결제 예정 금액</span>
              <span>{selectedTotal.toLocaleString()}원</span>
            </div>
            <button type="button" className="kakaopay-btn" onClick={handleCheckout} disabled={selectedIds.length === 0}>
              선택한 도서 주문하기
            </button>
          </div>
        </>
      )}
    </div>
  );
}
