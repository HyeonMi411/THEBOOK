<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@ include file="../inc/header.jsp"%>

<style>
body{
    background:#f5f7fb;
}

/* ===========================
   Layout
=========================== */

.book-wrap{
    max-width:1300px;
    margin:60px auto;
}

/* ===========================
   Header
=========================== */

.page-header{
    display:flex;
    justify-content:space-between;
    align-items:center;
    margin-bottom:35px;
}

.page-title{
    font-size:34px;
    font-weight:700;
    color:#222;
}

.page-title span{
    color:#2563eb;
}

.btn-write{
    border-radius:10px;
    padding:10px 24px;
    font-weight:600;
}

/* ===========================
   Card Grid
=========================== */

.book-grid{
    display:grid;
    grid-template-columns:repeat(auto-fill,minmax(260px,1fr));
    gap:28px;
}

/* ===========================
   Card
=========================== */

.book-card{
    background:#fff;
    border-radius:18px;
    overflow:hidden;
    cursor:pointer;
    box-shadow:0 8px 20px rgba(0,0,0,.08);
    transition:.25s;
}

.book-card:hover{
    transform:translateY(-8px);
    box-shadow:0 18px 35px rgba(0,0,0,.15);
}

.book-cover{
    height:360px;
    overflow:hidden;
}

.book-cover img{
    width:100%;
    height:100%;
    object-fit:cover;
    transition:.3s;
}

.book-card:hover .book-cover img{
    transform:scale(1.05);
}

.book-body{
    padding:18px;
}

.book-title{
    font-size:20px;
    font-weight:700;
    color:#222;
    margin-bottom:10px;
    overflow:hidden;
    text-overflow:ellipsis;
    white-space:nowrap;
}

.book-author{
    color:#666;
    margin-bottom:10px;
}

.book-category{
    display:inline-block;
    background:#eef4ff;
    color:#2563eb;
    padding:4px 10px;
    border-radius:30px;
    font-size:13px;
    margin-bottom:15px;
}

.book-rating{
    display:flex;
    justify-content:space-between;
    align-items:center;
}

.star{
    color:#f59e0b;
    font-weight:700;
}

.review{
    color:#888;
    font-size:14px;
}

/* ===========================
   Pagination
=========================== */

.pagination-area{
    margin-top:50px;
}

.page-link{
    border-radius:8px !important;
    margin:0 4px;
}

.page-item.active .page-link{
    background:#2563eb;
    border-color:#2563eb;
}
</style>

<div class="container">

    <div class="book-wrap">

        <div class="page-header">

            <div class="page-title">
                📚 <span>BookStore</span>
            </div>

            <a href="${pageContext.request.contextPath}/book/write"
               class="btn btn-primary btn-write">
                + 도서 등록
            </a>

        </div>

        <div class="book-grid">

            <c:forEach var="book" items="${list}">

                <div class="book-card"
                     onclick="location.href='${pageContext.request.contextPath}/book/detail?bookId=${book.bookId}'">

                    <div class="book-cover">

                        <img src="${pageContext.request.contextPath}/upload/${book.bookCover}"
                             alt="${book.title}">

                    </div>

                    <div class="book-body">

                        <div class="book-title">
                            ${book.title}
                        </div>

                        <div class="book-author">
                            ${book.author} · ${book.publisher}
                        </div>

                        <div class="book-category">
                            ${book.category}
                        </div>

                        <c:if test="${book.rating != null}">

                            <div class="book-rating">

                                <div class="star">
                                    ⭐ ${book.rating}
                                </div>

                                <div class="review">
                                    리뷰 ${book.reviewCount}
                                </div>

                            </div>

                        </c:if>

                    </div>

                </div>

            </c:forEach>

        </div>

        <div class="pagination-area">

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

                    <li class="page-item ${i == paging.current ? 'active' : ''}">

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

        </div>

    </div>

</div>

<%@ include file="../inc/footer.jsp"%>