<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="../inc/header.jsp" %>

<style>
    /* BookStore 테마 맞춤 스타일 */
    :root {
        --bs-primary: #0077ff;
    }

    .card {
        border-radius: 15px;
        overflow: hidden;
    }

    .card-header {
        background-color: #ffffff !important;
        border-bottom: 1px solid #eeeeee;
        padding: 25px 20px 15px 20px;
    }

    .card-header h4 {
        color: #333;
        font-weight: 700;
        text-align: center;
        letter-spacing: -0.5px;
    }

    .form-label {
        font-size: 14px;
        color: #555;
        margin-bottom: 8px;
    }

    .form-control {
        border-radius: 10px;
        padding: 12px 15px;
        border: 1px solid #ddd;
        font-size: 15px;
    }

    .form-control:focus {
        border-color: #0077ff;
        box-shadow: 0 0 0 0.25rem rgba(0, 119, 255, 0.1);
    }

    .btn-primary {
        background-color: #0077ff;
        border: none;
        border-radius: 10px;
        padding: 12px;
        font-weight: 600;
        transition: 0.2s;
    }

    .btn-primary:hover {
        background-color: #0056b3;
    }

    .btn-outline-secondary {
        border-radius: 10px;
        padding: 12px;
    }
</style>

<div class="container my-5" style="max-width: 450px;">

    <script>
        window.addEventListener("load", function() {
            let result = '${result}';
            console.log(result);

            if (result === "회원가입실패") {
                alert(result);
                history.go(-1);
            } else if (result.length !== 0) {
                alert(result);
            }
        });
    </script>

    <div class="card shadow-sm border-0">
        <div class="card-header">
            <h4 class="mb-0">로그인</h4>
        </div>

        <div class="card-body p-4">
            <form action="${pageContext.request.contextPath}/login" method="post" onsubmit="return checkForm()">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

                <div class="mb-3">
                    <label for="email" class="form-label fw-semibold">이메일</label>
                    <input type="email" class="form-control" id="email" name="username" placeholder="이메일을 입력하세요" />
                </div>

                <div class="mb-4">
                    <label for="bpass" class="form-label fw-semibold">비밀번호</label>
                    <input type="password" class="form-control" id="bpass" name="password" placeholder="비밀번호를 입력하세요" />
                </div>

                <div class="d-grid gap-2">
                    <button type="submit" class="btn btn-primary">로그인</button>
                    <button type="reset" class="btn btn-outline-secondary">취소</button>
                </div>

                <div class="text-center mt-4">
                    <span class="text-muted font-size-14">아직 회원이 아니신가요?</span>
                    <a href="${pageContext.request.contextPath}/users/join" class="text-decoration-none ms-1" style="color: #0077ff; font-weight: 600;">회원가입</a>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
function checkForm() {
    let email = document.getElementById("email");
    let bpass = document.getElementById("bpass");

    if (email.value.trim() === "") {
        alert("이메일을 입력하세요");
        email.focus();
        return false;
    }
    if (bpass.value.trim() === "") {
        alert("비밀번호를 입력하세요");
        bpass.focus();
        return false;
    }
    return true;
}
</script>

<%@include file="../inc/footer.jsp" %>