<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@ include file="../inc/header.jsp"%>

<style>

.delete-wrap{
    max-width:650px;
    margin:70px auto;
}

.delete-card{
    border:none;
    border-radius:18px;
    overflow:hidden;
    box-shadow:0 10px 30px rgba(0,0,0,.08);
}

.delete-header{
    background:#dc3545;
    color:#fff;
    padding:22px 28px;
}

.delete-header h3{
    margin:0;
    font-weight:700;
}

.delete-body{
    padding:35px;
}

.delete-icon{
    font-size:60px;
    text-align:center;
    margin-bottom:20px;
}

.form-control{
    height:50px;
    border-radius:10px;
}

.btn{
    min-width:120px;
    border-radius:10px;
}

</style>

<div class="container">

    <div class="delete-wrap">

        <div class="card delete-card">

            <div class="delete-header">

                <h3>🗑 게시글 삭제</h3>

            </div>

            <div class="delete-body">

                <div class="delete-icon">
                    ⚠️
                </div>

                <div class="alert alert-warning">

                    <strong>삭제된 게시글은 복구할 수 없습니다.</strong><br>
                    계속 진행하려면 작성 시 사용한 비밀번호를 입력하세요.

                </div>

                <form action="${pageContext.request.contextPath}/board/delete.do?bno=${param.bno}"
                      method="post"
                      onsubmit="return checkForm()">

                    <input type="hidden"
                           name="${_csrf.parameterName}"
                           value="${_csrf.token}">

                    <div class="mb-4">

                        <label class="form-label fw-bold">
                            비밀번호
                        </label>

                        <input
                            type="password"
                            class="form-control"
                            id="bpass"
                            name="bpass"
                            placeholder="비밀번호를 입력하세요">

                    </div>

                    <div class="d-flex justify-content-center gap-3">

                        <a href="${pageContext.request.contextPath}/board/list.do"
                           class="btn btn-outline-secondary">
                            목록
                        </a>

                        <button
                            type="reset"
                            class="btn btn-outline-dark">
                            다시입력
                        </button>

                        <button
                            type="submit"
                            class="btn btn-danger">
                            삭제하기
                        </button>

                    </div>

                </form>

            </div>

        </div>

    </div>

</div>

<script>

function checkForm(){

    const bpass=document.getElementById("bpass");

    if(bpass.value.trim()==""){

        alert("비밀번호를 입력해주세요.");

        bpass.focus();

        return false;

    }

    return confirm("정말 삭제하시겠습니까?");

}

</script>

<%@ include file="../inc/footer.jsp"%>