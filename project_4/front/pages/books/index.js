// pages/books/index.js
import React, { useEffect } from 'react';
import { useRouter } from 'next/router';
import { useSelector, useDispatch } from "react-redux";
import { fetchBooksRequest, searchBooksRequest, resetBookState } from "../../reducers/bookReducer";
import BookList from '../../components/BookList';
import BestsellerRanking from '../../components/BestsellerRanking';

export default function BooksPage() {
  const router = useRouter();
  const dispatch = useDispatch();
  const { books, loading, error, currentPage, totalPages, totalElements } = useSelector((state) => state.book);
  // URL을 직접 수정해서 들어오는 경우(?keyword= 뒤에 공백 등)까지 대비해 여기서도 정리.
  const keyword = typeof router.query.keyword === 'string' ? router.query.keyword.trim() : router.query.keyword;

  // 페이지 진입/쿼리변경시: ?keyword= 있으면 검색키워드 관련 목록조회, 없으면 ?page=(기본1) 로 페이징 조회 (12개씩)
  useEffect(() => {
    if (!router.isReady) return;
    const { page, category } = router.query;
    if (keyword) {
      dispatch(searchBooksRequest(keyword));
    } else {
      dispatch(fetchBooksRequest({ page: Number(page) || 1, size: 12, category }));
    }
  }, [dispatch, router.isReady, keyword, router.query.page, router.query.category]);

  const handlePageChange = (page) => {
    const query = { ...router.query, page };
    router.push({ pathname: '/books', query }, undefined, { scroll: true });
  };

  useEffect(() => {
    return () => { dispatch(resetBookState()); };
  }, [dispatch]);

  return (
    <div>
      {/* 검색 중이 아닐 때만(전체 목록 화면에서만) 베스트셀러 랭킹을 보여줍니다 */}
      {!keyword && <BestsellerRanking />}

      {/* 검색키워드 관련 도서 목록 화면 - AJAX 검색(BookSearchBox)에서 이 화면으로 이동합니다 */}
      {keyword && (
        <div className="search-result-header">
          <div className="search-result-title">
            🔍 &ldquo;<strong>{keyword}</strong>&rdquo; 검색결과
            <span className="search-result-count">{books.length}건</span>
          </div>
          <a
            className="search-result-back"
            onClick={(e) => { e.preventDefault(); router.push('/books'); }}
            href="/books"
          >
            전체 도서 목록으로
          </a>
        </div>
      )}

      {loading && <p style={{ textAlign: "center" }}>로딩중...</p>}
      {error && <p style={{ color: "red", textAlign: "center" }}>{error}</p>}

      {keyword && !loading && books.length === 0 ? (
        <div className="search-result-empty">
          <div style={{ fontSize: 44, marginBottom: 12 }}>🔎</div>
          &ldquo;{keyword}&rdquo;에 대한 검색결과가 없습니다.<br />
          다른 검색어로 다시 시도해보세요.
        </div>
      ) : (
        <BookList
          books={books}
          currentPage={currentPage}
          totalPages={keyword ? 1 : totalPages} // 검색결과는 페이징 없이 전체표시
          onPageChange={handlePageChange}
        />
      )}
    </div>
  );
}
