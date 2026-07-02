<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@ include file="../inc/header.jsp"%>

<style>

body{
    background:#f5f7fb;
}

/* ===========================
   Layout
=========================== */

.write-wrap{
    max-width:900px;
    margin:60px auto;
}

.write-card{
    border:none;
    border-radius:18px;
    overflow:hidden;
    box-shadow:0 10px 30px rgba(0,0,0,.08);
}

.write-header{
    background:linear-gradient(135deg,#0d6efd,#2563eb);
    color:#fff;
    padding:28px 35px;
}

.write-header h3{
    margin:0;
    font-size:30px;
    font-weight:700;
}

.write-header p{
    margin:8px 0 0;
    opacity:.9;
}

/* ===========================
   Form
=========================== */

.write-body{
    padding:35px;
}

.form-label{
    font-weight:700;
    color:#444;
}

.form-control{
    border-radius:10px;
    min-height:48px;
}

.form-control:focus{
    border-color:#0d6efd;
    box-shadow:0 0 0 .2rem rgba(13,110,253,.15);
}

textarea.form-control{
    min-height:220px;
    resize:vertical;
}

/* ===========================
   Upload
=========================== */

.upload-box{

    border:2px dashed #ced4da;
    border-radius:12px;
    padding:18px;
    background:#fafafa;

}

/* ===========================
   Button
=========================== */

.button-area{

    border-top:1px solid #eee;
    margin-top:35px;
    padding-top:25px;

}

.btn{

    min-width:120px;
    border-radius:10px;

}

</style>

<div class="container">

    <div class="write-wrap">

        <div class="card write-card">

            <div class="write-header">

                <h3>📝 공지사항 등록</h3>

                <p>
                    새로운 공지사항을 작성하여 회원들에게 안내할 수 있습니다.
                </p>

            </div>

            <div class="write-body">

                <form action="${pageContext.request.contextPath}/board/write.do"
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
                            id="bname"
                            name="bname"
                            class="form-control"
                            placeholder="작성자를 입력하세요">

                    </div>

                    <!-- 비밀번호 -->

                    <div class="mb-4">

                        <label class="form-label">
                            비밀번호
                        </label>

                        <input
                            type="password"
                            id="bpass"
                            name="bpass"
                            class="form-control"
                            placeholder="비밀번호를 입력하세요">

                    </div>

                    <!-- 제목 -->

                    <div class="mb-4">

                        <label class="form-label">
                            제목
                        </label>

                        <input
                            type="text"
                            id="btitle"
                            name="btitle"
                            class="form-control"
                            placeholder="공지사항 제목을 입력하세요">

                    </div>

                    <!-- 내용 -->

                    <div class="mb-4">

                        <label class="form-label">
                            내용
                        </label>

                        <textarea
                            id="bcontent"
                            name="bcontent"
                            class="form-control"
                            placeholder="공지사항 내용을 입력하세요"></textarea>

                    </div>

                    <!-- 파일 -->

                    <div class="mb-4">

                        <label class="form-label">
                            첨부파일
                        </label>

                        <div class="upload-box">

                            <input
                                type="file"
                                id="file"
                                name="file"
                                class="form-control">

                        </div>

                        <div class="form-text mt-2">
                            이미지 또는 첨부파일을 등록할 수 있습니다.
                        </div>

                    </div>

                    <!-- 버튼 -->

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

                                등록하기

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

    const bname=document.getElementById("bname");
    const bpass=document.getElementById("bpass");
    const btitle=document.getElementById("btitle");
    const bcontent=document.getElementById("bcontent");

    if(bname.value.trim()==""){
        alert("작성자를 입력해주세요.");
        bname.focus();
        return false;
    }

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

    return confirm("공지사항을 등록하시겠습니까?");

}

</script>

<%@ include file="../inc/footer.jsp"%>