// pages/notices/[id].js  (SBOARD2 - 공지사항, 상세조회시 서버에서 BHIT +1 처리됨)
import React, { useEffect } from "react";
import { useRouter } from "next/router";
import { useSelector, useDispatch } from "react-redux";
import { Card, Spin, Tag, Divider } from "antd";
import { fetchNoticeDetailRequest, resetNoticeState } from "../../reducers/noticeReducer";
import moment from "moment";

export default function NoticeDetailPage() {
  const router = useRouter();
  const { id } = router.query;
  const dispatch = useDispatch();

  const { currentNotice, loading, error } = useSelector((state) => state.notice);

  useEffect(() => {
    if (id) {
      dispatch(fetchNoticeDetailRequest(id)); // 상세조회 = 조회수 +1 (백엔드 Sboard2Service.getNotice)
    }
    return () => { dispatch(resetNoticeState()); };
  }, [id, dispatch]);

  if (loading || !currentNotice) return <Spin />;
  if (error) return <p style={{ color: "red" }}>{error}</p>;

  return (
    <Card style={{ maxWidth: 800, margin: "0 auto" }}>
      <h2>{currentNotice.btitle}</h2>
      <div style={{ color: "#999", marginBottom: 12 }}>
        <span>작성자 : {currentNotice.userNickname}</span>
        <span style={{ marginLeft: 16 }}>
          작성일 : {currentNotice.createdAt ? moment(currentNotice.createdAt).format("YYYY-MM-DD HH:mm") : ""}
        </span>
        <Tag style={{ marginLeft: 16 }}>조회수 {currentNotice.bhit}</Tag>
      </div>

      <Divider />

      <p style={{ whiteSpace: "pre-wrap", minHeight: 120 }}>{currentNotice.bcontent}</p>

      {currentNotice.bfile && (
        <div style={{ marginTop: 24 }}>
          <a href={`http://localhost:8080/${currentNotice.bfile}`} target="_blank" rel="noreferrer">
            📎 첨부파일 다운로드
          </a>
        </div>
      )}
    </Card>
  );
}
