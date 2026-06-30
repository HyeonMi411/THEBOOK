<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="../inc/header.jsp" %>

<style>
    /* BookStore 테마 맞춤 스타일 */
    :root {
        --bs-primary: #0077ff;
        --bs-primary-rgb: 0, 119, 255;
    }

    .card {
        border-radius: 15px;
        overflow: hidden;
    }

    .card-header {
        background-color: #ffffff !important;
        border-bottom: 1px solid #eeeeee;
        padding: 20px;
    }

    .card-header h4 {
        color: #333;
        font-weight: 700;
        text-align: center;
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

    /* 중복검사 알림창 스타일 슬림화 */
    .alert {
        border-radius: 10px;
        padding: 8px 15px;
        font-size: 13px;
        margin-top: 8px;
        border: none;
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

<div class="container my-5" style="max-width: 500px;">
    <div class="card shadow-sm border-0">
        <div class="card-header">
            <h4 class="mb-0">회원가입</h4>
        </div>

        <div class="card-body p-4">
            <form action="${pageContext.request.contextPath}/users/join" method="post" onsubmit="return checkForm()">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

                <div class="mb-3">
                    <label for="nickname" class="form-label fw-semibold">닉네임</label>
                    <input type="text" class="form-control" id="nickname" name="nickname" placeholder="닉네임을 입력하세요" />
                    <div class="alert alert-warning tnickname">
                        닉네임 중복검사는 필수입니다.
                    </div>
                </div>

                <div class="mb-3">
                    <label for="bpass" class="form-label fw-semibold">비밀번호</label>
                    <input type="password" class="form-control" id="bpass" name="bpass" placeholder="비밀번호를 입력하세요" />
                </div>

                <div class="mb-3">
                    <label for="email" class="form-label fw-semibold">이메일</label>
                    <input type="email" class="form-control" id="email" name="email" placeholder="example@bookstore.com" />
                    <div class="alert alert-warning target">
                        이메일 중복검사는 필수입니다.
                    </div>
                </div>

                <div class="mb-4">
                    <label for="mobile" class="form-label fw-semibold">휴대폰</label>
                    <input type="text" class="form-control" id="mobile" name="mobile" placeholder="010-0000-0000" />
                </div>

                <div class="d-grid gap-2">
                    <button type="submit" class="btn btn-primary">가입하기</button>
                    <button type="reset" class="btn btn-outline-secondary">취소</button>
                </div>
                
                <div class="text-center mt-4">
                    <span class="text-muted font-size-14">이미 계정이 있으신가요?</span>
                    <a href="${pageContext.request.contextPath}/users/login" class="text-decoration-none ms-1" style="color: #0077ff; font-weight: 600;">로그인</a>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    window.addEventListener("load", function() {
        // 닉네임 중복 체크
        let nicknameInput = document.getElementById("nickname");
        let nicknameTarget = document.querySelector(".tnickname");

        nicknameInput.addEventListener("keyup", function(e) {
            let value = e.target.value.trim();
            if (value !== "") {
                fetch("${pageContext.request.contextPath}/doubleNickname?nickname=" + encodeURIComponent(value))
                    .then(response => response.json())
                    .then(data => {
                        if (data.exists) {
                            nicknameTarget.textContent = "이미 사용중인 닉네임입니다.";
                            nicknameTarget.className = "alert alert-danger tnickname";
                        } else {
                            nicknameTarget.textContent = "사용 가능한 닉네임입니다.";
                            nicknameTarget.className = "alert alert-success tnickname";
                        }
                    })
                    .catch(err => {
                        nicknameTarget.textContent = "서버 오류입니다.";
                        nicknameTarget.className = "alert alert-info tnickname";
                    });
            } else {
                nicknameTarget.textContent = "닉네임 중복검사는 필수입니다.";
                nicknameTarget.className = "alert alert-warning tnickname";
            }
        });

        // 이메일 중복 체크
        let emailInput = document.getElementById("email");
        let emailTarget = document.querySelector(".target");

        emailInput.addEventListener("keyup", function(e) {
            let value = e.target.value.trim();
            if (value !== "") {
                fetch("${pageContext.request.contextPath}/doubleEmail?email=" + encodeURIComponent(value))
                    .then(response => response.json())
                    .then(data => {
                        if (data.exists) {
                            emailTarget.textContent = "이미 사용중인 이메일입니다.";
                            emailTarget.className = "alert alert-danger target";
                        } else {
                            emailTarget.textContent = "사용 가능한 이메일입니다.";
                            emailTarget.className = "alert alert-success target";
                        }
                    })
                    .catch(err => {
                        emailTarget.textContent = "서버 오류입니다.";
                        emailTarget.className = "alert alert-info target";
                    });
            } else {
                emailTarget.textContent = "이메일 중복검사는 필수입니다.";
                emailTarget.className = "alert alert-warning target";
            }
        });
    });

    function checkForm() {
        let nickname = document.getElementById("nickname");
        let bpass = document.getElementById("bpass");
        let email = document.getElementById("email");
        let mobile = document.getElementById("mobile");

        if (nickname.value.trim() === "") { alert("닉네임을 입력하세요"); nickname.focus(); return false; }
        if (bpass.value.trim() === "") { alert("비밀번호를 입력하세요"); bpass.focus(); return false; }
        if (email.value.trim() === "") { alert("이메일을 입력하세요"); email.focus(); return false; }
        if (mobile.value.trim() === "") { alert("휴대폰 번호를 입력하세요"); mobile.focus(); return false; }

        return true;
    }
</script>

<%@include file="../inc/footer.jsp" %>