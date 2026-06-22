<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>   

<%@include file="../inc/header.jsp"  %>
<!-- 	header		 -->
<!-- 	header		 -->
    <section class="container  my-5">

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
            <a href="${pageContext.request.contextPath}/book/list" class="btn btn-back">목록으로</a>
            <a href="${pageContext.request.contextPath}/book/edit?bookId=${book.bookId}" class="btn btn-edit">수정</a>
            <a href="${pageContext.request.contextPath}/book/delete?bookId=${book.bookId}" class="btn btn-delete"
               onclick="return confirm('정말 삭제하시겠습니까?')">삭제</a>
        </div>

    </div>

</section>
<!-- 	footer		 -->
<!-- 	footer		 -->
<%@include file="../inc/footer.jsp"  %>
