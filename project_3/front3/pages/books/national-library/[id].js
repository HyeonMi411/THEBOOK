// pages/books/national-library/[id].js
// 국립중앙도서관 검색결과 상세화면. 목록에서 클릭한 도서 정보를 Redux(nlSelectedBook)로 그대로
// 받아서 보여주며, 관리자는 "BookStore에 저장" 버튼으로 실제 DB에 저장할 수 있습니다.
// (국립중앙도서관 API 특성상 id 하나로 다시 조회하는 API가 없어서, 목록에서 선택한 데이터를
// 그대로 들고 이동하는 방식입니다. 새로고침 등으로 선택정보가 없으면 검색화면으로 안내합니다.)
import React, { useEffect } from 'react';
import { useRouter } from 'next/router';
import { useSelector, useDispatch } from 'react-redux';
import { nlSaveRequest, resetNlSaveState, clearNlSelectedBook } from '../../../reducers/bookReducer';
import BookCoverImage from '../../../components/BookCoverImage';

export default function NationalLibraryDetailPage() {
  const router = useRouter();
  const dispatch = useDispatch();
  const { nlSelectedBook, nlSaveLoading, nlSaveError, nlSaveSuccess } = useSelector((state) => state.book);
  const { user } = useSelector((state) => state.auth);
  const isAdmin = user?.role === 'ROLE_ADMIN';

  useEffect(() => {
    return () => { dispatch(resetNlSaveState()); };
  }, [dispatch]);

  const handleSave = () => {
    dispatch(nlSaveRequest(nlSelectedBook));
  };

  const handleGoToBookList = () => {
    dispatch(clearNlSelectedBook());
    router.push('/books');
  };

  if (!nlSelectedBook) {
    return (
      <div className="nl-detail-wrap">
        <div className="nl-detail-card" style={{ padding: 40, textAlign: 'center' }}>
          <p style={{ marginBottom: 16 }}>
            선택된 도서 정보가 없습니다. 국립중앙도서관 검색 화면에서 도서를 다시 선택해주세요.
          </p>
          <a
            className="btn btn-primary-bs"
            onClick={(e) => { e.preventDefault(); router.push('/books/national-library'); }}
            href="/books/national-library"
          >
            검색화면으로 이동
          </a>
        </div>
      </div>
    );
  }

  const book = nlSelectedBook;

  return (
    <div className="nl-detail-wrap">
      <div className="nl-detail-card">
        <div className="nl-detail-row">
          <div className="nl-detail-cover">
            {/* 표지 없음/링크깨짐 → 자동으로 기본 아이콘 표시 */}
            <BookCoverImage src={book.bookCover} alt={book.title_info} height={420} iconSize={54} style={{ borderRadius: 12 }} />
          </div>

          <div className="nl-detail-info">
            <div className="nl-detail-title">{book.title_info}</div>

            <table className="nl-detail-table">
              <tbody>
                <tr><th>저자</th><td>{book.author_info || '-'}</td></tr>
                <tr><th>출판사</th><td>{book.pub_info || '-'}</td></tr>
                <tr><th>발행년도</th><td>{book.pub_year_info || '-'}</td></tr>
                <tr><th>ISBN</th><td>{book.isbn || '-'}</td></tr>
                <tr><th>KDC 분류</th><td>{book.kdc_name_1s || '-'}</td></tr>
              </tbody>
            </table>

            <div className="btn-area">
              <a
                className="btn btn-outline"
                onClick={(e) => { e.preventDefault(); router.push('/books/national-library'); }}
                href="/books/national-library"
              >
                검색결과로
              </a>

              {/* BookStore DB 저장은 관리자만 가능 */}
              {isAdmin && !nlSaveSuccess && (
                <button type="button" className="btn btn-primary-bs" onClick={handleSave} disabled={nlSaveLoading}>
                  {nlSaveLoading ? '저장중...' : '📥 BookStore에 저장'}
                </button>
              )}
            </div>

            {nlSaveSuccess && (
              <div className="nl-save-result ok">
                ✅ BookStore에 저장되었습니다.{' '}
                <a onClick={(e) => { e.preventDefault(); handleGoToBookList(); }} href="/books" style={{ fontWeight: 700 }}>
                  도서 목록에서 확인하기 →
                </a>
              </div>
            )}
            {nlSaveError && (
              <div className="nl-save-result error">⚠️ {nlSaveError}</div>
            )}
            {!isAdmin && (
              <p style={{ marginTop: 16, color: '#999', fontSize: 13 }}>
                * BookStore에 저장하는 기능은 관리자만 이용할 수 있습니다.
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
