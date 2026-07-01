<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<%@ include file="../inc/header.jsp" %>

<style>

body{
    background:#f5f7fb;
}

/* ==========================
   Layout
========================== */

.join-wrap{
    max-width:560px;
    margin:60px auto;
}

.join-card{
    border:none;
    border-radius:20px;
    overflow:hidden;
    box-shadow:0 12px 35px rgba(0,0,0,.08);
}

.join-header{
    background:linear-gradient(135deg,#2563eb,#0d6efd);
    color:#fff;
    padding:35px;
    text-align:center;
}

.join-header h2{
    margin:0;
    font-weight:700;
}

.join-header p{
    margin-top:8px;
    opacity:.9;
}

/* ==========================
   Form
========================== */

.join-body{
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

.form-text{
    margin-top:6px;
    font-size:13px;
    border-radius:8px;
    padding:8px 12px;
}

/* 상태 */

.status-default{
    background:#fff3cd;
    color:#856404;
}

.status-success{
    background:#d1e7dd;
    color:#0f5132;
}

.status-danger{
    background:#f8d7da;
    color:#842029;
}

.status-info{
    background:#cff4fc;
    color:#055160;
}

/* 버튼 */

.btn{
    min-height:48px;
    border-radius:10px;
    font-weight:600;
}

.login-link{
    margin-top:25px;
    text-align:center;
    color:#777;
}

.login-link a{
    text-decoration:none;
    font-weight:700;
}

</style>

<div class="container">

    <div class="join-wrap">

        <div class="card join-card">

            <div class="join-header">

                <h2>회원가입</h2>

                <p>
                    BookStore 회원이 되어 다양한 서비스를 이용해보세요.
                </p>

            </div>

            <div class="join-body">

                <form action="${pageContext.request.contextPath}/users/join"
                      method="post"
                      onsubmit="return checkForm();">

                    <input type="hidden"
                           name="${_csrf.parameterName}"
                           value="${_csrf.token}">

                    <!-- 닉네임 -->

                    <div class="mb-4">

                        <label class="form-label">
                            닉네임
                        </label>

                        <input
                            type="text"
                            id="nickname"
                            name="nickname"
                            class="form-control"
                            placeholder="닉네임을 입력하세요">

                        <div class="form-text status-default tnickname">
                            닉네임 중복검사를 진행해주세요.
                        </div>

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

                    <!-- 이메일 -->

                    <div class="mb-4">

                        <label class="form-label">
                            이메일
                        </label>

                        <input
                            type="email"
                            id="email"
                            name="email"
                            class="form-control"
                            placeholder="example@bookstore.com">

                        <div class="form-text status-default target">
                            이메일 중복검사를 진행해주세요.
                        </div>

                    </div>

                    <!-- 휴대폰 -->

                    <div class="mb-4">

                        <label class="form-label">
                            휴대폰
                        </label>

                        <input
                            type="text"
                            id="mobile"
                            name="mobile"
                            class="form-control"
                            placeholder="010-1234-5678">

                    </div>

                    <!-- 버튼 -->

                    <div class="d-grid gap-2">

                        <button
                            class="btn btn-primary"
                            type="submit">

                            회원가입

                        </button>

                        <button
                            class="btn btn-outline-secondary"
                            type="reset">

                            초기화

                        </button>

                    </div>

                </form>

                <div class="login-link">

                    이미 회원이신가요?

                    <a href="${pageContext.request.contextPath}/users/login">

                        로그인

                    </a>

                </div>

            </div>

        </div>

    </div>

</div>

<script>

window.addEventListener("load",function(){

    checkDuplicate(
        "nickname",
        ".tnickname",
        "${pageContext.request.contextPath}/doubleNickname?nickname=",
        "닉네임"
    );

    checkDuplicate(
        "email",
        ".target",
        "${pageContext.request.contextPath}/doubleEmail?email=",
        "이메일"
    );

});

function checkDuplicate(id,target,url,label){

    const input=document.getElementById(id);
    const msg=document.querySelector(target);

    input.addEventListener("keyup",function(){

        const value=this.value.trim();

        if(value==""){

            msg.className="form-text status-default "+target.substring(1);
            msg.innerHTML=label+" 중복검사를 진행해주세요.";
            return;

        }

        fetch(url+encodeURIComponent(value))

        .then(res=>res.json())

        .then(data=>{

            if(data.exists){

                msg.className="form-text status-danger "+target.substring(1);
                msg.innerHTML="이미 사용중인 "+label+"입니다.";

            }else{

                msg.className="form-text status-success "+target.substring(1);
                msg.innerHTML="사용 가능한 "+label+"입니다.";

            }

        })

        .catch(()=>{

            msg.className="form-text status-info "+target.substring(1);
            msg.innerHTML="서버와 통신할 수 없습니다.";

        });

    });

}

function checkForm(){

    if(nickname.value.trim()==""){
        alert("닉네임을 입력하세요.");
        nickname.focus();
        return false;
    }

    if(bpass.value.trim()==""){
        alert("비밀번호를 입력하세요.");
        bpass.focus();
        return false;
    }

    if(email.value.trim()==""){
        alert("이메일을 입력하세요.");
        email.focus();
        return false;
    }

    if(mobile.value.trim()==""){
        alert("휴대폰 번호를 입력하세요.");
        mobile.focus();
        return false;
    }

    return confirm("회원가입을 진행하시겠습니까?");

}

</script>

<%@ include file="../inc/footer.jsp" %>