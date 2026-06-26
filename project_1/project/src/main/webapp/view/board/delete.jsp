<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ include file="../inc/header.jsp" %>

<div class="container my-5" style="max-width: 500px;">
    <div class="card shadow-sm border-0">
        <div class="card-header bg-danger text-white">
            <h4 class="mb-0">글 삭제</h4>
        </div>

        <div class="card-body">
            <p class="text-muted mb-4">
                게시글을 삭제하려면 비밀번호를 입력해주세요.
            </p>

            <form action="${pageContext.request.contextPath}/board/delete.do?bno=${param.bno}"
                  method="post" onsubmit="return checkForm()">

                <div class="mb-3">
                    <label for="bpass" class="form-label fw-semibold">비밀번호</label>
                    <input type="password" class="form-control form-control-lg"
                           id="bpass" name="bpass" placeholder="비밀번호 입력" />
                </div>

                <div class="d-flex justify-content-between mt-4">
                    <button type="reset" class="btn btn-outline-secondary px-4">취소</button>
                    <a href="${pageContext.request.contextPath}/board/list.do"
                       class="btn btn-outline-success px-4">목록</a>
                    <button type="submit" class="btn btn-danger px-4">글삭제</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
function checkForm() {
    let bpass = document.getElementById("bpass");
    if (bpass.value.trim() === "") {
        alert("비밀번호를 입력해주세요.");
        bpass.focus();
        return false;
    }
    return true;
}
</script>

<%@ include file="../inc/footer.jsp" %>
