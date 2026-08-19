// pages/books/[id].js
// boot1(the703) templates/book/detail.html 디자인을 그대로 재현했습니다.
import React, { useEffect, useState } from "react";
import { useRouter } from "next/router";
import { useSelector, useDispatch } from "react-redux";
import { fetchBookDetailRequest, deleteBookRequest, updateBookRequest, resetBookState } from "../../reducers/bookReducer";
import EditBookModal from "../../components/EditBookModal";
import BookCoverImage from "../../components/BookCoverImage";

export default function BookDetailPage() {
  const router = useRouter();
  const { id } = router.query;
  const dispatch = useDispatch();

  const { currentBook, loading, error } = useSelector((state) => state.book);
  const { user } = useSelector((state) => state.auth);
  const isAdmin = user?.role === "ROLE_ADMIN";

  const [isEditModalVisible, setIsEditModalVisible] = useState(false);

  useEffect(() => {
    if (id) {
      dispatch(fetchBookDetailRequest(id));
    }
    return () => { dispatch(resetBookState()); };
  }, [id, dispatch]);

  const handleDelete = () => {
    if (window.confirm('정말 삭제하시겠습니까?')) {
      dispatch(deleteBookRequest(currentBook.id));
      router.push('/books');
    }
  };

  // ★수정모달을 상세페이지 안에서 바로 열고 닫습니다. (페이징 도입 이후, 목록 페이지의
  //   현재 화면(12개)에 수정대상이 없을 수도 있어 ?edit=id 로 목록에 되돌아가 찾는 방식은
  //   더 이상 사용하지 않습니다.)
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
            {/* ★표지 없음/링크깨짐 → 자동으로 기본 아이콘 표시 */}
            <BookCoverImage src={coverSrc} alt={currentBook.title} height={460} iconSize={60} style={{ borderRadius: 12 }} />
          </div>

          {/* 정보 */}
          <div className="detail-info-col">
            <h2 className="info-title">{currentBook.title}</h2>
            <div className="info-meta">{currentBook.author} · {currentBook.publisher}</div>
            <span className="badge-category">{currentBook.category}</span>

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
                <tr><th>출판일</th><td>{currentBook.publishDate}</td></tr>
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

            <div className="btn-area">
              <a className="btn btn-outline" onClick={(e) => { e.preventDefault(); router.push('/books'); }} href="/books">
                목록
              </a>

              {/* ★수정/삭제는 관리자 전용 */}
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
