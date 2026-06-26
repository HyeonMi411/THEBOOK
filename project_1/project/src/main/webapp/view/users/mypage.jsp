<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="../inc/header.jsp" %>

<div class="container my-5" style="max-width: 700px;">

    <div class="card shadow-sm border-0">
        <div class="card-header bg-primary text-white">
            <h4 class="mb-0">My Page</h4>
        </div>

        <div class="card-body">

            <table class="table table-hover align-middle mb-0">
                <caption class="visually-hidden">User Info</caption>
                <tbody>
                    <tr>
                        <th scope="row" class="bg-light fw-semibold" style="width: 25%;">닉네임</th>
                        <td>${dto.nickname}</td>
                    </tr>
                    <tr>
                        <th scope="row" class="bg-light fw-semibold">이메일</th>
                        <td>${dto.email}</td>
                    </tr>
                    <tr>
                        <th scope="row" class="bg-light fw-semibold">휴대폰</th>
                        <td>${dto.mobile}</td>
                    </tr>
                    <tr>
                        <th scope="row" class="bg-light fw-semibold">가입일</th>
                        <td>${dto.udate}</td>
                    </tr>
                    <tr>
                        <th scope="row" class="bg-light fw-semibold">가입 IP</th>
                        <td>${dto.bip}</td>
                    </tr>
                </tbody>
            </table>

        </div>
    </div>

</div>

<%@include file="../inc/footer.jsp" %>
