<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@ include file="../inc/header.jsp"%>

<script>
window.addEventListener("load",function(){

    let result='${result}';

    if(result==="비밀번호 확인!"){
        alert(result);
        history.back();
    }else if(result.length>0){
        alert(result);
    }

});
</script>

<style>

.detail-wrap{
    max-width:900px;
    margin:60px auto;
}

.detail-card{
    border:none;
    border-radius:18px;
    overflow:hidden;
    box-shadow:0 10px 30px rgba(0,0,0,.08);
}

.detail-header{
    padding:35px;
    border-bottom:1px solid #eee;
}

.detail-title{
    font-size:30px;
    font-weight:700;
    margin-bottom:15px;
}

.detail-info{
    display:flex;
    gap:30px;
    color:#666;
    font-size:15px;
}

.detail-image{
    padding:30px;
    text-align:center;
}

.detail-image img{
    max-width:100%;
    max-height:450px;
    border-radius:12px;
    box-shadow:0 8px 20px rgba(0,0,0,.1);
}

.detail-content{
    padding:35px;
    border-top:1px solid #eee;
    white-space:pre-wrap;
    line-height:1.9;
    font-size:17px;
    min-height:220px;
}

.detail-footer{
    padding:25px 35px;
    border-top:1px solid #eee;
}

.btn{
    min-width:110px;
    border-radius:10px;
}

</style>

<div class="container">

    <div class="detail-wrap">

        <div class="card detail-card">

            <div class="detail-header">

                <div class="detail-title">
                    ${dto.btitle}
                </div>

                <div class="detail-info">

                    <div>
                        👤 <strong>${dto.bname}</strong>
                    </div>

                    <div>
                        📌 공지사항
                    </div>

                </div>

            </div>

            <c:if test="${not empty dto.bfile}">

                <div class="detail-image">

                    <img
                        src="${pageContext.request.contextPath}/upload/${dto.bfile}"
                        alt="${dto.btitle}">

                </div>

            </c:if>

            <div class="detail-content">

                ${dto.bcontent}

            </div>

            <div class="detail-footer">

                <div class="d-flex justify-content-end gap-2">

                    <a href="${pageContext.request.contextPath}/board/edit.do?bno=${dto.bno}"
                       class="btn btn-outline-primary">
                        수정
                    </a>

                    <a href="${pageContext.request.contextPath}/board/delete.do?bno=${dto.bno}"
                       class="btn btn-outline-danger">
                        삭제
                    </a>

                    <a href="${pageContext.request.contextPath}/board/list.do"
                       class="btn btn-primary">
                        목록
                    </a>

                </div>

            </div>

        </div>

    </div>

</div>

<%@ include file="../inc/footer.jsp"%>