// pages/notices/index.js  (SBOARD2 - 공지사항)
import React, { useEffect } from 'react';
import { useRouter } from 'next/router';
import { useSelector, useDispatch } from "react-redux";
import { fetchNoticesRequest, resetNoticeState } from "../../reducers/noticeReducer";
import NoticeList from '../../components/NoticeList';

export default function NoticesPage() {
  const router = useRouter();
  const dispatch = useDispatch();
  const {
    notices, loading, error, currentPage, totalPages, totalElements, pageSize
  } = useSelector((state) => state.notice);

  useEffect(() => {
    if (!router.isReady) return;
    const { page } = router.query;
    dispatch(fetchNoticesRequest({ page: Number(page) || 1, size: 12 }));
  }, [dispatch, router.isReady, router.query.page]);

  const handlePageChange = (page) => {
    router.push({ pathname: '/notices', query: { page } }, undefined, { scroll: true });
  };

  useEffect(() => {
    return () => { dispatch(resetNoticeState()); };
  }, [dispatch]);

  return (
    <div>
      {loading && <p style={{ textAlign: "center" }}>로딩중...</p>}
      {error && <p style={{ color: "red", textAlign: "center" }}>{error}</p>}
      <NoticeList
        notices={notices}
        currentPage={currentPage}
        totalPages={totalPages}
        totalElements={totalElements}
        pageSize={pageSize}
        onPageChange={handlePageChange}
      />
    </div>
  );
}
