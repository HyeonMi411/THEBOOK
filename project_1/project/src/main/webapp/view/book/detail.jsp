<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>   
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@include file="../inc/header.jsp" %>

<style>
    body { font-family:'Noto Sans KR', sans-serif; background:#fafafa; margin:0; padding:30px; }
    .container { display:flex; gap:30px; max-width:900px; margin:auto; }
    img { width:280px; border-radius:10px; height:380px; object-fit:cover; }
    .info { flex:1; }
    .title { font-size:28px; font-weight:700; margin-bottom:10px; }
    .meta { color:#555; margin-bottom:10px; }
    .rating { font-size:18px; color:#f39c12; margin-bottom:10px; }
    .desc { margin-top:20px; line-height:1.6; }
    .box { background:#fff; padding:20px; border-radius:10px; box-shadow:0 2px 6px rgba(0,0,0,0.1); }

    /* 버튼 영역 */
    .btn-area {
        margin-top: 30px;
        display: flex;
        gap: 12px;
    }
    .btn-custom {
        padding: 10px 22px;
        border-radius: 8px;
        font-size: 15px;
        font-weight: 600;
        text-decoration: none;
        transition: 0.25s;
        display: inline-block;
    }
    .btn-back { background:#f1f3f5; color:#333; border:1px solid #d0d4d8; }
    .btn-back:hover { background:#e2e6ea; }
    .btn-edit { background:#4dabf7; color:white; }
    .btn-edit:hover { background:#339af0; }
    .btn-delete { background:#ff6b6b; color:white; }
    .btn-delete:hover { background:#fa5252; }
</style>

<section class="container my-5">

    <!-- 책 표지 -->
    <img src="${pageContext.request.contextPath}/upload/${book.bookCover}">

    <div class="info">

        <!-- 제목 -->
        <div class="title">${book.title}</div>

        <!-- 저자 / 출판사 / 출판일 -->
        <div class="meta">
            ${book.author} | ${book.publisher} | ${book.publishDate} 출간
        </div>

        <!-- 카테고리 -->
        <div class="meta">카테고리: ${book.category}</div>

        <!-- 랭킹 -->
        <c:if test="${book.ranking != null}">
            <div class="meta">랭킹: ${book.ranking}</div>
        </c:if>

        <!-- 평점 -->
        <c:if test="${book.rating != null}">
            <div class="rating">⭐ ${book.rating} (리뷰 ${book.reviewCount}개)</div>
        </c:if>

        <!-- 설명 -->
        <div class="box desc">
            ${book.description}
        </div>

        <!-- 기타 정보 -->
        <div class="meta" style="margin-top:20px;">
            페이지: ${book.pages}쪽 · 가격: <fmt:formatNumber value="${book.price}" type="number"/>원  
            <br>등록일: ${book.regDate}
        </div>

        <!-- 버튼 영역 -->
        <div class="btn-area">
            <a href="${pageContext.request.contextPath}/book/list" class="btn-custom btn-back">목록으로</a>
            <a href="${pageContext.request.contextPath}/book/edit?bookId=${book.bookId}" class="btn-custom btn-edit">수정</a>
            <a href="${pageContext.request.contextPath}/book/delete?bookId=${book.bookId}" 
               class="btn-custom btn-delete"
               onclick="return confirm('정말 삭제하시겠습니까?')">삭제</a>
        </div>

    </div>

</section>

<%@include file="../inc/footer.jsp" %>
