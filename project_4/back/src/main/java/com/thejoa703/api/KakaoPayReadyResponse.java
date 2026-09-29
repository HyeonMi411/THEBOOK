package com.thejoa703.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * 카카오페이 결제준비(ready) API 응답을 담는 Dto
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoPayReadyResponse {
    private String tid;                     // 결제 고유번호 (승인/조회/취소 요청시 필요)
    private String next_redirect_pc_url;    // PC 웹 결제화면 URL
    private String next_redirect_mobile_url;// 모바일 웹 결제화면 URL
    private String next_redirect_app_url;   // 앱 결제화면 URL
    private String created_at;              // 결제 준비 요청 시각
}
