<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ include file="../inc/header.jsp" %>

<style>
/* 전체 컨테이너 */
.notice-wrap {
    max-width: 1100px;
    margin: 60px auto;
}

/* 제목 */
.notice-title {
    font-size: 1.9rem;
    font-weight: 700;
    color: #1f2937;
    margin-bottom: 32px;
}

/* 카드 */
.notice-card {
    background: #ffffff;
    border-radius: 12px;
    padding: 25px 30px;
    box-shadow: 0 4px 16px rgba(0,0,0,0.05);
}

/* 테이블 */
.notice-table {
    width: 100%;
    border-collapse: separate;
    border-spacing: 0;
}

.notice-table thead th {
    background: #f9fafb;
    padding: 14px 16px;
    font-size: 0.85rem;
    font-weight: 600;
    color: #6b7280;
    border-bottom: 2px solid #e5e7eb;
    text-transform: uppercase;
}

.notice-table tbody td {
    padding: 16px;
    font-size: 0.95rem;
    color: #374151;
    border-bottom: 1px solid #f1f5f9;
    transition: background 0.2s ease;
}

.notice-table tbody tr:hover td {
    background: #f9fafc;
}

/* 링크 */
.notice-link {
    color: #1f2937;
    font-weight: 500;
    text-decoration: none;
    transition: color 0.2s ease;
}
.notice-link:hover {
    color: #0077ff;
    text-decoration: underline;
}

/* 페이징 */
.pagination-area {
    margin-top: 25px;
}

.custom-pagination .page-link {
    color: #4b5563;
    border: 1px solid #e5e7eb;
    padding: 8px 14px;
    border-radius: 6px;
    margin: 0 3px;
    transition: all 0.2s ease;
}
.custom-pagination .page-link:hover {
    background: #eef2ff;
    border-color: #c7d2fe;
    color: #4338ca;
}
.custom-pagination .active .page-link {
    background: #0077ff;
    border-color: #0077ff;
    color: #fff;
}

/* 글쓰기 버튼 */
.btn-write {
    background: #0077ff;
    color: #fff;
    padding: 10px 22px;
    border-radius: 8px;
    font-weight: 500;
    transition: 0.2s ease;
    text-decoration: none;
}
.btn-write:hover {
    background: #005fcc;
    box-shadow: 0 4px 10px rgba(0,119,255,0.25);
}
</style>

<script>
window.addEventListener("load", function(){
    let result = '${result}';
    if(result === "글쓰기 실패" || result === "비밀번호 확인!"){
        alert(result);
        history.go(-1);
    } else if(result.length !== 0){
        alert(result);
    }
});
</script>

<section class="notice-wrap">
    <h3 class="notice-title">공지사항</h3>

    <div class="notice-card">
        <table class="notice-table">
            <thead>
                <tr>
                    <th style="width: 8%;">NO</th>
                    <th style="width: 55%;" class="text-start">TITLE</th>
                    <th style="width: 12%;">WRITER</th>
                    <th style="width: 15%;">DATE</th>
                    <th style="width: 10%;">HIT</th>
                </tr>
            </thead>

            <tbody>
                <c:forEach var="dto" items="${list}" varStatus="status">
                    <tr>
                        <td>${paging.listtotal - paging.pstartno - status.index}</td>
                        <td class="text-start">
                            <a href="${pageContext.request.contextPath}/board/detail.do?bno=${dto.bno}"
                               class="notice-link">
                                ${dto.btitle}
                            </a>
                        </td>
                        <td>${dto.bname}</td>
                        <td><span style="color:#6b7280; font-size:0.9rem;">${dto.bdate}</span></td>
                        <td>${dto.bhit}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <!-- 페이징 -->
        <div class="row pagination-area align-items-center">
            <div class="col-sm-2"></div>

            <div class="col-sm-8 d-flex justify-content-center">
                <ul class="pagination custom-pagination mb-0">
                    <c:if test="${paging.start > paging.bottomlist}">
                        <li class="page-item">
                            <a href="?pstartno=${paging.start-1}" class="page-link">이전</a>
                        </li>
                    </c:if>

                    <c:forEach var="i" begin="${paging.start}" end="${paging.end}">
                        <li class="page-item <c:if test='${i==paging.current}'>active</c:if>'">
                            <a href="?pstartno=${i}" class="page-link">${i}</a>
                        </li>
                    </c:forEach>

                    <c:if test="${paging.pagetotal > paging.end}">
                        <li class="page-item">
                            <a href="?pstartno=${paging.end+1}" class="page-link">다음</a>
                        </li>
                    </c:if>
                </ul>
            </div>

            <div class="col-sm-2 text-end mt-3 mt-sm-0">
                <a href="${pageContext.request.contextPath}/board/write.do" class="btn-write">글쓰기</a>
            </div>
        </div>
    </div>
</section>

<%@ include file="../inc/footer.jsp" %>
