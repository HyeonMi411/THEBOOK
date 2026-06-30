<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="../inc/header.jsp" %>

<style>
    /* BookStore 마이페이지 커스텀 스타일 */
    .profile-card {
        border-radius: 15px;
        background: #ffffff;
        border: 1px solid #e5e5e5;
    }
    
    .profile-header {
        border-bottom: 1px solid #f0f0f0;
        padding: 30px 20px;
        text-align: center;
        background-color: #fafafa;
        border-radius: 15px 15px 0 0;
    }

    .profile-avatar {
        width: 70px;
        height: 70px;
        background-color: #0077ff;
        color: #ffffff;
        font-size: 28px;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        margin: 0 auto 15px auto;
        box-shadow: 0 4px 10px rgba(0, 119, 255, 0.2);
    }

    .profile-name {
        font-size: 20px;
        font-weight: 700;
        color: #333;
        margin-bottom: 5px;
    }

    .profile-email {
        font-size: 14px;
        color: #888;
    }

    .info-list {
        padding: 10px 25px;
    }

    .info-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 18px 0;
        border-bottom: 1px solid #f5f5f5;
    }

    .info-item:last-child {
        border-bottom: none;
    }

    .info-label {
        font-size: 15px;
        font-weight: 600;
        color: #555;
    }

    .info-value {
        font-size: 15px;
        color: #333;
        font-weight: 500;
    }

    .info-value .badge {
        font-weight: 500;
        padding: 5px 10px;
        border-radius: 6px;
    }
    
    .btn-edit-profile {
        border-radius: 10px;
        padding: 12px;
        font-weight: 600;
        color: #0077ff;
        border: 1px solid #0077ff;
        background: #ffffff;
        transition: 0.2s;
    }
    
    .btn-edit-profile:hover {
        background-color: #0077ff;
        color: #ffffff;
    }
</style>

<div class="container my-5" style="max-width: 550px;">
    <div class="profile-card shadow-sm">
        
        <div class="profile-header">
            <div class="profile-avatar">
                ${dto.nickname.substring(0,1)}
            </div>
            <h4 class="profile-name">${dto.nickname} 님</h4>
            <p class="profile-email mb-0">${dto.email}</p>
        </div>

        <div class="card-body p-0">
            <div class="info-list">
                <div class="info-item">
                    <span class="info-label">휴대폰 번호</span>
                    <span class="info-value">${dto.mobile}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">가입일</span>
                    <span class="info-value text-muted">${dto.udate}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">접속 IP</span>
                    <span class="info-value"><span class="badge bg-light text-dark border">${dto.bip}</span></span>
                </div>
            </div>
            
            </div>
        
    </div>
</div>

<%@include file="../inc/footer.jsp" %>