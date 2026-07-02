<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@ include file="../inc/header.jsp"%>

<style>

.edit-wrap{
    max-width:900px;
    margin:60px auto;
}

.edit-card{
    border:none;
    border-radius:18px;
    overflow:hidden;
    box-shadow:0 10px 30px rgba(0,0,0,.08);
}

.edit-header{
    background:#0d6efd;
    color:#fff;
    padding:24px 30px;
}

.edit-header h3{
    margin:0;
    font-weight:700;
}

.edit-body{
    padding:35px;
}

.form-label{
    font-weight:700;
    margin-bottom:8px;
}

.form-control{
    border-radius:10px;
    min-height:48px;
}

textarea.form-control{
    min-height:220px;
    resize:vertical;
}

.file-box{
    background:#f8f9fa;
    border:1px solid #dee2e6;
    border-radius:10px;
    padding:12px 15px;
    color:#666;
}

.preview{
    margin-top:20px;
    text-align:center;
}

.preview img{
    max-width:250px;
    max-height:320px;
    border-radius:12px;
    border:1px solid #ddd;
    box-shadow:0 5px 15px rgba(0,0,0,.08);
}

.button-area{
    border-top:1px solid #eee;
    padding-top:25px;
}

.btn{
    min-width:120px;
    border-radius:10px;
}

</style>

<div class="container">

    <div class="edit-wrap">

        <div class="card edit-card">

            <div class="edit-header">
                <h3>✏️ 공지사항 수정</h3>
            </div>

            <div class="edit-body">

                <form action="${pageContext.request.contextPath}/board/edit.do?bno=${dto.bno}"
                      method="post"
                      enctype="multipart/form-data"
                      onsubmit="return checkForm();">

                    <input type="hidden"
                           name="${_csrf.parameterName}"
                           value="${_csrf.token}">

                    <!-- 작성자 -->
                    <div class="mb-4">

                        <label class="form-label">
                            작성자
                        </label>

                        <input
                            type="text"
                            class="form-control"
                            id="bname"
                            name="bname"
                            value="${dto.bname}"
                            readonly>

                    </div>

                    <!-- 비밀번호 -->
                    <div class="mb-4">

                        <label class="form-label">
                            비밀번호
                        </label>

                        <input
                            type="password"
                            class="form-control"
                            id="bpass"
                            name="bpass"
                            placeholder="비밀번호를 입력하세요">

                    </div>

                    <!-- 제목 -->
                    <div class="mb-4">

                        <label class="form-label">
                            제목
                        </label>

                        <input
                            type="text"
                            class="form-control"
                            id="btitle"
                            name="btitle"
                            value="${dto.btitle}">

                    </div>

                    <!-- 내용 -->
                    <div class="mb-4">

                        <label class="form-label">
                            내용
                        </label>

                        <textarea
                            class="form-control"
                            id="bcontent"
                            name="bcontent">${dto.bcontent}</textarea>

                    </div>

                    <!-- 기존 첨부파일 -->
                    <div class="mb-3">

                        <label class="form-label">
                            현재 첨부파일
                        </label>

                        <div class="file-box">

                            <c:choose>

                                <c:when test="${not empty dto.bfile}">
                                    📎 ${dto.bfile}
                                </c:when>

                                <c:otherwise>
                                    첨부파일이 없습니다.
                                </c:otherwise>

                            </c:choose>

                        </div>

                    </div>

                    <!-- 이미지 미리보기 -->
                    <c:if test="${not empty dto.bfile}">

                        <div class="preview">

                            <img
                                src="${pageContext.request.contextPath}/upload/${dto.bfile}"
                                alt="${dto.btitle}">

                        </div>

                    </c:if>

                    <!-- 새 파일 -->
                    <div class="mt-4 mb-4">

                        <label class="form-label">
                            새 첨부파일
                        </label>

                        <input
                            type="file"
                            class="form-control"
                            name="file"
                            id="file">

                    </div>

                    <div class="button-area">

                        <div class="d-flex justify-content-end gap-2">

                            <button
                                type="reset"
                                class="btn btn-outline-secondary">
                                초기화
                            </button>

                            <a
                                href="${pageContext.request.contextPath}/board/list.do"
                                class="btn btn-outline-dark">
                                목록
                            </a>

                            <button
                                type="submit"
                                class="btn btn-primary">
                                수정 완료
                            </button>

                        </div>

                    </div>

                </form>

            </div>

        </div>

    </div>

</div>

<script>

function checkForm(){

    if(bpass.value.trim()==""){
        alert("비밀번호를 입력해주세요.");
        bpass.focus();
        return false;
    }

    if(btitle.value.trim()==""){
        alert("제목을 입력해주세요.");
        btitle.focus();
        return false;
    }

    if(bcontent.value.trim()==""){
        alert("내용을 입력해주세요.");
        bcontent.focus();
        return false;
    }

    return confirm("수정한 내용을 저장하시겠습니까?");

}

</script>

<%@ include file="../inc/footer.jsp"%>