<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec"%>

<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>BookStore</title>

<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js"></script>

<style>

*{
    margin:0;
    padding:0;
    box-sizing:border-box;
}

body{
    background:#f5f6f8;
    font-family:'Noto Sans KR',sans-serif;
}

/* ================= Header ================= */

.header{
    position:sticky;
    top:0;
    z-index:1000;
    background:#fff;
    border-bottom:1px solid #e9ecef;
    box-shadow:0 4px 18px rgba(0,0,0,.05);
}

.header-inner{
    max-width:1280px;
    margin:auto;
    height:80px;

    display:flex;
    align-items:center;
    justify-content:space-between;

    gap:30px;
    padding:0 24px;
}

/* Logo */

.logo{
    font-size:30px;
    font-weight:800;
    color:#2563eb;
    text-decoration:none;
    letter-spacing:-1px;
    white-space:nowrap;
}

.logo:hover{
    color:#1d4ed8;
}

/* Search */

.search-box{
    flex:1;
    max-width:600px;
}

.search-form{
    position:relative;
}

.search-form input{
    width:100%;
    height:50px;
    border:2px solid #2563eb;
    border-radius:40px;
    padding:0 60px 0 22px;
    font-size:15px;
    transition:.25s;
    background:#fff;
}

.search-form input:focus{
    outline:none;
    box-shadow:0 0 0 5px rgba(37,99,235,.15);
}

.search-btn{
    position:absolute;
    right:6px;
    top:6px;

    width:38px;
    height:38px;

    border:none;
    border-radius:50%;

    background:#2563eb;
    color:#fff;

    font-size:17px;
    transition:.2s;
}

.search-btn:hover{
    background:#1d4ed8;
}

/* Navigation */

.nav-menu{
    display:flex;
    align-items:center;
    gap:22px;
}

.nav-menu>a{
    color:#444;
    text-decoration:none;
    font-weight:600;
    position:relative;
    transition:.2s;
}

.nav-menu>a::after{
    content:'';
    position:absolute;
    left:0;
    bottom:-5px;
    width:0;
    height:2px;
    background:#2563eb;
    transition:.25s;
}

.nav-menu>a:hover{
    color:#2563eb;
}

.nav-menu>a:hover::after{
    width:100%;
}

/* User */

.user-name{
    color:#2563eb !important;
    font-weight:700 !important;
}

.logout-form{
    margin:0;
}

.logout-form button{
    border-radius:30px;
    padding:6px 16px;
}

/* Mobile */

@media(max-width:900px){

    .header-inner{
        height:auto;
        flex-wrap:wrap;
        padding:18px;
    }

    .logo{
        width:100%;
        text-align:center;
    }

    .search-box{
        width:100%;
        order:2;
        max-width:100%;
    }

    .nav-menu{
        width:100%;
        justify-content:center;
        flex-wrap:wrap;
        gap:16px;
        order:3;
    }

}

</style>

</head>

<body>

<header class="header">

    <div class="header-inner">

        <!-- Logo -->
        <a class="logo"
           href="${pageContext.request.contextPath}/book/list">
            📚 BookStore
        </a>

        <!-- Search -->
        <div class="search-box">

            <form class="search-form"
                  action="${pageContext.request.contextPath}/book/search"
                  method="get">

                <input
                    type="text"
                    name="keyword"
                    placeholder="도서명 · 작가 · 출판사 검색">

                <button class="search-btn" type="submit">
                    🔍
                </button>

            </form>

        </div>

        <!-- Navigation -->
        <nav class="nav-menu">

            <a href="${pageContext.request.contextPath}/book/list">
                📖 도서
            </a>

            <a href="${pageContext.request.contextPath}/board/list.do">
                📢 공지사항
            </a>

            <sec:authorize access="isAnonymous()">

                <a href="${pageContext.request.contextPath}/users/login">
                    로그인
                </a>

                <a href="${pageContext.request.contextPath}/users/join">
                    회원가입
                </a>

            </sec:authorize>

            <sec:authorize access="isAuthenticated()">

                <a class="user-name"
                   href="${pageContext.request.contextPath}/users/mypage">
                    👤 <sec:authentication property="principal.dto.email"/>
                </a>

                <form class="logout-form"
                      action="${pageContext.request.contextPath}/users/logout"
                      method="post">

                    <input type="hidden"
                           name="${_csrf.parameterName}"
                           value="${_csrf.token}"/>

                    <button class="btn btn-outline-danger btn-sm">
                        로그아웃
                    </button>

                </form>

            </sec:authorize>

        </nav>

    </div>

</header>

</body>
</html>