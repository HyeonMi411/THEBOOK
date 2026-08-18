// components/NoticeList.js  (SBOARD2 - 공지사항)
// boot1(the703) templates/board/list.html 의 notice-table 디자인을 그대로 재현했습니다.
// (boot1 원본과 동일하게, 목록에는 "글쓰기" 버튼만 두고 수정/삭제는 상세페이지에서 처리합니다.)
import React from 'react';
import { useRouter } from 'next/router';
import { useSelector } from 'react-redux';
import moment from 'moment';
import Pagination from './Pagination';

export default function NoticeList({ notices = [], currentPage = 1, totalPages, totalElements = 0, pageSize = 12, onPageChange }) {
  const router = useRouter();
  const { user } = useSelector((state) => state.auth);
  const isAdmin = user?.role === "ROLE_ADMIN";

  return (
    <div className="notice-wrap">
      <div className="notice-header">
        <div className="notice-title">
          <h2>📢 공지사항</h2>
          <p>BookStore의 새로운 소식과 이벤트를 확인하세요.</p>
        </div>
        {isAdmin && (
          <a
            className="btn-write"
            onClick={(e) => { e.preventDefault(); router.push('/notices/new'); }}
            href="/notices/new"
          >
            + 글쓰기
          </a>
        )}
      </div>

      <div className="notice-card">
        <table className="notice-table">
          <thead>
            <tr>
              <th style={{ width: "8%" }}>번호</th>
              <th style={{ width: "54%" }}>제목</th>
              <th style={{ width: "12%" }}>작성자</th>
              <th style={{ width: "16%" }}>작성일</th>
              <th style={{ width: "10%" }}>조회수</th>
            </tr>
          </thead>
          <tbody>
            {notices.length === 0 ? (
              <tr>
                <td colSpan={5} className="notice-empty">
                  등록된 공지사항이 없습니다.
                </td>
              </tr>
            ) : (
              notices.map((notice, i) => (
                <tr key={notice.id}>
                  {/* boot1 원본과 동일한 전체 역순번호 계산: 전체개수-((현재페이지-1)*페이지크기)-순번 */}
                  <td>{totalElements - ((currentPage - 1) * pageSize) - i}</td>
                  <td className="notice-title-cell">
                    <a
                      className="notice-link"
                      onClick={(e) => { e.preventDefault(); router.push(`/notices/${notice.id}`); }}
                      href={`/notices/${notice.id}`}
                    >
                      {notice.btitle}
                    </a>
                  </td>
                  <td>{notice.userNickname}</td>
                  <td className="notice-date">
                    {notice.createdAt ? moment(notice.createdAt).format("YYYY-MM-DD HH:mm") : ""}
                  </td>
                  <td>{notice.bhit}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* ★페이징 - boot1 board/list.html 과 동일하게 하단에 페이지번호 노출 */}
      <Pagination currentPage={currentPage} totalPages={totalPages} onChange={onPageChange} />
    </div>
  );
}
