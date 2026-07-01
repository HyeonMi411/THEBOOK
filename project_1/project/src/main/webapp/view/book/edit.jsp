<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@ include file="../inc/header.jsp"%>

<style>
body{
    background:#f5f7fb;
}

.write-wrap{
    max-width:1000px;
    margin:60px auto;
}

.write-card{
    border:none;
    border-radius:20px;
    overflow:hidden;
    box-shadow:0 12px 35px rgba(0,0,0,.08);
}

.write-header{
    background:linear-gradient(135deg,#2563eb,#0d6efd);
    color:#fff;
    padding:35px 40px;
}

.write-header h2{
    margin:0;
    font-weight:700;
}

.write-header p{
    margin-top:8px;
    opacity:.9;
}

.write-body{
    padding:40px;
}

.form-label{
    font-weight:700;
    color:#444;
}

.form-control{
    min-height:48px;
    border-radius:10px;
    border:1px solid #dcdfe6;
}

.form-control:focus{
    border-color:#2563eb;
    box-shadow:0 0 0 .2rem rgba(37,99,235,.15);
}

textarea.form-control{
    min-height:180px;
    resize:vertical;
}

.preview-box{
    border:2px dashed #d8dee8;
    border-radius:15px;
    background:#fafbfc;
    padding:20px;
    text-align:center;
}

.preview-box img{
    width:180px;
    height:240px;
    object-fit:cover;
    border-radius:10px;
    box-shadow:0 6px 20px rgba(0,0,0,.12);
}

.upload-box{
    border:2px dashed #d8dee8;
    border-radius:15px;
    padding:25px;
    background:#fafbfc;
    margin-top:20px;
}

.button-area{
    margin-top:35px;
    padding-top:30px;
    border-top:1px solid #eee;
}

.btn{
    min-width:150px;
    border-radius:10px;
    font-weight:600;
}
</style>

<script>
function readURL(input){

    if(input.files && input.files[0]){

        const reader = new FileReader();

        reader.onload = function(e){
            document.getElementById("previewImg").src = e.target.result;
        }

        reader.readAsDataURL(input.files[0]);

    }else{

        <c:choose>
            <c:when test="${not empty book.bookCover}">
            document.getElementById("previewImg").src =
                "${pageContext.request.contextPath}/upload/${book.bookCover}";
            </c:when>
            <c:otherwise>
            document.getElementById("previewImg").src =
                "https://via.placeholder.com/180x240?text=Book";
            </c:otherwise>
        </c:choose>

    }

}
</script>

<div class="container">

    <div class="write-wrap">

        <div class="card write-card">

            <div class="write-header">
                <h2>✏️ 도서 정보 수정</h2>
                <p>등록된 도서 정보를 수정합니다.</p>
            </div>

            <div class="write-body">

                <form action="${pageContext.request.contextPath}/book/edit?bookId=${book.bookId}"
                      method="post"
                      enctype="multipart/form-data">

                    <input type="hidden"
                           name="${_csrf.parameterName}"
                           value="${_csrf.token}"/>

                    <div class="row g-4">

                        <!-- 입력 영역 -->
                        <div class="col-lg-8">

                            <div class="row g-3">

                                <div class="col-md-8">
                                    <label class="form-label">도서 제목</label>
                                    <input type="text"
                                           name="title"
                                           class="form-control"
                                           value="${book.title}"
                                           required>
                                </div>

                                <div class="col-md-4">
                                    <label class="form-label">저자</label>
                                    <input type="text"
                                           name="author"
                                           class="form-control"
                                           value="${book.author}"
                                           required>
                                </div>

                                <div class="col-md-4">
                                    <label class="form-label">출판사</label>
                                    <input type="text"
                                           name="publisher"
                                           class="form-control"
                                           value="${book.publisher}">
                                </div>

                                <div class="col-md-4">
                                    <label class="form-label">출판일</label>
                                    <input type="date"
                                           name="publishDate"
                                           class="form-control"
                                           value="${book.publishDate}">
                                </div>

                                <div class="col-md-4">
                                    <label class="form-label">카테고리</label>
                                    <input type="text"
                                           name="category"
                                           class="form-control"
                                           value="${book.category}">
                                </div>

                                <div class="col-md-4">
                                    <label class="form-label">판매 가격</label>
                                    <input type="number"
                                           name="price"
                                           class="form-control"
                                           value="${book.price}">
                                </div>

                                <div class="col-md-4">
                                    <label class="form-label">페이지 수</label>
                                    <input type="number"
                                           name="pages"
                                           class="form-control"
                                           value="${book.pages}">
                                </div>

                                <div class="col-md-4">
                                    <label class="form-label">랭킹</label>
                                    <input type="text"
                                           name="ranking"
                                           class="form-control"
                                           value="${book.ranking}">
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label">평점</label>
                                    <input type="text"
                                           name="rating"
                                           class="form-control"
                                           value="${book.rating}">
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label">리뷰 수</label>
                                    <input type="number"
                                           name="reviewCount"
                                           class="form-control"
                                           value="${book.reviewCount}">
                                </div>

                            </div>

                        </div>

                        <!-- 이미지 영역 -->
                        <div class="col-lg-4">

                            <div class="preview-box">

                                <c:choose>

                                    <c:when test="${not empty book.bookCover}">
                                        <img id="previewImg"
                                             src="${pageContext.request.contextPath}/upload/${book.bookCover}">
                                    </c:when>

                                    <c:otherwise>
                                        <img id="previewImg"
                                             src="https://via.placeholder.com/180x240?text=Book">
                                    </c:otherwise>

                                </c:choose>

                            </div>

                            <div class="upload-box">

                                <label class="form-label">표지 이미지 변경</label>

                                <input type="file"
                                       id="file"
                                       name="file"
                                       class="form-control"
                                       onchange="readURL(this);">

                                <div class="form-text mt-2">
                                    JPG, PNG 파일 업로드
                                </div>

                            </div>

                        </div>

                    </div>

                    <div class="mt-4">

                        <label class="form-label">도서 소개</label>

                        <textarea name="description"
                                  class="form-control">${book.description}</textarea>

                    </div>

                    <div class="button-area">

                        <div class="d-flex justify-content-end gap-2">

                            <a href="javascript:history.back();"
                               class="btn btn-outline-secondary">
                                취소
                            </a>

                            <button type="submit"
                                    class="btn btn-primary">
                                ✏️ 수정 완료
                            </button>

                        </div>

                    </div>

                </form>

            </div>

        </div>

    </div>

</div>

<%@ include file="../inc/footer.jsp"%>