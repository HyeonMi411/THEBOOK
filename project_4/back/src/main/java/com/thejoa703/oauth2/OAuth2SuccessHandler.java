package com.thejoa703.oauth2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
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

    @Value("${app.oauth2.signup-confirm-url:}")
    private String signupConfirmUrlProp; // 신규 소셜회원 "가입확인(추가정보 입력)" 화면 주소

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

        // 기존 회원인지 먼저 확인 (DB에 저장하지 않고 조회만)
        var existingUser = userService.findByEmailAndProvider(userInfo.getEmail(), userInfo.getProvider());

        if (existingUser.isEmpty()) {
            // provider 까지 같은 계정은 없지만, 같은 이메일로 다른 방법(local 회원가입 또는
            // 다른 소셜 provider)으로 이미 가입되어 있을 수 있음. 이 확인 없이 바로
            // 신규가입시키면, 한 사람이 같은 이메일로 계정을 여러 개 만들게 되어 "왜
            // 로그인이 안 되지" 하는 혼란을 겪게 됨. 신규가입 전에 반드시 이메일
            // 소유 여부부터 확인(회원인증) 필요.
            var sameEmailUser = userService.findByEmail(userInfo.getEmail());
            if (sameEmailUser.isPresent()) {
                String existingProvider = sameEmailUser.get().getProvider();
                String encodedEmail = URLEncoder.encode(userInfo.getEmail(), StandardCharsets.UTF_8);
                response.sendRedirect(redirectUrl
                        + "?error=email_already_exists"
                        + "&existingProvider=" + existingProvider
                        + "&email=" + encodedEmail);
                return;
            }

            // 신규 소셜회원 - 여기서 곧바로 회원가입시키지 않음.
            // "가입확인(닉네임 확인/수정)" 화면으로 먼저 보내고, 사용자가 그 화면에서
            // 확인을 완료해야만(POST /auth/social/signup) 실제로 DB에 저장.
            String signupToken = jwtProvider.createSignupToken(Map.of(
                    "email", userInfo.getEmail(),
                    "provider", userInfo.getProvider(),
                    "providerId", userInfo.getProviderId(),
                    "nickname", userInfo.getNickname() != null ? userInfo.getNickname() : "",
                    "image", userInfo.getImage() != null ? userInfo.getImage() : ""
            ));
            String signupConfirmUrl = (signupConfirmUrlProp != null && !signupConfirmUrlProp.isBlank())
                    ? signupConfirmUrlProp
                    : redirectUrl.replace("/oauth2/callback", "/oauth2/signup"); // 기본값 유추
            response.sendRedirect(signupConfirmUrl + "?signupToken=" + signupToken);
            return;
        }

        AppUser user = existingUser.get();

        // 탈퇴한 계정으로는 로그인을 허용하지 않음 - 일반 로그인(UserService.login())과
        // 동일한 정책. 여기서 막지 않으면 소셜로그인만으로 탈퇴 계정이 되살아나 버림.
        if (Boolean.TRUE.equals(user.getDeleted())) {
            response.sendRedirect(redirectUrl + "?error=account_deleted");
            return;
        }
        
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
        // jakarta.servlet.http.Cookie 는 SameSite 속성을 직접 미지원.
        // UserController(일반 로그인)의 refreshToken 쿠키와 SameSite 정책을 동일하게
        // 맞추기 위해, 응답 헤더에 SameSite=Lax 를 직접 덧붙임. ("Strict" 로 하면
        // 카카오페이 결제처럼 외부(카카오) 도메인을 거쳐 돌아오는 리다이렉트 흐름에서
        // 쿠키가 누락될 수 있어 완화)
        response.setHeader("Set-Cookie", response.getHeader("Set-Cookie") + "; SameSite=Lax");
        //
		// Step4) redirectUrl ( 리액트경로 )  accessToken= 전달         
        String targetUrl = redirectUrl + "?accessToken=" + access;
        response.sendRedirect(targetUrl);
    }
}



