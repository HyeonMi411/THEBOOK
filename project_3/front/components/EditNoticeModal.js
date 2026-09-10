// components/EditNoticeModal.js  (SBOARD2 - 공지사항)
import React, { useState, useEffect } from 'react';
import { Modal } from 'antd';

export default function EditNoticeModal({ visible, onCancel, editNotice, onSubmit }) {
  const [form, setForm] = useState({ btitle: "", bcontent: "" });
  const [file, setFile] = useState(null);

  useEffect(() => {
    if (editNotice) {
      setForm({ btitle: editNotice.btitle || "", bcontent: editNotice.bcontent || "" });
      setFile(null);
    }
  }, [editNotice, visible]);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleOk = () => {
    if (!form.btitle || !form.bcontent) {
      alert("제목과 내용을 입력하세요.");
      return;
    }
    onSubmit(form, file);
  };

  return (
    <Modal title="📝 공지사항 수정" open={visible} onCancel={onCancel} onOk={handleOk} okText="수정완료" cancelText="취소" destroyOnClose>
      <div style={{ marginBottom: 14 }}>
        <label className="bs-form-label">제목</label>
        <input name="btitle" className="bs-form-control" value={form.btitle} onChange={onChange} />
      </div>
      <div style={{ marginBottom: 14 }}>
        <label className="bs-form-label">내용</label>
        <textarea name="bcontent" className="bs-form-control" style={{ minHeight: 160 }} value={form.bcontent} onChange={onChange} />
      </div>
      <div>
        <label className="bs-form-label">첨부파일 (변경시에만)</label>
        <input type="file" accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.hwp,.txt,.zip" onChange={(e) => setFile(e.target.files[0] || null)} />
      </div>
    </Modal>
  );
}
