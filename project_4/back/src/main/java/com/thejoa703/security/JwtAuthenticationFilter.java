package com.thejoa703.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.thejoa703.oauth2.CustomOAuth2User;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

// 보안게이트
/**
 * JWT 인증필터
 * - Authorization 헤더에서 Bearer 토큰추출
 * - JwtProvider로 Claims 파싱
 * - CustomOAuth2User 기반 Principal 생성 후 SecurityContext 에 저장
*/
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	// JWT 토큰 발급/검증
    private final JwtProvider jwtProvider;
    // 생성자
    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }
    // /uploads/ 로 시작하는 요청은 JWT 필터 타지 않게 통과
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/uploads/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7); // "Bearer " 7자 제외
            try {
                Claims claims = jwtProvider.parse(token).getBody();
                Long userId = Long.parseLong(claims.getSubject());
                String role = claims.get("role", String.class);

                CustomOAuth2User userPrincipal = new CustomOAuth2User(userId, role);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userPrincipal, null, userPrincipal.getAuthorities()
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                // 토큰 원문/사용자 식별정보는 로그에 남기지 않음(탈취 위험). 실패 사유만
                // DEBUG 레벨로 남겨서, 운영환경에서는 기본적으로 조용하고 필요할 때만
                // 로그 레벨을 낮춰서 확인 가능.
                log.debug("JWT 인증 실패: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        // Authorization 헤더가 없는 요청은 로그인이 필요 없는 공개 API(도서 조회 등)일 수
        // 있는 정상적인 상황이므로, 별도 로그를 남기지 않고 조용히 다음 필터로 넘어갑니다.

        chain.doFilter(request, response);
    }
}
