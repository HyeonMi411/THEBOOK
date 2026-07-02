<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>${book.title}</title>

<style>
    body { font-family:'Noto Sans KR', sans-serif; background:#f5f5f5; margin:0; padding:30px; }

    .detail-container {
        max-width:900px;
        margin:auto;
        background:#fff;
        padding:30px;
        border-radius:12px;
        box-shadow:0 2px 8px rgba(0,0,0,0.1);
        display:flex;
        gap:30px;
    }

    .book-img {
        width:280px;
        height:380px;
        border-radius:10px;
        object-fit:cover;
        border:1px solid #ddd;
    }

    .info { flex:1; }
    .title { font-size:28px; font-weight:700; margin-bottom:10px; }
    .meta { color:#555; margin-bottom:8px; font-size:15px; }
    .rating { font-size:18px; color:#f39c12; margin-bottom:10px; }
    .desc-box {
        background:#fafafa;
        padding:18px;
        border-radius:10px;
        line-height:1.6;
        margin-top:15px;
        border:1px solid #eee;
    }

    .btn-area { margin-top:25px; display:flex; gap:10px; }
    .btn {
        padding:10px 18px;
        border-radius:6px;
        text-decoration:none;
        font-size:15px;
        cursor:pointer;
    }
    .btn-edit { background:#3498db; color:#fff; }
    .btn-edit:hover { background:#2c82c9; }
    .btn-delete { background:#e74c3c; color:#fff; }
    .btn-delete:hover { background:#c0392b; }
    .btn-back { background:#777; color:#fff; }
    .btn-back:hover { background:#555; }
</style>

</head>
<body>

<h1 style="margin-bottom:25px;">📖 도서 상세</h1>

<div class="detail-container">

    <!-- 책 표지 -->
    <img class="book-img"
         src="${book.bookCover != null ? book.bookCover : 'https://via.placeholder.com/280x380?text=No+Image'}">

    <!-- 책 정보 -->
    <div class="info">

        <div class="title">${book.title}</div>

        <div class="meta">${book.author} | ${book.publisher} | ${book.publishDate}</div>
        <div class="meta">카테고리: ${book.category}</div>

        <c:if test="${book.ranking != null}">
            <div class="meta">랭킹: ${book.ranking}</div>
        </c:if>

        <c:if test="${book.rating != null}">
            <div class="rating">⭐ ${book.rating} (리뷰 ${book.reviewCount}개)</div>
        </c:if>

        <div class="desc-box">
            ${book.description}
        </div>

        <div class="meta" style="margin-top:15px;">
            페이지: ${book.pages}쪽  
            <br>가격: <fmt:formatNumber value="${book.price}" type="number"/>원  
            <br>등록일: ${book.regDate}
        </div>

        <!-- 버튼 -->
        <div class="btn-area">
            <a href="/books" class="btn btn-back">목록으로</a>
            <a href="/books/edit/${book.bookId}" class="btn btn-edit">수정</a>
            <a href="/books/delete/${book.bookId}" class="btn btn-delete"
               onclick="return confirm('정말 삭제하시겠습니까?')">삭제</a>
        </div>

    </div>

</div>

</body>
</html>
