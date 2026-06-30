<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ include file="../inc/header.jsp" %>

<div class="container my-5" style="max-width: 900px;">

    <div class="card shadow-sm border-0">
        <div class="card-header bg-white border-bottom">
            <h4 class="mb-0 fw-bold">📝 공지사항 등록</h4>
        </div>

        <div class="card-body p-4">

            <form action="${pageContext.request.contextPath}/board/write.do"
                  method="post" enctype="multipart/form-data"
                  onsubmit="return checkForm()">

                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

                <!-- 이름 -->
                <div class="mb-4">
                    <label for="bname" class="form-label fw-semibold">작성자</label>
                    <input type="text" class="form-control form-control-lg"
                           id="bname" name="bname" placeholder="이름을 입력하세요" />
                </div>

                <!-- 비밀번호 -->
                <div class="mb-4">
                    <label for="bpass" class="form-label fw-semibold">비밀번호</label>
                    <input type="password" class="form-control form-control-lg"
                           id="bpass" name="bpass" placeholder="비밀번호를 입력하세요" />
                </div>

                <!-- 제목 -->
                <div class="mb-4">
                    <label for="btitle" class="form-label fw-semibold">제목</label>
                    <input type="text" class="form-control form-control-lg"
                           id="btitle" name="btitle" placeholder="제목을 입력하세요" />
                </div>

                <!-- 내용 -->
                <div class="mb-4">
                    <label for="bcontent" class="form-label fw-semibold">내용</label>
                    <textarea class="form-control" id="bcontent" name="bcontent"
                              rows="8" placeholder="내용을 입력하세요"></textarea>
                </div>

                <!-- 파일 업로드 -->
                <div class="mb-4">
                    <label for="file" class="form-label fw-semibold">파일 업로드</label>
                    <input type="file" class="form-control" id="file" name="file" />
                </div>

                <!-- 버튼 -->
                <div class="d-flex justify-content-end gap-2 mt-4">
                    <button type="reset" class="btn btn-outline-secondary px-4">취소</button>

                    <a href="${pageContext.request.contextPath}/board/list.do"
                       class="btn btn-outline-primary px-4">
                        목록
                    </a>

                    <button type="submit" class="btn btn-primary px-4">등록하기</button>
                </div>

            </form>

        </div>
    </div>
</div>

<script>
function checkForm() {
    let bname = document.getElementById("bname");
    let bpass = document.getElementById("bpass");
    let btitle = document.getElementById("btitle");
    let bcontent = document.getElementById("bcontent");

    if (bname.value.trim() === "") { alert("이름을 입력해주세요."); bname.focus(); return false; }
    if (bpass.value.trim() === "") { alert("비밀번호를 입력해주세요."); bpass.focus(); return false; }
    if (btitle.value.trim() === "") { alert("제목을 입력해주세요."); btitle.focus(); return false; }
    if (bcontent.value.trim() === "") { alert("내용을 입력해주세요."); bcontent.focus(); return false; }

    return true;
}
</script>

<%@ include file="../inc/footer.jsp" %>
