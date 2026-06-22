<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>

<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>BookStore</title>

    <!-- Bootstrap -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js"></script>

    <style>
        /* BookStore 스타일 */
        .header {
            width:100%;
            background:#ffffff;
            border-bottom:1px solid #e5e5e5;
            padding:12px 0;
            position:sticky;
            top:0;
            z-index:100;
        }
        .header-inner {
            max-width:1200px;
            margin:auto;
            display:flex;
            align-items:center;
            justify-content:space-between;
            gap:20px;
            padding:0 20px;
        }
        .logo {
            font-size:22px;
            font-weight:700;
            color:#333;
            white-space:nowrap;
        }
        .search-box {
            flex:1;
            display:flex;
            justify-content:center;
        }
        .search-box input {
            width:70%;
            padding:10px 14px;
            border:1px solid #ccc;
            border-radius:20px;
            font-size:14px;
            outline:none;
            transition:0.2s;
        }
        .search-box input:focus {
            border-color:#666;
        }
        .nav a {
            margin-left:20px;
            text-decoration:none;
            color:#333;
            font-size:15px;
            transition:0.2s;
        }
        .nav a:hover {
            color:#0077ff;
        }
        .login-btn {
            margin-left:20px;
            color:#0077ff;
            font-weight:600;
        }
        .logout-btn {
            margin-left:20px;
        }
    </style>
</head>

<body>

<!-- 통합 헤더 -->
<header class="header">
    <div class="header-inner">

        <!-- 로고 -->
        <div class="logo">📚 THEJOA703 BookStore</div>

        <!-- 검색창 -->
        <div class="search-box">
            <form action="/books/search" method="get" style="width:100%;">
                <input type="text" name="keyword" placeholder="도서 제목, 작가, 출판사를 검색하세요">
            </form>
        </div>

        <!-- 메뉴 + 로그인/로그아웃 -->
        <nav class="nav">

            <a href="/books">홈</a>
            <a href="/books/category/소설">소설</a>
            <a href="/books/category/에세이">에세이</a>
            <a href="/books/category/자기계발">자기계발</a>

            <!-- 로그인 안한 상태 -->
            <sec:authorize access="isAnonymous()">
                <a href="${pageContext.request.contextPath}/users/login" class="login-btn">Login</a>
                <a href="${pageContext.request.contextPath}/users/join" class="login-btn">Join</a>
            </sec:authorize>

            <!-- 로그인한 상태 -->
            <sec:authorize access="isAuthenticated()">
                <a href="${pageContext.request.contextPath}/users/mypage">
                    <sec:authentication property="principal.dto.email"/>
                </a>

                <form action="${pageContext.request.contextPath}/users/logout" method="post" class="logout-btn">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <input type="submit" value="로그아웃" class="btn btn-danger btn-sm"/>
                </form>
            </sec:authorize>

        </nav>

    </div>
</header>
