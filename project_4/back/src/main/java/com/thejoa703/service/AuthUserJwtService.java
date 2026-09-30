package com.thejoa703.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.thejoa703.oauth2.CustomOAuth2User;

/**
 * JWT 인증 사용자 정보서비스
 * - Authentication 에서  CustomOAuth2User 에서 꺼내서 현재 로그인한 사용자 정보를 제공
 * */ 
@Component
public class AuthUserJwtService {

    /**
     * 토큰이 없거나 만료되어 로그인 정보가 없으면 401 로 응답하도록 인증 예외를 던진다.
     * (/auth/** 처럼 공개 경로에 있는 "본인 전용" API 에서 NullPointerException → 500 이 나던 문제 방지)
     */
    private CustomOAuth2User principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User user)) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }
        return user;
    }
	// 현재 로그인한 사용자 ID반환
    public Long getCurrentUserId(Authentication authentication) {
        CustomOAuth2User userPrincipal = principal(authentication);
        return userPrincipal.getId();
    }
    // 현재 로그인한 사용자 EMAIL반환 
    public String getCurrentUserEmail(Authentication authentication) {
        CustomOAuth2User userPrincipal = principal(authentication);
        return userPrincipal.getEmail();
    }
    // 현재 로그인한 사용자 닉네임반환  
    public String getCurrentUserNickname(Authentication authentication) {
        CustomOAuth2User userPrincipal = principal(authentication);
        return userPrincipal.getNickname();
    }
}
