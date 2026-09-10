// pages/notices/new.js  ( 공지사항 글쓰기는 관리자(ROLE_ADMIN)만 가능 )
// boot1(the703) templates/board/write.html 디자인을 그대로 재현했음.
import React, { useState, useEffect } from "react";
import { useSelector, useDispatch } from "react-redux";
import { useRouter } from "next/router";
import { createNoticeRequest, resetNoticeState } from "../../reducers/noticeReducer";
import AccessDenied from "../../components/AccessDenied";

export default function NewNoticePage() {
  const router = useRouter();
  const dispatch = useDispatch();

  const { loading, error, success } = useSelector((state) => state.notice);
  const { user } = useSelector((state) => state.auth);
  const isAdmin = user?.role === "ROLE_ADMIN";

  const [form, setForm] = useState({ btitle: "", bcontent: "" });
  const [file, setFile] = useState(null);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = (e) => {
    e.preventDefault();
    if (!form.btitle || !form.bcontent) {
      alert("제목과 내용을 입력하세요.");
      return;
    }
    dispatch(createNoticeRequest({ dto: form, file }));
  };

  useEffect(() => {
    if (success) {
      alert("공지사항이 성공적으로 작성되었습니다.");
      dispatch(resetNoticeState());
      router.push("/notices");
    }
  }, [success, router, dispatch]);

  if (!user) {
    return <AccessDenied needLogin message="공지사항 작성은 관리자만 가능합니다. 먼저 로그인해주세요." backHref="/notices" />;
  }
  if (!isAdmin) {
    return <AccessDenied message="공지사항 작성은 관리자(ROLE_ADMIN)만 가능합니다." backHref="/notices" />;
  }

  return (
    <div className="write-wrap">
      <div className="write-card">
        <div className="write-header">
          <h2>📝 공지사항 작성</h2>
          <p>BookStore 이용자들에게 전달할 공지사항을 작성합니다.</p>
        </div>

        <div className="write-body">
          <form onSubmit={onSubmit}>
            <div style={{ marginBottom: 18 }}>
              <label className="bs-form-label">제목</label>
              <input name="btitle" className="bs-form-control" value={form.btitle} onChange={onChange} required />
            </div>

            <div style={{ marginBottom: 18 }}>
              <label className="bs-form-label">내용</label>
              <textarea name="bcontent" className="bs-form-control" style={{ minHeight: 220 }} value={form.bcontent} onChange={onChange} required />
            </div>

            <div className="upload-box" style={{ marginBottom: 8 }}>
              <label className="bs-form-label">첨부파일 (선택)</label>
              <input type="file" accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.hwp,.txt,.zip" onChange={(e) => setFile(e.target.files[0] || null)} />
            </div>

            <div className="button-area">
              <button type="submit" className="btn btn-primary-bs" disabled={loading}>
                {loading ? "작성중..." : "공지사항 작성"}
              </button>
              <a className="btn btn-outline" onClick={(e) => { e.preventDefault(); router.push('/notices'); }} href="/notices">
                취소
              </a>
            </div>
            {error && <p style={{ color: "red", marginTop: 12 }}>{error}</p>}
          </form>
        </div>
      </div>
    </div>
  );
}
