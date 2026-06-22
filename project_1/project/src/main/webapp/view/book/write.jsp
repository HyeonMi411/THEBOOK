<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>   

<%@include file="../inc/header.jsp"  %>
<!-- 	header		 -->
<!-- 	header		 -->
<title>도서 등록</title>

<style>
    body { font-family:'Noto Sans KR', sans-serif; background:#f5f5f5; margin:0; padding:30px; }
    h1 { margin-bottom:25px; }

    .form-container {
        max-width:700px;
        margin:auto;
        background:#fff;
        padding:25px;
        border-radius:12px;
        box-shadow:0 2px 8px rgba(0,0,0,0.1);
    }

    .form-group { margin-bottom:18px; }
    label { font-weight:600; display:block; margin-bottom:6px; }
    input, textarea {
        width:100%;
        padding:10px;
        border:1px solid #ccc;
        border-radius:6px;
        font-size:15px;
        box-sizing:border-box;
    }
    textarea { height:120px; resize:none; }

    .btn-area { margin-top:25px; display:flex; gap:10px; }
    .btn {
        padding:10px 18px;
        border-radius:6px;
        text-decoration:none;
        font-size:15px;
        text-align:center;
        cursor:pointer;
    }
    .btn-submit { background:#0077ff; color:#fff; border:none; }
    .btn-submit:hover { background:#005fcc; }
    .btn-cancel { background:#777; color:#fff; }

    /* 이미지 미리보기 */
    .preview-box {
        margin-top:10px;
        text-align:center;
    }
    .preview-box img {
        width:200px;
        height:280px;
        object-fit:cover;
        border-radius:8px;
        border:1px solid #ddd;
    }
</style>

<script>
    function previewImage() {
        const url = document.getElementById("bookCover").value;
        const img = document.getElementById("previewImg");
        img.src = url ? url : "https://via.placeholder.com/200x280?text=No+Image";
    }
</script>

</head>
<body>

<h1>📘 도서 등록</h1>

<div class="form-container">

    <form action="${pageContext.request.contextPath}/book/write" method="post"    
     enctype="multipart/form-data">

	    <input  type="hidden" name="${_csrf.parameterName}"  value="${_csrf.token}" />	 
        <div class="form-group">
            <label>제목</label>
            <input type="text" name="title" required>
        </div>

        <div class="form-group">
            <label>저자</label>
            <input type="text" name="author" required>
        </div>

        <div class="form-group">
            <label>출판사</label>
            <input type="text" name="publisher">
        </div>

        <div class="form-group">
            <label>출판일</label>
            <input type="date" name="publishDate">
        </div>

        <div class="form-group">
            <label>랭킹</label>
            <input type="text" name="ranking" required>
        </div>

        <div class="form-group">
            <label>조회수</label>
            <input type="text" name="reviewCount">
        </div>

        <div class="form-group">
            <label>별점</label>
            <input type="text" name="rating">
        </div>


        <div class="form-group">
            <label>카테고리</label>
            <input type="text" name="category">
        </div>

        <div class="form-group">
            <label>페이지 수</label>
            <input type="number" name="pages">
        </div>

        <div class="form-group">
            <label>가격</label>
            <input type="number" name="price">
        </div>

        <div class="form-group">
            <label>설명</label>
            <textarea name="description"></textarea>
        </div>

        <div class="form-group">
			<label for="file"   class="form-label">파일업로드</label>
			<input type="file"  id="file"  name="file"   class="form-control"/>
        </div>

        <div class="btn-area">
            <button type="submit" class="btn btn-submit">등록하기</button>
            <a href="/book/${book.bookId}" class="btn btn-cancel">취소</a>
        </div>

    </form>

</div>

<!-- 	footer		 -->
<!-- 	footer		 -->
<%@include file="../inc/footer.jsp"  %>