<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@ include file="../inc/header.jsp"%>

<style>

body{
    background:#f5f7fb;
}

/* =========================
   Page
========================= */

.notice-wrap{
    max-width:1200px;
    margin:60px auto;
}

.notice-header{
    display:flex;
    justify-content:space-between;
    align-items:center;
    margin-bottom:30px;
}

.notice-title h2{
    font-size:34px;
    font-weight:800;
    margin:0;
    color:#222;
}

.notice-title p{
    margin-top:8px;
    color:#777;
    font-size:15px;
}

/* =========================
   Card
========================= */

.notice-card{
    background:#fff;
    border-radius:18px;
    overflow:hidden;
    box-shadow:0 10px 30px rgba(0,0,0,.08);
}

/* =========================
   Table
========================= */

.notice-table{
    margin:0;
}

.notice-table thead{
    background:#f8f9fa;
}

.notice-table th{

    padding:18px;
    text-align:center;
    border:none;

    font-size:15px;
    color:#555;
    font-weight:700;

}

.notice-table td{

    padding:18px;
    text-align:center;
    vertical-align:middle;

}

.notice-table tbody tr{

    transition:.2s;

}

.notice-table tbody tr:hover{

    background:#f8fbff;

}

.notice-link{

    text-decoration:none;
    color:#222;
    font-weight:600;

}

.notice-link:hover{

    color:#0d6efd;

}

.notice-title-cell{

    text-align:left!important;

}

.notice-date{

    color:#777;
    font-size:14px;

}

/* =========================
   Button
========================= */

.btn-write{

    border-radius:30px;
    padding:10px 24px;
    font-weight:600;

}

/* =========================
   Pagination
========================= */

.pagination{

    margin:35px 0;

}

.page-link{

    border-radius:8px!important;
    margin:0 3px;

}

</style>

<script>

window.addEventListener("load",function(){

    let result='${result}';

    if(result=="글쓰기 실패" || result=="비밀번호 확인!"){

        alert(result);

        history.back();

    }else if(result.length>0){

        alert(result);

    }

});

</script>

<div class="container notice-wrap">

    <!-- Header -->

    <div class="notice-header">

        <div class="notice-title">

            <h2>📢 공지사항</h2>

            <p>
                BookStore의 새로운 소식과 이벤트를 확인하세요.
            </p>

        </div>

        <a href="${pageContext.request.contextPath}/board/write.do"
           class="btn btn-primary btn-write">

            + 글쓰기

        </a>

    </div>

    <!-- Table -->

    <div class="notice-card">

        <table class="table notice-table">

            <thead>

            <tr>

                <th width="8%">번호</th>
                <th width="54%">제목</th>
                <th width="12%">작성자</th>
                <th width="16%">작성일</th>
                <th width="10%">조회수</th>

            </tr>

            </thead>

            <tbody>

            <c:forEach var="dto"
                       items="${list}"
                       varStatus="status">

                <tr>

                    <td>

                        ${paging.listtotal-paging.pstartno-status.index}

                    </td>

                    <td class="notice-title-cell">

                        <a
                            class="notice-link"
                            href="${pageContext.request.contextPath}/board/detail.do?bno=${dto.bno}">

                            ${dto.btitle}

                        </a>

                    </td>

                    <td>

                        ${dto.bname}

                    </td>

                    <td class="notice-date">

                        ${dto.bdate}

                    </td>

                    <td>

                        ${dto.bhit}

                    </td>

                </tr>

            </c:forEach>

            <c:if test="${empty list}">

                <tr>

                    <td colspan="5" class="py-5 text-secondary">

                        등록된 공지사항이 없습니다.

                    </td>

                </tr>

            </c:if>

            </tbody>

        </table>

    </div>

    <!-- Pagination -->

    <nav>

        <ul class="pagination justify-content-center">

            <c:if test="${paging.start > paging.bottomlist}">

                <li class="page-item">

                    <a class="page-link"
                       href="?pstartno=${paging.start-1}">

                        이전

                    </a>

                </li>

            </c:if>

            <c:forEach var="i"
                       begin="${paging.start}"
                       end="${paging.end}">

                <li class="page-item ${i==paging.current?'active':''}">

                    <a class="page-link"
                       href="?pstartno=${i}">

                        ${i}

                    </a>

                </li>

            </c:forEach>

            <c:if test="${paging.pagetotal > paging.end}">

                <li class="page-item">

                    <a class="page-link"
                       href="?pstartno=${paging.end+1}">

                        다음

                    </a>

                </li>

            </c:if>

        </ul>

    </nav>

</div>

<%@ include file="../inc/footer.jsp"%>