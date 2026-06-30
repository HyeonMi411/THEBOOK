<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@include file="../inc/header.jsp" %>

<style>
    /* BookStore 수정 폼 전용 스타일 (write.jsp 테마 완벽 동화) */
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
    // 업로드할 파일 선택 시 변경된 이미지를 실시간으로 반영하는 함수
    function readURL(input) {
        if (input.files && input.files[0]) {
            const reader = new FileReader();
            reader.onload = function(e) {
                document.getElementById('previewImg').src = e.target.result;
            }
            reader.readAsDataURL(input.files[0]);
        } else {
            // 파일을 취소하거나 비었을 때 기본 보관된 도서 이미지를 복구
            document.getElementById('previewImg').src = "${pageContext.request.contextPath}/upload/${book.bookCover}";
        }
    }
</script>

<div class="form-container my-5">
    
    <h2 class="form-title">✏️ 도서 정보 수정</h2>

    <div class="card-form shadow-sm">
        <form action="${pageContext.request.contextPath}/book/edit?bookId=${book.bookId}" method="post" enctype="multipart/form-data">
            
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

            <div class="row g-3 mb-4">
                <div class="col-md-7">
                    <label class="form-label">도서 제목 <span class="text-danger">*</span></label>
                    <input type="text" name="title" class="form-control" value="${book.title}" required placeholder="도서명을 입력하세요">
                </div>
                <div class="col-md-5">
                    <label class="form-label">저자 <span class="text-danger">*</span></label>
                    <input type="text" name="author" class="form-control" value="${book.author}" required placeholder="저자 이름을 입력하세요">
                </div>
            </div>

            <div class="row g-3 mb-4">
                <div class="col-md-4">
                    <label class="form-label">출판사</label>
                    <input type="text" name="publisher" class="form-control" value="${book.publisher}" placeholder="출판사명">
                </div>
                <div class="col-md-4">
                    <label class="form-label">출판일</label>
                    <input type="date" name="publishDate" class="form-control" value="${book.publishDate}">
                </div>
                <div class="col-md-4">
                    <label class="form-label">카테고리</label>
                    <input type="text" name="category" class="form-control" value="${book.category}" placeholder="에세이, 소설, IT 등">
                </div>
            </div>

            <div class="row g-3 mb-4">
                <div class="col-md-4">
                    <label class="form-label">판매 가격 (원)</label>
                    <input type="number" name="price" class="form-control" value="${book.price}" placeholder="숫자만 입력">
                </div>
                <div class="col-md-4">
                    <label class="form-label">페이지 수</label>
                    <input type="number" name="pages" class="form-control" value="${book.pages}" placeholder="쪽 수">
                </div>
                <div class="col-md-4">
                    <label class="form-label">도서 랭킹 <span class="text-danger">*</span></label>
                    <input type="text" name="ranking" class="form-control" value="${book.ranking}" required placeholder="노출 순위">
                </div>
            </div>

            <div class="row g-3 mb-4">
                <div class="col-md-6">
                    <label class="form-label">평점 (별점)</label>
                    <input type="text" name="rating" class="form-control" value="${book.rating}" placeholder="0.0 ~ 5.0">
                </div>
                <div class="col-md-6">
                    <label class="form-label">리뷰 수 (조회수)</label>
                    <input type="text" name="reviewCount" class="form-control" value="${book.reviewCount}" placeholder="0">
                </div>
            </div>

            <div class="mb-4">
                <label class="form-label">도서 소개글</label>
                <textarea name="description" class="form-control" placeholder="책에 대한 상세 소개 문구를 작성해주세요.">${book.description}</textarea>
            </div>

            <div class="row g-3 align-items-stretch mb-2">
                <div class="col-sm-4 text-center">
                    <label class="form-label d-block text-sm-start">표지 미리보기</label>
                    <div class="preview-box">
                        <c:choose>
                            <c:when test="${not empty book.bookCover}">
                                <img id="previewImg" src="${pageContext.request.contextPath}/upload/${book.bookCover}" alt="Current Cover">
                            </c:when>
                            <c:otherwise>
                                <img id="previewImg" src="https://via.placeholder.com/140x190?text=No+Image" alt="No Cover">
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
                <div class="col-sm-8">
                    <div class="file-upload-box">
                        <label for="file" class="form-label fw-bold text-primary">📚 도서 표지 변경</label>
                        <input type="file" id="file" name="file" class="form-control" onchange="readURL(this);" />
                        <small class="text-muted d-block mt-2">※ 새로운 이미지를 선택하면 임시 미리보기가 작동하며, 저장 시 교체됩니다.</small>
                    </div>
                </div>
            </div>

            <div class="btn-area">
                <a href="javascript:history.go(-1)" class="btn-action btn-cancel-back">변경 취소</a>
                <button type="submit" class="btn-action btn-save shadow-sm">수정 완료</button>
            </div>

        </form>
    </div>
    
</div>

<%@include file="../inc/footer.jsp" %>