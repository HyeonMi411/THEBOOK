<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="../inc/header.jsp" %>

<div class="container my-5" style="max-width: 650px;">
    <div class="card shadow-sm border-0">
        <div class="card-header bg-primary text-white">
            <h4 class="mb-0">회원가입</h4>
        </div>

        <div class="card-body">

            <form action="${pageContext.request.contextPath}/users/join"
                  method="post" onsubmit="return checkForm()">

                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

                <!-- 닉네임 -->
                <div class="mb-3">
                    <label for="nickname" class="form-label fw-semibold">닉네임</label>
                    <input type="text" class="form-control form-control-lg"
                           id="nickname" name="nickname" placeholder="닉네임을 입력하세요" />
                </div>

                <div class="alert alert-warning tnickname mb-4">
                    닉네임 중복검사는 필수입니다.
                </div>

                <!-- 닉네임 중복 검사 스크립트 -->
                <script>
                    window.addEventListener("load", function() {
                        let nickname = document.getElementById("nickname");
                        let target = document.querySelector(".tnickname");

                        nickname.addEventListener("keyup", function(e) {
                            let value = e.target.value.trim();

                            if (value !== "") {
                                fetch("${pageContext.request.contextPath}/doubleNickname?nickname=" + encodeURIComponent(value))
                                    .then(response => response.json())
                                    .then(data => {
                                        if (data.exists) {
                                            target.textContent = "이미 사용중인 닉네임입니다.";
                                            target.className = "alert alert-danger tnickname";
                                        } else {
                                            target.textContent = "사용 가능한 닉네임입니다.";
                                            target.className = "alert alert-success tnickname";
                                        }
                                    })
                                    .catch(err => {
                                        target.textContent = "서버 오류입니다.";
                                        target.className = "alert alert-info tnickname";
                                    });
                            } else {
                                target.textContent = "닉네임 중복검사는 필수입니다.";
                                target.className = "alert alert-warning tnickname";
                            }
                        });
                    });
                </script>

                <!-- 비밀번호 -->
                <div class="mb-3">
                    <label for="bpass" class="form-label fw-semibold">비밀번호</label>
                    <input type="password" class="form-control form-control-lg"
                           id="bpass" name="bpass" placeholder="비밀번호를 입력하세요" />
                </div>

                <!-- 이메일 -->
                <div class="mb-3">
                    <label for="email" class="form-label fw-semibold">이메일</label>
                    <input type="email" class="form-control form-control-lg"
                           id="email" name="email" placeholder="이메일을 입력하세요" />
                </div>

                <div class="alert alert-warning target mb-4">
                    이메일 중복검사는 필수입니다.
                </div>

                <!-- 이메일 중복 검사 스크립트 -->
                <script>
                    window.addEventListener("load", function() {
                        let email = document.getElementById("email");
                        let target = document.querySelector(".target");

                        email.addEventListener("keyup", function(e) {
                            let value = e.target.value.trim();

                            if (value !== "") {
                                fetch("${pageContext.request.contextPath}/doubleEmail?email=" + encodeURIComponent(value))
                                    .then(response => response.json())
                                    .then(data => {
                                        if (data.exists) {
                                            target.textContent = "이미 사용중인 이메일입니다.";
                                            target.className = "alert alert-danger target";
                                        } else {
                                            target.textContent = "사용 가능한 이메일입니다.";
                                            target.className = "alert alert-success target";
                                        }
                                    })
                                    .catch(err => {
                                        target.textContent = "서버 오류입니다.";
                                        target.className = "alert alert-info target";
                                    });
                            } else {
                                target.textContent = "이메일 중복검사는 필수입니다.";
                                target.className = "alert alert-warning target";
                            }
                        });
                    });
                </script>

                <!-- 휴대폰 -->
                <div class="mb-4">
                    <label for="mobile" class="form-label fw-semibold">휴대폰</label>
                    <input type="text" class="form-control form-control-lg"
                           id="mobile" name="mobile" placeholder="휴대폰 번호를 입력하세요" />
                </div>

                <!-- 버튼 -->
                <div class="d-flex justify-content-end gap-2">
                    <button type="reset" class="btn btn-outline-secondary px-4">취소</button>
                    <button type="submit" class="btn btn-primary px-4">가입하기</button>
                </div>

            </form>

        </div>
    </div>
</div>

<script>
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
