// components/BookSearchBox.js
// boot1(the703) fragments/header.html 의 AJAX 실시간 검색(keyup 이벤트 → /book/search)을
// React 버전(/api/books/search)으로 재현했습니다.
// ※ boot1 원본은 드롭다운 항목을 클릭하면 그 책 1권의 상세페이지로 바로 이동했지만,
//   검색키워드와 관련된 전체 목록을 보고 싶다는 요청에 따라 드롭다운 항목/검색버튼/Enter
//   모두 "검색키워드 관련 도서 목록(/books?keyword=)" 화면으로 이동하도록 변경했습니다.
import React, { useState, useRef } from 'react';
import { useRouter } from 'next/router';
import api from '../api/axios';

export default function BookSearchBox() {
  const router = useRouter();
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState([]);
  const [showResult, setShowResult] = useState(false);
  const debounceRef = useRef(null);

  const handleChange = (e) => {
    const value = e.target.value;
    setKeyword(value);

    if (debounceRef.current) clearTimeout(debounceRef.current);

    if (value.trim() === '') {
      setResults([]);
      setShowResult(false);
      return;
    }

    // boot1 원본처럼 입력할때마다(keyup) 바로 검색하되, 너무 잦은 요청은 살짝 디바운스
    debounceRef.current = setTimeout(async () => {
      try {
        const res = await api.get('/api/books/search', { params: { keyword: value } });
        setResults(res.data || []);
        setShowResult(true);
      } catch (err) {
        setResults([]);
        setShowResult(true);
      }
    }, 250);
  };

  // ★검색키워드 관련 도서 목록 화면으로 이동 (더 이상 특정 도서 1권의 상세로 바로 가지 않습니다)
  const goToKeywordList = (kw) => {
    const targetKeyword = (kw ?? keyword).trim();
    if (!targetKeyword) return;
    setShowResult(false);
    setKeyword('');
    router.push(`/books?keyword=${encodeURIComponent(targetKeyword)}`);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    goToKeywordList();
  };

  return (
    <div className="bs-search-box">
      <form className="bs-search-form" onSubmit={handleSubmit}>
        <input
          type="text"
          value={keyword}
          onChange={handleChange}
          onFocus={() => { if (results.length > 0) setShowResult(true); }}
          onBlur={() => setTimeout(() => setShowResult(false), 150)} // 클릭 이벤트 먼저 처리되도록 지연
          placeholder="도서명 · 작가 · 출판사 검색"
        />
        <button type="submit" className="bs-search-btn">🔍</button>

        {showResult && (
          <div className="bs-search-result-box">
            {results.length === 0 ? (
              <div className="bs-search-item">검색 결과 없음</div>
            ) : (
              <>
                {results.map((book) => (
                  <div
                    key={book.id}
                    className="bs-search-item"
                    onMouseDown={() => goToKeywordList()} // onBlur보다 먼저 실행되도록 mousedown 사용
                  >
                    <strong>{book.title}</strong>
                    <br />
                    <small>{book.author} · {book.publisher}</small>
                  </div>
                ))}
                {/* 전체 검색결과 목록 보기 (미리보기 아래 항상 노출) */}
                <div
                  className="bs-search-item bs-search-viewall"
                  onMouseDown={() => goToKeywordList()}
                >
                  🔍 &ldquo;{keyword}&rdquo; 검색결과 {results.length}건 전체보기
                </div>
              </>
            )}
          </div>
        )}
      </form>
    </div>
  );
}
