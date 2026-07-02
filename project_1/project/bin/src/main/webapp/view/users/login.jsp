<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<%@ include file="../inc/header.jsp"%>

<style>

body{
    background:#f5f7fb;
}

/* ==========================
   Layout
========================== */

.login-wrap{
    max-width:480px;
    margin:70px auto;
}

.login-card{
    border:none;
    border-radius:20px;
    overflow:hidden;
    box-shadow:0 12px 35px rgba(0,0,0,.08);
}

.login-header{
    background:linear-gradient(135deg,#2563eb,#0d6efd);
    color:#fff;
    padding:35px;
    text-align:center;
}

.login-header h2{
    margin:0;
    font-weight:700;
}

.login-header p{
    margin-top:8px;
    opacity:.9;
}

/* ==========================
   Form
========================== */

.login-body{
    padding:35px;
}

.form-label{
    font-weight:700;
    color:#444;
}

.form-control{
    min-height:50px;
    border-radius:12px;
    border:1px solid #dcdfe6;
}

.form-control:focus{
    border-color:#2563eb;
    box-shadow:0 0 0 .2rem rgba(37,99,235,.15);
}

/* ==========================
   Buttons
========================== */

.btn{
    min-height:48px;
    border-radius:10px;
    font-weight:600;
}

.join-link{
    text-align:center;
    margin-top:25px;
    color:#777;
}

.join-link a{
    text-decoration:none;
    font-weight:700;
}

</style>

<script>

window.addEventListener("load",function(){

    const result='${result}';

    if(result==="회원가입실패"){

        alert(result);
        history.back();

    }else if(result.length>0){

        alert(result);

    }

});

</script>

<div class="container">

    <div class="login-wrap">

        <div class="card login-card">

            <div class="login-header">

                <h2>로그인</h2>

                <p>
                    BookStore에 로그인하여 다양한 서비스를 이용하세요.
                </p>

            </div>

            <div class="login-body">

                <form action="${pageContext.request.contextPath}/login"
                      method="post"
                      onsubmit="return checkForm();">

                    <input type="hidden"
                           name="${_csrf.parameterName}"
                           value="${_csrf.token}">

                    <!-- 이메일 -->

                    <div class="mb-4">

                        <label class="form-label">
                            이메일
                        </label>

                        <input
                            type="email"
                            id="email"
                            name="username"
                            class="form-control"
                            placeholder="example@bookstore.com">

                    </div>

                    <!-- 비밀번호 -->

                    <div class="mb-4">

                        <label class="form-label">
                            비밀번호
                        </label>

                        <input
                            type="password"
                            id="bpass"
                            name="password"
                            class="form-control"
                            placeholder="비밀번호를 입력하세요">

                    </div>

                    <!-- 버튼 -->

                    <div class="d-grid gap-2">

                        <button
                            type="submit"
                            class="btn btn-primary">

                            로그인

                        </button>

                        <button
                            type="reset"
                            class="btn btn-outline-secondary">

                            초기화

                        </button>

                    </div>

                </form>

                <div class="join-link">

                    아직 회원이 아니신가요?

                    <a href="${pageContext.request.contextPath}/users/join">

                        회원가입

                    </a>

                </div>

            </div>

        </div>

    </div>

</div>

<script>

function checkForm(){

    const email=document.getElementById("email");
    const bpass=document.getElementById("bpass");

    if(email.value.trim()==""){

        alert("이메일을 입력해주세요.");

        email.focus();

        return false;

    }

    if(bpass.value.trim()==""){

        alert("비밀번호를 입력해주세요.");

        bpass.focus();

        return false;

    }

    return true;

}

</script>

<%@ include file="../inc/footer.jsp"%>