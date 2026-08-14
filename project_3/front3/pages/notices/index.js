// pages/notices/index.js  (SBOARD2 - 공지사항)
import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from "react-redux";
import {
  fetchNoticesRequest, updateNoticeRequest, deleteNoticeRequest, resetNoticeState
} from "../../reducers/noticeReducer";
import { Spin, message } from 'antd';
import NoticeList from '../../components/NoticeList';
import EditNoticeModal from '../../components/EditNoticeModal';

export default function NoticesPage() {
  const dispatch = useDispatch();
  const { user } = useSelector((state) => state.auth);
  const { notices, loading, error } = useSelector((state) => state.notice);

  const isAdmin = user?.role === "ROLE_ADMIN"; // ★공지사항 글쓰기/수정/삭제는 관리자만

  const [isEditModalVisible, setIsEditModalVisible] = useState(false);
  const [uploadFile, setUploadFile] = useState(null);
  const [editNotice, setEditNotice] = useState(null);

  useEffect(() => {
    dispatch(fetchNoticesRequest());
  }, [dispatch]);

  const handleEdit = (notice) => {
    setEditNotice(notice);
    setIsEditModalVisible(true);
    setUploadFile(null);
  };

  const handleEditSubmit = (values) => {
    dispatch(updateNoticeRequest({
      noticeId: editNotice.id,
      dto: { btitle: values.btitle, bcontent: values.bcontent },
      file: uploadFile,
    }));
    setIsEditModalVisible(false);
    setEditNotice(null);
    message.success("공지사항이 수정되었습니다.");
  };

  const handleDelete = (noticeId) => {
    dispatch(deleteNoticeRequest(noticeId));
    message.success("공지사항이 삭제되었습니다.");
  };

  useEffect(() => {
    return () => { dispatch(resetNoticeState()); };
  }, [dispatch]);

  return (
    <div>
      {loading && <Spin />}
      {error && <p style={{ color: "red" }}>{error}</p>}

      <NoticeList
        notices={notices}
        isAdmin={isAdmin}
        handleEdit={handleEdit}
        handleDelete={handleDelete}
      />

      <EditNoticeModal
        visible={isEditModalVisible}
        onCancel={() => setIsEditModalVisible(false)}
        editNotice={editNotice}
        onSubmit={handleEditSubmit}
        uploadFile={uploadFile}
        setUploadFile={setUploadFile}
      />
    </div>
  );
}
