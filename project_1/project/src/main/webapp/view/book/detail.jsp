<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<%@ include file="../inc/header.jsp"%>

<style>
.detail-container{
    max-width:1100px;
    margin:60px auto;
}

.detail-card{
    background:#fff;
    border-radius:18px;
    box-shadow:0 8px 25px rgba(0,0,0,.08);
    overflow:hidden;
}

.book-cover{
    width:100%;
    height:520px;
    object-fit:cover;
    border-radius:12px;
}

.info-title{
    font-size:32px;
    font-weight:700;
    color:#222;
    margin-bottom:15px;
}

.info-meta{
    color:#666;
    font-size:15px;
    margin-bottom:12px;
}

.badge-category{
    display:inline-block;
    background:#eef4ff;
    color:#2563eb;
    padding:6px 14px;
    border-radius:30px;
    font-size:14px;
    font-weight:600;
    margin-bottom:18px;
}

.rating{
    font-size:20px;
    color:#f59e0b;
    font-weight:700;
    margin-bottom:20px;
}

.info-table{
    margin-top:20px;
}

.info-table th{
    width:120px;
    color:#666;
    font-weight:600;
    padding:10px 0;
}

.info-table td{
    color:#333;
    padding:10px 0;
}

.description{
    margin-top:30px;
    padding:25px;
    background:#f8f9fa;
    border-radius:12px;
    line-height:1.8;
    color:#444;
    white-space:pre-line;
}

.btn-area{
    margin-top:35px;
    display:flex;
    gap:12px;
}

.btn-area .btn{
    min-width:120px;
    border-radius:10px;
    font-weight:600;
}

@media(max-width:768px){

    .book-cover{
        height:380px;
    }

    .info-title{
        font-size:26px;
        margin-top:25px;
    }

    .btn-area{
        flex-direction:column;
    }

    .btn-area .btn{
        width:100%;
    }

}
</style>

<div class="container detail-container">

    <div class="detail-card">

        <div class="row g-0">

            <!-- 이미지 -->
            <div class="col-lg-4 p-4">

                <c:choose>

                    <c:when test="${not empty book.bookCover}">
                        <img
                            src="${pageContext.request.contextPath}/upload/${book.bookCover}"
                            class="book-cover"
                            alt="${book.title}">
                    </c:when>

                    <c:otherwise>
                        <img
                            src="https://via.placeholder.com/320x480?text=No+Image"
                            class="book-cover"
                            alt="No Image">
                    </c:otherwise>

                </c:choose>

            </div>

            <!-- 정보 -->
            <div class="col-lg-8">

                <div class="p-4 p-lg-5">

                    <h2 class="info-title">
                        ${book.title}
                    </h2>

                    <div class="info-meta">
                        ${book.author} · ${book.publisher}
                    </div>

                    <span class="badge-category">
                        ${book.category}
                    </span>

                    <c:if test="${book.rating != null}">
                        <div class="rating">
                            ⭐ ${book.rating}
                            <small class="text-secondary">
                                (${book.reviewCount} Reviews)
                            </small>
                        </div>
                    </c:if>

                    <table class="table info-table">

                        <tr>
                            <th>출판일</th>
                            <td>${book.publishDate}</td>
                        </tr>

                        <tr>
                            <th>페이지</th>
                            <td>${book.pages} Page</td>
                        </tr>

                        <tr>
                            <th>가격</th>
                            <td>
                                <strong class="text-primary">
                                    <fmt:formatNumber value="${book.price}" pattern="#,###"/>
                                    원
                                </strong>
                            </td>
                        </tr>

                        <tr>
                            <th>랭킹</th>
                            <td>${book.ranking}</td>
                        </tr>

                        <tr>
                            <th>등록일</th>
                            <td>${book.regDate}</td>
                        </tr>

                    </table>

                    <div class="description">
                        ${book.description}
                    </div>

                    <div class="btn-area">

                        <a href="${pageContext.request.contextPath}/book/list"
                           class="btn btn-outline-secondary">
                            목록
                        </a>

                        <a href="${pageContext.request.contextPath}/book/edit?bookId=${book.bookId}"
                           class="btn btn-primary">
                            수정
                        </a>

                        <a href="${pageContext.request.contextPath}/book/delete?bookId=${book.bookId}"
                           class="btn btn-danger"
                           onclick="return confirm('정말 삭제하시겠습니까?');">
                            삭제
                        </a>

                    </div>

                </div>

            </div>

        </div>

    </div>

</div>

<%@ include file="../inc/footer.jsp"%>