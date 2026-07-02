<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<%@ include file="../inc/header.jsp"%>

<style>

body{
    background:#f5f7fb;
}

/* ==========================
   Layout
========================== */

.mypage-wrap{
    max-width:700px;
    margin:60px auto;
}

/* ==========================
   Card
========================== */

.profile-card{

    border:none;
    border-radius:20px;
    overflow:hidden;
    box-shadow:0 12px 35px rgba(0,0,0,.08);

}

/* ==========================
   Header
========================== */

.profile-header{

    background:linear-gradient(135deg,#2563eb,#0d6efd);
    color:#fff;

    text-align:center;

    padding:45px 30px;

}

.profile-avatar{

    width:90px;
    height:90px;

    border-radius:50%;

    background:#fff;
    color:#2563eb;

    font-size:34px;
    font-weight:700;

    display:flex;
    align-items:center;
    justify-content:center;

    margin:0 auto 20px;

    box-shadow:0 8px 20px rgba(0,0,0,.15);

}

.profile-name{

    font-size:28px;
    font-weight:700;
    margin-bottom:8px;

}

.profile-email{

    opacity:.9;
    margin:0;

}

/* ==========================
   Body
========================== */

.profile-body{

    padding:35px;

}

.info-row{

    display:flex;
    justify-content:space-between;
    align-items:center;

    padding:18px 0;

    border-bottom:1px solid #eee;

}

.info-row:last-child{

    border-bottom:none;

}

.info-title{

    color:#666;
    font-weight:600;

}

.info-value{

    font-weight:700;
    color:#222;

}

.ip{

    background:#eef4ff;
    color:#2563eb;

    padding:6px 12px;

    border-radius:20px;

    font-size:14px;

}

/* ==========================
   Button
========================== */

.button-area{

    margin-top:35px;

}

.btn{

    min-width:150px;
    border-radius:10px;
    font-weight:600;

}

</style>

<div class="container">

    <div class="mypage-wrap">

        <div class="card profile-card">

            <!-- Header -->

            <div class="profile-header">

                <div class="profile-avatar">

                    ${dto.nickname.substring(0,1)}

                </div>

                <div class="profile-name">

                    ${dto.nickname}

                </div>

                <p class="profile-email">

                    ${dto.email}

                </p>

            </div>

            <!-- Body -->

            <div class="profile-body">

                <div class="info-row">

                    <div class="info-title">
                        📱 휴대폰 번호
                    </div>

                    <div class="info-value">
                        ${dto.mobile}
                    </div>

                </div>

                <div class="info-row">

                    <div class="info-title">
                        📅 가입일
                    </div>

                    <div class="info-value">
                        ${dto.udate}
                    </div>

                </div>

                <div class="info-row">

                    <div class="info-title">
                        🌐 최근 접속 IP
                    </div>

                    <div class="info-value">

                        <span class="ip">

                            ${dto.bip}

                        </span>

                    </div>

                </div>

                <div class="button-area">

                    <div class="d-grid">

                        <a
                            href="${pageContext.request.contextPath}/book/list"
                            class="btn btn-primary">

                            📚 도서 보러가기

                        </a>

                    </div>

                </div>

            </div>

        </div>

    </div>

</div>

<%@ include file="../inc/footer.jsp"%>