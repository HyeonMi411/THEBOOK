// components/BookList.js
// boot1(the703) templates/book/list.html 의 book-grid / book-card 디자인을 그대로 재현했습니다.
import React from 'react';
import { useRouter } from 'next/router';
import { useSelector } from 'react-redux';
import Pagination from './Pagination';

export default function BookList({ books = [], currentPage, totalPages, onPageChange }) {
  const router = useRouter();
  const { user } = useSelector((state) => state.auth);
  const isAdmin = user?.role === "ROLE_ADMIN";

  return (
    <div className="book-wrap">
      <div className="page-header">
        <div className="page-title">
          📚 <span>BookStore</span>
        </div>
        {isAdmin && (
          <a
            className="btn-write"
            onClick={(e) => { e.preventDefault(); router.push('/books/new'); }}
            href="/books/new"
          >
            + 도서 등록
          </a>
        )}
      </div>

      {books.length === 0 ? (
        <div className="notice-empty">등록된 도서가 없습니다.</div>
      ) : (
        <div className="book-grid">
          {books.map((book) => (
            <div
              key={book.id}
              className="book-card"
              onClick={() => router.push(`/books/${book.id}`)}
            >
              <div className="book-cover">
                {book.bookCover ? (
                  <img
                    src={
                      book.bookCover.startsWith('http')
                        ? book.bookCover
                        : `http://localhost:8080/${book.bookCover}`
                    }
                    alt={book.title}
                  />
                ) : (
                  <div style={{
                    width: "100%", height: "100%", display: "flex",
                    alignItems: "center", justifyContent: "center", color: "#bbb", fontSize: 40,
                  }}>📕</div>
                )}
              </div>

              <div className="book-body">
                <div className="book-title">{book.title}</div>
                <div className="book-author">{book.author} · {book.publisher}</div>
                <div className="book-category">{book.category}</div>

                <div className="book-rating">
                  {book.rating != null ? (
                    <span className="book-star">⭐ {book.rating}</span>
                  ) : <span />}
                  {book.price != null && (
                    <span className="book-price">{book.price.toLocaleString()}원</span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ★페이징 - boot1 book/list.html 과 동일하게 하단에 페이지번호 노출 */}
      <Pagination currentPage={currentPage} totalPages={totalPages} onChange={onPageChange} />
    </div>
  );
}
