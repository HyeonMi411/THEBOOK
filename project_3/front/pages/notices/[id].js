// pages/notices/[id].js  (SBOARD2 - 공지사항, 상세조회시 서버에서 BHIT +1 처리됨)
// boot1(the703) templates/board/detail.html 디자인을 그대로 재현했음.
import React, { useEffect, useState } from "react";
import { useRouter } from "next/router";
import { useSelector, useDispatch } from "react-redux";
import {
  fetchNoticeDetailRequest, deleteNoticeRequest, updateNoticeRequest, resetNoticeState
} from "../../reducers/noticeReducer";
import EditNoticeModal from "../../components/EditNoticeModal";
import moment from "moment";

export default function NoticeDetailPage() {
  const router = useRouter();
  const { id } = router.query;
  const dispatch = useDispatch();

  const { currentNotice, loading, error } = useSelector((state) => state.notice);
  const { user } = useSelector((state) => state.auth);
  const isAdmin = user?.role === "ROLE_ADMIN";

  const [isEditModalVisible, setIsEditModalVisible] = useState(false);

  useEffect(() => {
    if (id) {
      dispatch(fetchNoticeDetailRequest(id)); // 상세조회 = 조회수 +1 (백엔드 Sboard2Service.getNotice)
    }
    return () => { dispatch(resetNoticeState()); };
  }, [id, dispatch]);

  const handleDelete = () => {
    if (window.confirm('정말 삭제하시겠습니까?')) {
      dispatch(deleteNoticeRequest(currentNotice.id));
      router.push('/notices');
    }
  };

  const handleEditSubmit = (values, file) => {
    dispatch(updateNoticeRequest({
      noticeId: currentNotice.id,
      dto: { btitle: values.btitle, bcontent: values.bcontent },
      file,
    }));
    setIsEditModalVisible(false);
  };

  if (loading || !currentNotice) return <div className="notice-detail-wrap">로딩중...</div>;
  if (error) return <div className="notice-detail-wrap" style={{ color: "red" }}>{error}</div>;

  return (
    <div className="notice-detail-wrap">
      <div className="notice-detail-card">
        <div className="notice-detail-header">
          <div className="notice-detail-title">{currentNotice.btitle}</div>
          <div className="notice-detail-info">
            <div>작성자 : {currentNotice.userNickname}</div>
            <div>
              작성일 : {currentNotice.createdAt ? moment(currentNotice.createdAt).format("YYYY-MM-DD HH:mm") : ""}
            </div>
            <div>조회수 : {currentNotice.bhit}</div>
          </div>
        </div>

        {currentNotice.bfile && (
          <div className="notice-detail-image">
            {/\.(jpg|jpeg|png|gif|webp)$/i.test(currentNotice.bfile) ? (
              <img src={`http://localhost:8080/${currentNotice.bfile}`} alt="첨부이미지" />
            ) : (
              <a href={`http://localhost:8080/${currentNotice.bfile}`} target="_blank" rel="noreferrer">
                📎 첨부파일 다운로드
              </a>
            )}
          </div>
        )}

        <div className="notice-detail-content">{currentNotice.bcontent}</div>

        <div className="notice-detail-footer">
          {/* 수정/삭제는 관리자 전용 */}
          {isAdmin && (
            <>
              <button type="button" className="btn btn-outline" onClick={() => setIsEditModalVisible(true)}>
                수정
              </button>
              <button type="button" className="btn btn-danger-bs" onClick={handleDelete}>
                삭제
              </button>
            </>
          )}
          <a
            className="btn btn-primary-bs"
            onClick={(e) => { e.preventDefault(); router.push('/notices'); }}
            href="/notices"
          >
            목록
          </a>
        </div>
      </div>

      <EditNoticeModal
        visible={isEditModalVisible}
        onCancel={() => setIsEditModalVisible(false)}
        editNotice={currentNotice}
        onSubmit={handleEditSubmit}
      />
    </div>
  );
}
