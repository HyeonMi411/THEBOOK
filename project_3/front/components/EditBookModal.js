// components/EditBookModal.js
// boot1(BookStore) 톤을 유지한 도서수정 모달 (antd Modal 틀 + bs-form-control 스타일)
import React, { useState, useEffect } from 'react';
import { Modal } from 'antd';

export default function EditBookModal({ visible, onCancel, editBook, onSubmit }) {
  const [form, setForm] = useState({});
  const [coverFile, setCoverFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);

  useEffect(() => {
    if (editBook) {
      setForm({
        title: editBook.title || "",
        author: editBook.author || "",
        publisher: editBook.publisher || "",
        publishDate: editBook.publishDate || "",
        category: editBook.category || "",
        ranking: editBook.ranking || "",
        price: editBook.price ?? "",
        pages: editBook.pages ?? "",
        description: editBook.description || "",
      });
      setCoverFile(null);
      setPreviewUrl(null);
    }
  }, [editBook, visible]);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  const onCoverChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setCoverFile(file);
    setPreviewUrl(URL.createObjectURL(file));
  };

  const handleOk = () => {
    onSubmit({
      ...form,
      price: form.price !== "" ? Number(form.price) : undefined,
      pages: form.pages !== "" ? Number(form.pages) : undefined,
    }, coverFile);
  };

  return (
    <Modal title="📚 도서 수정" open={visible} onCancel={onCancel} onOk={handleOk} okText="수정완료" cancelText="취소" destroyOnClose width={640}>
      <div style={{ display: "flex", gap: 20, flexWrap: "wrap" }}>
        <div style={{ flex: "2 1 320px", display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
          <div style={{ gridColumn: "span 2" }}>
            <label className="bs-form-label">도서명</label>
            <input name="title" className="bs-form-control" value={form.title || ""} onChange={onChange} />
          </div>
          <div>
            <label className="bs-form-label">저자</label>
            <input name="author" className="bs-form-control" value={form.author || ""} onChange={onChange} />
          </div>
          <div>
            <label className="bs-form-label">출판사</label>
            <input name="publisher" className="bs-form-control" value={form.publisher || ""} onChange={onChange} />
          </div>
          <div>
            <label className="bs-form-label">출간일</label>
            <input type="date" name="publishDate" className="bs-form-control" value={form.publishDate || ""} onChange={onChange} />
          </div>
          <div>
            <label className="bs-form-label">카테고리</label>
            <input name="category" className="bs-form-control" value={form.category || ""} onChange={onChange} />
          </div>
          <div>
            <label className="bs-form-label">랭킹</label>
            <input name="ranking" className="bs-form-control" value={form.ranking || ""} onChange={onChange} />
          </div>
          <div>
            <label className="bs-form-label">페이지수</label>
            <input type="number" name="pages" className="bs-form-control" value={form.pages ?? ""} onChange={onChange} />
          </div>
          <div style={{ gridColumn: "span 2" }}>
            <label className="bs-form-label">가격</label>
            <input type="number" name="price" className="bs-form-control" value={form.price ?? ""} onChange={onChange} />
          </div>
          <div style={{ gridColumn: "span 2" }}>
            <label className="bs-form-label">도서설명</label>
            <textarea name="description" className="bs-form-control" value={form.description || ""} onChange={onChange} />
          </div>
        </div>

        <div style={{ flex: "1 1 140px" }}>
          <div className="preview-box" style={{ marginBottom: 10, textAlign: "center" }}>
            {previewUrl
              ? <img src={previewUrl} alt="미리보기" style={{ width: 130, height: 175, objectFit: "cover", borderRadius: 8 }} />
              : (editBook?.bookCover
                ? <img
                    src={editBook.bookCover.startsWith('http') ? editBook.bookCover : `http://localhost:8080/${editBook.bookCover}`}
                    alt="현재 표지"
                    style={{ width: 130, height: 175, objectFit: "cover", borderRadius: 8 }}
                  />
                : <div style={{ width: 130, height: 175, background: "#eef1f5", borderRadius: 8, margin: "0 auto" }} />)}
          </div>
          <label className="bs-form-label">표지이미지 (변경시에만)</label>
          <input type="file" accept="image/*" onChange={onCoverChange} />
        </div>
      </div>
    </Modal>
  );
}
