<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>   
    
<%@ taglib  prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>  
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>도서 목록</title>
<style>
    body { font-family: 'Noto Sans KR', sans-serif; background:#f5f5f5; margin:0; padding:20px; }
    h2 { margin-bottom:20px; }
    .book-list { display:grid; grid-template-columns:repeat(3, 1fr); gap:20px; }
    .book-card {
        background:#fff; border-radius:10px; padding:15px;
        box-shadow:0 2px 6px rgba(0,0,0,0.1);
        transition:0.2s;
    }
    .book-card:hover { transform:translateY(-4px); }
    .book-card img { width:100%; border-radius:8px; height:260px; object-fit:cover; }
    .title { font-size:18px; font-weight:700; margin:10px 0 5px; }
    .author { color:#555; margin-bottom:5px; }
    .rating { color:#f39c12; font-weight:700; }
    .category { font-size:14px; color:#888; }


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
</style>

</head>
<body>

<!-- 공통 헤더 -->
<header class="header">
    <div class="header-inner">
        <div class="logo">📚 BookStore</div>

        <div class="search-box">
            <input type="text" placeholder="도서 제목, 작가, 출판사를 검색하세요">
        </div>

        <nav class="nav">
            <a href="#">홈</a>
            <a href="#">베스트셀러</a>
            <a href="#">신간</a>
            <a href="#">카테고리</a>
        </nav>
    </div>
</header>



    <!--  header -->
    <!--  header -->
    <!--  header -->
    <!--  header -->
    <!--  jsp014_header.jsp -->
    <!--  jsp014_header.jsp -->

 