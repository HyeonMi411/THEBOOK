<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>도서 목록</title>

<style>
    body { font-family: 'Noto Sans KR', sans-serif; background:#f5f5f5; margin:0; padding:20px; }

    /* 제목 + 글쓰기 버튼 */
    h1 { 
        margin-bottom:20px; 
        display:flex; 
        justify-content:space-between; 
        align-items:center; 
    }

    .btn-write {
        background:#0077ff;
        color:#fff;
        padding:10px 18px;
        border-radius:6px;
        text-decoration:none;
        font-size:15px;
        transition:0.2s;
    }
    .btn-write:hover { background:#005fcc; }

    /* 카드 리스트 */
    .book-list { 
        display:grid; 
        grid-template-columns:repeat(3, 1fr); 
        gap:20px; 
    }
    .book-card {
        background:#fff; 
        border-radius:10px; 
        padding:15px;
        box-shadow:0 2px 6px rgba(0,0,0,0.1);
        transition:0.2s;
        cursor:pointer;
    }
    .book-card:hover { transform:translateY(-4px); }
    .book-card img { 
        width:100%; 
        border-radius:8px; 
        height:260px; 
        object-fit:cover; 
    }
    .title { font-size:18px; font-weight:700; margin:10px 0 5px; }
    .author { color:#555; margin-bottom:5px; }
    .rating { color:#f39c12; font-weight:700; }
    .category { font-size:14px; color:#888; }

    /* 페이징 */
    .pagination {
        margin-top:40px;
        display:flex;
        justify-content:center;
        gap:8px;
    }
    .pagination a {
        padding:8px 14px;
        background:#fff;
        border-radius:6px;
        border:1px solid #ddd;
        text-decoration:none;
        color:#333;
        font-size:14px;
        transition:0.2s;
    }
    .pagination a:hover {
        background:#0077ff;
        color:#fff;
        border-color:#0077ff;
    }
    .pagination .active {
        background:#0077ff;
        color:#fff;
        border-color:#0077ff;
    }
</style>
</head>

<body>

<!-- 제목 + 글쓰기 버튼 -->
<h1>
    📚 도서 목록
    <a href="/books/new" class="btn-write">+ 글쓰기</a>
</h1>

<!-- 도서 카드 리스트 -->
<div class="book-list">

    <c:forEach var="book" items="${list}">
        <div class="book-card" onclick="location.href='/books/${book.bookId}'">

            <img src="${book.bookCover != null ? book.bookCover : 'https://via.placeholder.com/300x260?text=No+Image'}">

            <div class="title">${book.title}</div>
            <div class="author">${book.author} · ${book.publisher}</div>
            <div class="category">${book.category}</div>

            <c:if test="${book.rating != null}">
                <div class="rating">⭐ ${book.rating} (${book.reviewCount}명)</div>
            </c:if>

        </div>
    </c:forEach>

</div>

<!-- 페이징 UI -->
<div class="pagination">

    <!-- 이전 -->
    <c:if test="${page > 1}">
        <a href="/books?page=${page - 1}">이전</a>
    </c:if>

    <!-- 페이지 번호 -->
    <c:forEach begin="1" end="${totalPage}" var="p">
        <a href="/books?page=${p}" class="${p == page ? 'active' : ''}">${p}</a>
    </c:forEach>

    <!-- 다음 -->
    <c:if test="${page < totalPage}">
        <a href="/books?page=${page + 1}">다음</a>
    </c:if>

</div>

</body>
</html>
