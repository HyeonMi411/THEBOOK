<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>   
<%@include file="../inc/header.jsp"  %>

<script>
window.addEventListener("load", function() {
    let result = '${result}';
    console.log(result);

    if (result === "비밀번호 확인!") {
        alert(result);
        history.go(-1);
    } else if (result.length !== 0) {
        alert(result);
    }
});
</script>

<div class="container my-5" style="max-width: 800px;">
    <div class="card shadow-sm border-0">
        <div class="card-header bg-primary text-white">
            <h4 class="mb-0">글 상세보기</h4>
        </div>

        <div class="card-body">

            <form action="#" method="post">

                <div class="mb-4">
                    <label for="bname" class="form-label fw-semibold">이름</label>
                    <input type="text" class="form-control form-control-lg"
                           id="bname" name="bname" value="${dto.bname}" readonly />
                </div>

                <div class="mb-4">
                    <label for="btitle" class="form-label fw-semibold">제목</label>
                    <input type="text" class="form-control form-control-lg"
                           id="btitle" name="btitle" value="${dto.btitle}" readonly />
                </div>

                <div class="mb-4">
                    <label for="bcontent" class="form-label fw-semibold">내용</label>
                    <textarea class="form-control" id="bcontent" name="bcontent"
                              rows="7" readonly>${dto.bcontent}</textarea>
                </div>

                <c:if test="${not empty dto.bfile}">
                    <div class="mb-4 text-center">
                        <img src="${pageContext.request.contextPath}/upload/${dto.bfile}"
                             alt="${dto.btitle}"
                             class="img-fluid rounded shadow-sm"
                             style="max-height: 350px; object-fit: cover;" />
                    </div>
                </c:if>

                <div class="d-flex justify-content-end gap-2 mt-4">
                    <a href="${pageContext.request.contextPath}/board/edit.do?bno=${dto.bno}"
                       class="btn btn-outline-primary px-4">수정</a>

                    <a href="${pageContext.request.contextPath}/board/delete.do?bno=${dto.bno}"
                       class="btn btn-outline-danger px-4">삭제</a>

                    <a href="${pageContext.request.contextPath}/board/list.do"
                       class="btn btn-primary px-4">목록</a>
                </div>

            </form>

        </div>
    </div>
</div>

<%@include file="../inc/footer.jsp"  %>
