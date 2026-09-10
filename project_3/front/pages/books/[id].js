// pages/books/[id].js
// boot1(the703) templates/book/detail.html 디자인을 그대로 재현했음.
import React, { useEffect, useState } from "react";
import { useRouter } from "next/router";
import { useSelector, useDispatch } from "react-redux";
import { fetchBookDetailRequest, deleteBookRequest, updateBookRequest, resetBookState } from "../../reducers/bookReducer";
import { addToCartRequest } from "../../reducers/cartReducer";
import { createOrderRequest, resetOrderState } from "../../reducers/orderReducer";
import EditBookModal from "../../components/EditBookModal";
import BookCoverImage from "../../components/BookCoverImage";

export default function BookDetailPage() {
  const router = useRouter();
  const { id } = router.query;
  const dispatch = useDispatch();

  const { currentBook, loading, error } = useSelector((state) => state.book);
  const { user } = useSelector((state) => state.auth);
  const { currentOrder } = useSelector((state) => state.order);
  const cart = useSelector((state) => state.cart);
  const isAdmin = user?.role === "ROLE_ADMIN";

  const [isEditModalVisible, setIsEditModalVisible] = useState(false);
  const [buyQuantity, setBuyQuantity] = useState(1);
  const [addingToCart, setAddingToCart] = useState(false); // 담기 요청이 실제로 끝날 때까지 추적

  useEffect(() => {
    if (id) {
      dispatch(fetchBookDetailRequest(id));
    }
    return () => { dispatch(resetBookState()); };
  }, [id, dispatch]);

  // 다른 도서로 이동했을 때, 이전 도서에서 선택했던 수량이 새 도서의 재고보다
  // 크게 남아있지 않도록 1로 리셋
  useEffect(() => {
    setBuyQuantity(1);
  }, [currentBook?.id]);

  // 바로구매: 주문 생성 성공하면 결제확인 화면으로 이동
  useEffect(() => {
    if (currentOrder) {
      router.push(`/order/checkout?orderId=${currentOrder.id}`);
      dispatch(resetOrderState());
    }
  }, [currentOrder, router, dispatch]);

  const handleDelete = () => {
    if (window.confirm('정말 삭제하시겠습니까?')) {
      dispatch(deleteBookRequest(currentBook.id));
      router.push('/books');
    }
  };

  // 수정모달을 상세페이지 안에서 바로 열고 닫음. (페이징 도입 이후, 목록 페이지의
  // 현재 화면(12개)에 수정대상이 없을 수도 있어 ?edit=id 로 목록에 되돌아가 찾는 방식은
  // 더 이상 사용하지 않음.)
  const handleEditSubmit = (values, coverFile) => {
    dispatch(updateBookRequest({
      bookId: currentBook.id,
      dto: {
        title: values.title,
        author: values.author,
        publisher: values.publisher,
        publishDate: values.publishDate,
        category: values.category,
        ranking: values.ranking,
        pages: values.pages,
        price: values.price,
        description: values.description,
      },
      cover: coverFile,
    }));
    setIsEditModalVisible(false);
  };

  // 장바구니 담기 (로그인 필요) - dispatch 만 하고 alert 은 실제 결과(성공/실패)가
  // 온 뒤(아래 useEffect)에 띄움. 이전에는 dispatch 직후 곧바로 "담았습니다"를 띄워서,
  // 재고부족/인증만료 등으로 실제로는 실패해도 항상 성공 메시지가 뜨는 문제가 있었음.
  const handleAddToCart = () => {
    if (!user) { router.push('/login'); return; }
    setAddingToCart(true);
    dispatch(addToCartRequest({ bookId: currentBook.id, quantity: buyQuantity }));
  };

  // 담기 요청이 실제로 끝난 시점(loading: true → false)에만 결과를 알림
  useEffect(() => {
    if (!addingToCart || cart.loading) return;
    if (cart.error) {
      alert(`장바구니 담기에 실패했습니다: ${cart.error}`);
    } else {
      alert('장바구니에 담았습니다.');
    }
    setAddingToCart(false);
  }, [addingToCart, cart.loading, cart.error]);

  // 바로구매 (로그인 필요) - 주문 생성 후 결제확인 화면으로 이동
  const handleBuyNow = () => {
    if (!user) { router.push('/login'); return; }
    dispatch(createOrderRequest({ bookId: currentBook.id, quantity: buyQuantity }));
  };

  if (loading || !currentBook) return <div className="detail-container">로딩중...</div>;
  if (error) return <div className="detail-container" style={{ color: "red" }}>{error}</div>;

  const coverSrc = currentBook.bookCover
    ? (currentBook.bookCover.startsWith('http')
        ? currentBook.bookCover
        : `http://localhost:8080/${currentBook.bookCover}`)
    : null;

  return (
    <div className="detail-container">
      <div className="detail-card">
        <div className="row-flex">
          {/* 이미지 */}
          <div className="detail-cover-col">
            {/* 표지 없음/링크깨짐 → 자동으로 기본 아이콘 표시 */}
            <BookCoverImage src={coverSrc} alt={currentBook.title} height={460} iconSize={60} style={{ borderRadius: 12 }} />
          </div>

          {/* 정보 */}
          <div className="detail-info-col">
            <h2 className="info-title">{currentBook.title}</h2>
            <div className="info-meta">{currentBook.author} · {currentBook.publisher}</div>
            <span className="badge-category">{currentBook.category}</span>
            <span className={`stock-badge ${currentBook.stockQuantity > 0 ? 'in-stock' : 'out-of-stock'}`}>
              {currentBook.stockQuantity > 0 ? `재고 ${currentBook.stockQuantity}권` : '품절'}
            </span>

            {currentBook.rating != null && (
              <div className="rating-line">
                ⭐ {currentBook.rating}
                {currentBook.reviewCount != null && (
                  <small style={{ color: "#888", fontWeight: 400, marginLeft: 8 }}>
                    ({currentBook.reviewCount} Reviews)
                  </small>
                )}
              </div>
            )}

            <table className="info-table">
              <tbody>
                <tr><th>출판일</th><td>{currentBook.publishDate || "출판일 미상"}</td></tr>
                <tr><th>페이지</th><td>{currentBook.pages != null ? `${currentBook.pages} Page` : "-"}</td></tr>
                <tr>
                  <th>가격</th>
                  <td>
                    <strong style={{ color: "#2563eb" }}>
                      {currentBook.price != null ? `${currentBook.price.toLocaleString()} 원` : "-"}
                    </strong>
                  </td>
                </tr>
                <tr><th>랭킹</th><td>{currentBook.ranking || "-"}</td></tr>
                <tr><th>등록자</th><td>{currentBook.userNickname}</td></tr>
              </tbody>
            </table>

            <div className="description-box">{currentBook.description}</div>

            {/* 장바구니 담기 / 바로구매 - 로그인한 회원이면 누구나 가능 */}
            <div style={{ marginTop: 20 }}>
              {/* 예전에는 드롭다운(최대 10권 고정)이라 재고가 10권을 넘어도 그 이상은
                  선택할 수 없었음. 재고 전체 범위를 직접 입력할 수 있도록 숫자 입력으로 변경 */}
              <input
                type="number"
                className="qty-select"
                value={buyQuantity}
                min={1}
                max={currentBook.stockQuantity}
                onChange={(e) => {
                  const raw = Number(e.target.value);
                  if (Number.isNaN(raw)) { setBuyQuantity(1); return; }
                  // 재고 범위(1 ~ stockQuantity) 밖으로 못 나가게 즉시 보정
                  const clamped = Math.min(Math.max(raw, 1), currentBook.stockQuantity);
                  setBuyQuantity(clamped);
                }}
                disabled={currentBook.stockQuantity === 0}
                style={{ width: 90 }}
              />
              <span style={{ marginLeft: 6, color: "#888" }}>권 (최대 {currentBook.stockQuantity}권)</span>
              <div className="buy-btn-area">
                <button type="button" className="btn-cart" onClick={handleAddToCart} disabled={currentBook.stockQuantity === 0}>
                  🛒 장바구니 담기
                </button>
                <button type="button" className="btn-buy-now" onClick={handleBuyNow} disabled={currentBook.stockQuantity === 0}>
                  ⚡ 바로구매
                </button>
              </div>
            </div>

            <div className="btn-area">
              <a className="btn btn-outline" onClick={(e) => { e.preventDefault(); router.push('/books'); }} href="/books">
                목록
              </a>

              {/* 수정/삭제는 관리자 전용 */}
              {isAdmin && (
                <>
                  <button type="button" className="btn btn-primary-bs" onClick={() => setIsEditModalVisible(true)}>
                    수정
                  </button>
                  <button type="button" className="btn btn-danger-bs" onClick={handleDelete}>
                    삭제
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      <EditBookModal
        visible={isEditModalVisible}
        onCancel={() => setIsEditModalVisible(false)}
        editBook={currentBook}
        onSubmit={handleEditSubmit}
      />
    </div>
  );
}
