package com.thejoa703.oauth2;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.thejoa703.entity.AppUser;
import com.thejoa703.security.JwtProperties;
import com.thejoa703.security.JwtProvider;
import com.thejoa703.security.TokenStore;
import com.thejoa703.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 로그인시 성공핸들러
 */

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;		// DB 저장/조회  
    private final JwtProvider jwtProvider;    // JWT 토큰 발급/검증
    private final TokenStore tokenStore;    // REDIS - JWT 저장소
    private final JwtProperties props;    // JWT 토큰
 
    @Value("${app.oauth2.redirect-url}")
    private String redirectUrl;  // access Token을 react로 리다이렉트하면서 전달

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attrs = oAuth2User.getAttributes();
        
        // 공급자 식별 (google, kakao, naver) 
        String registrationId = ((OAuth2AuthenticationToken) authentication)
                .getAuthorizedClientRegistrationId();
        
        // 공급자 사용자 정보 매핑 (google, kakao, naver)         
        UserInfoOAuth2 userInfo;
        switch (registrationId) {
            case "google": userInfo = new UserInfoGoogle(attrs); break;
            case "kakao":  userInfo = new UserInfoKakao(attrs); break;
            case "naver":  userInfo = new UserInfoNaver(attrs); break;
            default: throw new IllegalArgumentException("지원하지 않는 Provider: " + registrationId);
        }
        
        // Step1) db조회 / 저장
        AppUser user = userService.findByEmailAndProvider(userInfo.getEmail(), userInfo.getProvider())
                .orElseGet(() -> userService.saveSocialUser(
                        userInfo.getEmail(),
                        userInfo.getProvider(),
                        userInfo.getProviderId(),
                        userInfo.getNickname(),
                        userInfo.getImage()
                ));
        
        // Step2) JWT 토큰발급 
        String access = jwtProvider.createAccessToken(user.getId().toString(), Map.of(
                "nickname", user.getNickname(),
                "provider", user.getProvider(),
                "role", user.getRole(),
                "email", user.getEmail()
        ));
        String refresh = jwtProvider.createRefreshToken(user.getId().toString());
        // redis저장          
        tokenStore.saveRefreshToken(
                user.getId().toString(),
                refresh,
                (long) props.getRefreshTokenExpSeconds()
        );
  
        // Step3) RefreshToken  을 쿠키로 설정 
        Cookie refreshCookie = new Cookie("refreshToken", refresh);
        refreshCookie.setHttpOnly(true);
        boolean isLocal = request.getServerName().equals("localhost") || request.getServerName().equals("127.0.0.1");
        refreshCookie.setSecure(!isLocal);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) props.getRefreshTokenExpSeconds());
        response.addCookie(refreshCookie);
        // ★jakarta.servlet.http.Cookie 는 SameSite 속성을 직접 지원하지 않습니다.
        //   UserController(일반 로그인)의 refreshToken 쿠키와 SameSite 정책을 동일하게
        //   맞추기 위해, 응답 헤더에 SameSite=Lax 를 직접 덧붙입니다. ("Strict" 로 하면
        //   카카오페이 결제처럼 외부(카카오) 도메인을 거쳐 돌아오는 리다이렉트 흐름에서
        //   쿠키가 누락될 수 있어 완화했습니다)
        response.setHeader("Set-Cookie", response.getHeader("Set-Cookie") + "; SameSite=Lax");
        //
		// Step4) redirectUrl ( 리액트경로 )  accessToken= 전달         
        String targetUrl = redirectUrl + "?accessToken=" + access;
        response.sendRedirect(targetUrl);
    }
}



