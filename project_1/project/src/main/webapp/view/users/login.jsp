<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="../inc/header.jsp" %>

<div class="container my-5" style="max-width: 500px;">

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
        <div class="card-header bg-primary text-white">
            <h4 class="mb-0">로그인</h4>
        </div>

        <div class="card-body">

            <form action="${pageContext.request.contextPath}/login"
                  method="post" onsubmit="return checkForm()">

                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

                <!-- 이메일 -->
                <div class="mb-4">
                    <label for="email" class="form-label fw-semibold">이메일</label>
                    <input type="email" class="form-control form-control-lg"
                           id="email" name="username" placeholder="이메일을 입력하세요" />
                </div>

                <!-- 비밀번호 -->
                <div class="mb-4">
                    <label for="bpass" class="form-label fw-semibold">비밀번호</label>
                    <input type="password" class="form-control form-control-lg"
                           id="bpass" name="password" placeholder="비밀번호를 입력하세요" />
                </div>

                <!-- 버튼 -->
                <div class="d-flex justify-content-end gap-2">
                    <button type="reset" class="btn btn-outline-secondary px-4">취소</button>
                    <button type="submit" class="btn btn-primary px-4">로그인</button>
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
