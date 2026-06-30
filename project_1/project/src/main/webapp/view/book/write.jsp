<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@include file="../inc/header.jsp" %>

<style>
    /* BookStore 등록 폼 전용 스타일 */
    .form-container {
        max-width: 800px;
        margin: 0 auto;
        padding: 0 20px;
    }

    .form-title {
        font-size: 24px;
        font-weight: 700;
        color: #333;
        margin-bottom: 25px;
    }

    .card-form {
        background: #fff;
        border: 1px solid #e5e5e5;
        border-radius: 15px;
        padding: 35px;
    }

    .form-label {
        font-size: 14px;
        font-weight: 600;
        color: #444;
        margin-bottom: 8px;
    }

    .form-control {
        border-radius: 10px;
        padding: 11px 15px;
        border: 1px solid #ddd;
        font-size: 15px;
        transition: 0.2s;
    }

    .form-control:focus {
        border-color: #0077ff;
        box-shadow: 0 0 0 0.25rem rgba(0, 119, 255, 0.1);
    }

    textarea.form-control {
        height: 140px;
        resize: none;
    }

    /* 이미지 업로드 & 실시간 프리뷰 섹션 */
    .file-upload-box {
        background-color: #f9f9f9;
        border: 1px dashed #ccc;
        border-radius: 10px;
        padding: 20px;
        height: 100%;
        display: flex;
        flex-direction: column;
        justify-content: center;
    }

    .preview-box {
        text-align: center;
        background: #fff;
        border: 1px solid #eee;
        border-radius: 10px;
        padding: 15px;
    }

    .preview-box img {
        max-width: 140px;
        height: 190px;
        object-fit: cover;
        border-radius: 8px;
        box-shadow: 0 4px 10px rgba(0,0,0,0.08);
    }

    /* 하단 버튼 제어 영역 */
    .btn-area {
        margin-top: 30px;
        display: flex;
        gap: 12px;
        border-top: 1px solid #eee;
        padding-top: 25px;
    }

    .btn-action {
        padding: 12px 25px;
        border-radius: 10px;
        font-size: 15px;
        font-weight: 600;
        text-align: center;
        transition: 0.2s;
    }

    .btn-save {
        background: #0077ff;
        color: #fff;
        border: none;
        flex: 2;
    }

    .btn-save:hover {
        background: #0056b3;
        color: #fff;
    }

    .btn-cancel-back {
        background: #f1f3f5;
        color: #495057;
        border: 1px solid #ced4da;
        text-decoration: none;
        flex: 1;
    }

    .btn-cancel-back:hover {
        background: #e2e6ea;
        color: #212529;
    }
</style>

<script>
    // 업로드할 파일 선택 시 화면에 실시간으로 이미지를 띄워주는 함수
    function readURL(input) {
        if (input.files && input.files[0]) {
            const reader = new FileReader();
            reader.onload = function(e) {
                document.getElementById('previewImg').src = e.target.result;
            }
            reader.readAsDataURL(input.files[0]);
        } else {
            document.getElementById('previewImg').src = "https://via.placeholder.com/140x190?text=No+Image";
        }
    }
</script>

<div class="form-container my-5">
    
    <h2 class="form-title">📘 새 도서 등록</h2>

    <div class="card-form shadow-sm">
        <form action="${pageContext.request.contextPath}/book/write" method="post" enctype="multipart/form-data">
            
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

            <div class="row g-3 mb-4">
                <div class="col-md-7">
                    <label class="form-label">도서 제목 <span class="text-danger">*</span></label>
                    <input type="text" name="title" class="form-control" required placeholder="도서명을 입력하세요">
                </div>
                <div class="col-md-5">
                    <label class="form-label">저자 <span class="text-danger">*</span></label>
                    <input type="text" name="author" class="form-control" required placeholder="저자 이름을 입력하세요">
                </div>
            </div>

            <div class="row g-3 mb-4">
                <div class="col-md-4">
                    <label class="form-label">출판사</label>
                    <input type="text" name="publisher" class="form-control" placeholder="출판사명">
                </div>
                <div class="col-md-4">
                    <label class="form-label">출판일</label>
                    <input type="date" name="publishDate" class="form-control">
                </div>
                <div class="col-md-4">
                    <label class="form-label">카테고리</label>
                    <input type="text" name="category" class="form-control" placeholder="에세이, 소설, IT 등">
                </div>
            </div>

            <div class="row g-3 mb-4">
                <div class="col-md-4">
                    <label class="form-label">판매 가격 (원)</label>
                    <input type="number" name="price" class="form-control" placeholder="숫자만 입력">
                </div>
                <div class="col-md-4">
                    <label class="form-label">페이지 수</label>
                    <input type="number" name="pages" class="form-control" placeholder="쪽 수">
                </div>
                <div class="col-md-4">
                    <label class="form-label">도서 랭킹 <span class="text-danger">*</span></label>
                    <input type="text" name="ranking" class="form-control" required placeholder="초기 노출 순위">
                </div>
            </div>

            <div class="row g-3 mb-4">
                <div class="col-md-6">
                    <label class="form-label">초기 평점 (별점)</label>
                    <input type="text" name="rating" class="form-control" placeholder="0.0 ~ 5.0 (예: 4.5)">
                </div>
                <div class="col-md-6">
                    <label class="form-label">초기 리뷰 수 (조회수)</label>
                    <input type="text" name="reviewCount" class="form-control" placeholder="0">
                </div>
            </div>

            <div class="mb-4">
                <label class="form-label">도서 소개글</label>
                <textarea name="description" class="form-control" placeholder="책에 대한 상세 소개 문구를 작성해주세요."></textarea>
            </div>

            <div class="row g-3 align-items-stretch mb-2">
                <div class="col-sm-4 text-center">
                    <label class="form-label d-block text-sm-start">표지 미리보기</label>
                    <div class="preview-box">
                        <img id="previewImg" src="https://via.placeholder.com/140x190?text=No+Image" alt="Cover Preview">
                    </div>
                </div>
                <div class="col-sm-8">
                    <div class="file-upload-box">
                        <label for="file" class="form-label fw-bold text-primary">📚 도서 표지 이미지 업로드</label>
                        <input type="file" id="file" name="file" class="form-control" onchange="readURL(this);" />
                        <small class="text-muted d-block mt-2">※ 권장 비율은 가로 3: 세로 4 비율의 JPG, PNG 파일입니다.</small>
                    </div>
                </div>
            </div>

            <div class="btn-area">
                <a href="javascript:history.go(-1)" class="btn-action btn-cancel-back">등록 취소</a>
                <button type="submit" class="btn-action btn-save shadow-sm">신규 도서 등록</button>
            </div>

        </form>
    </div>
    
</div>

<%@include file="../inc/footer.jsp" %>