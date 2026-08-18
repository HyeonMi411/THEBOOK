// pages/books/new.js  ( ★도서등록은 관리자(ROLE_ADMIN)만 가능 )
// boot1(the703) templates/book/write.html 의 write-wrap/write-card 레이아웃(입력폼+표지미리보기)을 재현했습니다.
import React, { useState, useEffect } from "react";
import { useSelector, useDispatch } from "react-redux";
import { useRouter } from "next/router";
import { createBookRequest, resetBookState, kakaoInsertRequest, resetKakaoInsertState } from "../../reducers/bookReducer";
import AccessDenied from "../../components/AccessDenied";

export default function NewBookPage() {
  const router = useRouter();
  const dispatch = useDispatch();

  const { loading, error, success, kakaoLoading, kakaoError, kakaoInsertedCount } = useSelector((state) => state.book);
  const { user } = useSelector((state) => state.auth);
  const isAdmin = user?.role === "ROLE_ADMIN";

  const [kakaoSearch, setKakaoSearch] = useState("");

  const [form, setForm] = useState({
    title: "", author: "", publisher: "", publishDate: "", category: "",
    price: "", pages: "", ranking: "", rating: "", reviewCount: "", description: "",
  });
  const [coverFile, setCoverFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onCoverChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setCoverFile(file);
    setPreviewUrl(URL.createObjectURL(file));
  };

  const onSubmit = (e) => {
    e.preventDefault();
    if (!form.title || !form.author || !form.publisher || !form.publishDate || !form.category) {
      alert("도서명 / 저자 / 출판사 / 출판일 / 카테고리는 필수입니다.");
      return;
    }
    const dto = {
      title: form.title,
      author: form.author,
      publisher: form.publisher,
      publishDate: form.publishDate,
      category: form.category,
      ranking: form.ranking || undefined,
      pages: form.pages ? Number(form.pages) : undefined,
      price: form.price ? Number(form.price) : undefined,
      rating: form.rating ? Number(form.rating) : undefined,
      reviewCount: form.reviewCount ? Number(form.reviewCount) : undefined,
      description: form.description || undefined,
    };
    dispatch(createBookRequest({ dto, cover: coverFile }));
  };

  useEffect(() => {
    if (success) {
      alert("도서가 성공적으로 등록되었습니다.");
      dispatch(resetBookState());
      router.push("/books");
    }
  }, [success, router, dispatch]);

  // ★검색버튼을 누르면 카카오 API에서 도서를 가져와 자동으로 DB에 저장한 후,
  //   도서 목록 페이지로 이동합니다. (boot1 book/write.html 의 카카오검색 섹션과 동일한 흐름)
  const onKakaoSearch = (e) => {
    e.preventDefault();
    if (!kakaoSearch.trim()) {
      alert("검색할 도서명을 입력하세요.");
      return;
    }
    dispatch(kakaoInsertRequest(kakaoSearch.trim()));
  };

  useEffect(() => {
    if (kakaoInsertedCount !== null) {
      alert(`카카오 도서검색 결과 ${kakaoInsertedCount}권을 새로 등록했습니다.`);
      dispatch(resetKakaoInsertState());
      router.push("/books"); // ★저장 후 도서 목록 페이지로 이동
    }
  }, [kakaoInsertedCount, router, dispatch]);

  if (!user) {
    return <AccessDenied needLogin message="도서등록은 관리자만 가능합니다. 먼저 로그인해주세요." backHref="/books" />;
  }
  if (!isAdmin) {
    return <AccessDenied message="도서등록은 관리자(ROLE_ADMIN)만 가능합니다." backHref="/books" />;
  }

  return (
    <div className="write-wrap">
      <div className="write-card">
        <div className="write-header">
          <h2>📚 새 도서 등록</h2>
          <p>BookStore에 새로운 도서를 등록합니다.</p>
        </div>

        <div className="write-body">
          <form onSubmit={onSubmit}>
            <div style={{ display: "flex", gap: 32, flexWrap: "wrap" }}>

              {/* 입력 영역 */}
              <div style={{ flex: "2 1 480px", display: "grid", gridTemplateColumns: "2fr 1fr", gap: 16 }}>
                <div style={{ gridColumn: "span 2" }}>
                  <label className="bs-form-label">도서 제목 *</label>
                  <input name="title" className="bs-form-control" value={form.title} onChange={onChange} required />
                </div>

                <div>
                  <label className="bs-form-label">저자 *</label>
                  <input name="author" className="bs-form-control" value={form.author} onChange={onChange} required />
                </div>
                <div>
                  <label className="bs-form-label">출판사 *</label>
                  <input name="publisher" className="bs-form-control" value={form.publisher} onChange={onChange} required />
                </div>

                <div>
                  <label className="bs-form-label">출판일 *</label>
                  <input type="date" name="publishDate" className="bs-form-control" value={form.publishDate} onChange={onChange} required />
                </div>
                <div>
                  <label className="bs-form-label">카테고리 *</label>
                  <input name="category" className="bs-form-control" value={form.category} onChange={onChange} required placeholder="예: IT, 소설, 인문 ..." />
                </div>

                <div>
                  <label className="bs-form-label">판매 가격</label>
                  <input type="number" name="price" className="bs-form-control" value={form.price} onChange={onChange} min="0" />
                </div>
                <div>
                  <label className="bs-form-label">페이지 수</label>
                  <input type="number" name="pages" className="bs-form-control" value={form.pages} onChange={onChange} min="0" />
                </div>

                <div>
                  <label className="bs-form-label">랭킹</label>
                  <input name="ranking" className="bs-form-control" value={form.ranking} onChange={onChange} placeholder="예: TOP1" />
                </div>
                <div>
                  <label className="bs-form-label">평점</label>
                  <input type="number" step="0.1" name="rating" className="bs-form-control" value={form.rating} onChange={onChange} min="0" max="5" />
                </div>

                <div style={{ gridColumn: "span 2" }}>
                  <label className="bs-form-label">리뷰 수</label>
                  <input type="number" name="reviewCount" className="bs-form-control" value={form.reviewCount} onChange={onChange} min="0" />
                </div>

                <div style={{ gridColumn: "span 2" }}>
                  <label className="bs-form-label">도서설명</label>
                  <textarea name="description" className="bs-form-control" value={form.description} onChange={onChange} placeholder="도서 상세설명을 입력하세요." />
                </div>
              </div>

              {/* 표지이미지 영역 */}
              <div style={{ flex: "1 1 220px" }}>
                <div className="preview-box" style={{ marginBottom: 12, textAlign: "center" }}>
                  {previewUrl
                    ? <img src={previewUrl} alt="미리보기" style={{ width: 180, height: 240, objectFit: "cover", borderRadius: 10 }} />
                    : (
                      <div style={{
                        width: 180, height: 240, borderRadius: 10, background: "#eef1f5",
                        display: "flex", alignItems: "center", justifyContent: "center",
                        margin: "0 auto", color: "#bbb", fontSize: 40,
                      }}>📕</div>
                    )}
                </div>
                <div className="upload-box">
                  <label className="bs-form-label">표지이미지</label>
                  <input type="file" accept="image/*" onChange={onCoverChange} />
                </div>
              </div>
            </div>

            <div className="button-area">
              <button type="submit" className="btn btn-primary-bs" disabled={loading}>
                {loading ? "등록중..." : "도서 등록"}
              </button>
              <a className="btn btn-outline" onClick={(e) => { e.preventDefault(); router.push('/books'); }} href="/books">
                취소
              </a>
            </div>
            {error && <p style={{ color: "red", marginTop: 12 }}>{error}</p>}
          </form>
        </div>
      </div>

      {/* ============================= */}
      {/* 📚 도서 검색 및 등록 영역      */}
      {/* ============================= */}
      <div className="search-section">
        <div className="page-header" style={{ marginTop: 40, marginBottom: 14 }}>
          <div className="page-title" style={{ fontSize: 22 }}>📚 도서 검색 및 등록</div>
        </div>

        <div className="search-box-kakao">
          <form onSubmit={onKakaoSearch} style={{ display: "flex", gap: 8 }}>
            <input
              type="text"
              className="bs-form-control"
              placeholder="검색할 도서명을 입력하세요"
              value={kakaoSearch}
              onChange={(e) => setKakaoSearch(e.target.value)}
              required
            />
            <button type="submit" className="btn btn-primary-bs" disabled={kakaoLoading} style={{ whiteSpace: "nowrap" }}>
              {kakaoLoading ? "검색중..." : "검색 및 등록"}
            </button>
          </form>
        </div>

        <div className="alert-info-box">
          검색 버튼을 누르면 카카오 API에서 도서를 가져와 자동으로 DB에 저장한 후,
          도서 목록 페이지로 이동합니다.
        </div>

        {kakaoError && <p style={{ color: "red" }}>{kakaoError}</p>}
      </div>
    </div>
  );
}
