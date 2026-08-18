// components/Pagination.js
// boot1(the703) util/UtilPaging.java 의 페이지블록 계산 로직을 React 버전으로 그대로 재현했습니다.
// - bottomList(기본 10) 개씩 페이지번호를 묶어서 보여주고, 블록단위로 "이전"/"다음" 이동
import React from 'react';

export default function Pagination({ currentPage, totalPages, onChange, bottomList = 10 }) {
  if (!totalPages || totalPages <= 1) return null;

  // boot1 UtilPaging 과 동일한 계산식
  const start = Math.floor((currentPage - 1) / bottomList) * bottomList + 1;
  const end = Math.min(start + bottomList - 1, totalPages);

  const pageNumbers = [];
  for (let i = start; i <= end; i += 1) pageNumbers.push(i);

  return (
    <ul className="bs-pagination">
      {start > 1 && (
        <li>
          <a className="bs-page-link" onClick={() => onChange(start - 1)}>이전</a>
        </li>
      )}

      {pageNumbers.map((num) => (
        <li key={num}>
          <a
            className={`bs-page-link${num === currentPage ? ' active' : ''}`}
            onClick={() => onChange(num)}
          >
            {num}
          </a>
        </li>
      ))}

      {end < totalPages && (
        <li>
          <a className="bs-page-link" onClick={() => onChange(end + 1)}>다음</a>
        </li>
      )}
    </ul>
  );
}
