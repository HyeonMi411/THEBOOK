package com.thejoa703.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thejoa703.dto.UserDto.UserResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.security.JwtProperties;
import com.thejoa703.security.JwtProvider;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 모바일 앱 소셜 로그인 중계
 *
 * 1) 앱이 외부 브라우저로 /oauth2/authorization/{kakao|google|naver} 를 연다
 * 2) 로그인 성공 → OAuth2SuccessHandler 가 redirect-url(= /oauth2/app/callback?accessToken=...) 로 보내고
 *    같은 브라우저에 refreshToken 쿠키를 심는다
 * 3) 이 컨트롤러가 access/refresh 토큰을 Redis 에 60초짜리 "일회용 코드"로 맡겨두고,
 *    앱 딥링크(bookstore4://auth/callback?code=...)로 돌려보낸다 → 토큰 자체는 앱 URL 에 싣지 않음
 * 4) 앱이 POST /auth/app/exchange 로 코드를 토큰으로 바꾼다 (한 번 쓰면 즉시 삭제)
 *
 * 신규 소셜회원은 /oauth2/app/signup?signupToken=... → bookstore4://auth/social-signup?signupToken=...
 * (앱에서 닉네임 확인 + 이메일 인증 후 기존 POST /auth/social/signup 호출)
 */
@Tag(name = "App OAuth Bridge", description = "모바일 앱 소셜 로그인 중계")
@RestController
@RequiredArgsConstructor
public class AppOAuthBridgeController {

	private static final String CODE_PREFIX = "appauth:";
	private static final Duration CODE_TTL = Duration.ofSeconds(60);

	private final StringRedisTemplate redis;
	private final JwtProvider jwtProvider;
	private final JwtProperties props;
	private final AppUserRepository appUserRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${app.oauth2.app-scheme:bookstore4}")
	private String scheme;

	@GetMapping(value = "/oauth2/app/callback", produces = "text/html;charset=UTF-8")
	@ResponseBody
	public String callback(
			HttpServletRequest request,
			@RequestParam(name = "accessToken", required = false) String accessToken,
			@RequestParam(name = "error", required = false) String error,
			@RequestParam(name = "existingProvider", required = false) String existingProvider
	) throws Exception {
		if (error != null) {
			String target = scheme + "://auth/callback?error=" + enc(error)
					+ (existingProvider != null ? "&existingProvider=" + enc(existingProvider) : "");
			return bridgePage("로그인을 완료하지 못했습니다", target);
		}
		String refreshToken = readCookie(request, "refreshToken");
		if (accessToken == null || refreshToken == null) {
			return bridgePage("로그인 정보가 없습니다", scheme + "://auth/callback?error=missing_token");
		}
		// 위조된 토큰으로 코드를 만들 수 없도록 서명 검증 (실패하면 예외 → 에러 페이지)
		String userId;
		try {
			userId = jwtProvider.parse(accessToken).getBody().getSubject();
		} catch (Exception e) {
			return bridgePage("로그인 정보가 올바르지 않습니다", scheme + "://auth/callback?error=invalid_token");
		}
		Map<String, String> payload = new LinkedHashMap<>();
		payload.put("userId", userId);
		payload.put("accessToken", accessToken);
		payload.put("refreshToken", refreshToken);
		String code = UUID.randomUUID().toString().replace("-", "");
		redis.opsForValue().set(CODE_PREFIX + code, objectMapper.writeValueAsString(payload), CODE_TTL);
		return bridgePage("로그인 완료! 앱으로 돌아갑니다", scheme + "://auth/callback?code=" + code);
	}

	@GetMapping(value = "/oauth2/app/signup", produces = "text/html;charset=UTF-8")
	@ResponseBody
	public String signup(@RequestParam("signupToken") String signupToken) {
		return bridgePage("가입 확인을 위해 앱으로 돌아갑니다",
				scheme + "://auth/social-signup?signupToken=" + enc(signupToken));
	}

	@Operation(summary = "소셜 로그인 일회용 코드 → 토큰 교환 (앱 전용)",
			description = "응답은 일반 로그인(/auth/login)과 같은 형태이며 refreshToken 은 Set-Cookie 로 내려갑니다.")
	@PostMapping("/auth/app/exchange")
	public ResponseEntity<Map<String, Object>> exchange(
			@RequestBody Map<String, String> body,
			HttpServletRequest request,
			HttpServletResponse response
	) throws Exception {
		String code = body.get("code");
		if (code == null || !code.matches("^[a-f0-9]{32}$")) {
			throw new IllegalArgumentException("잘못된 로그인 코드입니다.");
		}
		String json = redis.opsForValue().getAndDelete(CODE_PREFIX + code);   // 일회용 - 조회와 동시에 삭제
		if (json == null) {
			throw new IllegalArgumentException("로그인 코드가 만료되었습니다. 다시 로그인해 주세요.");
		}
		@SuppressWarnings("unchecked")
		Map<String, String> payload = objectMapper.readValue(json, Map.class);
		Claims claims = jwtProvider.parse(payload.get("accessToken")).getBody();
		AppUser user = appUserRepository.findById(Long.parseLong(claims.getSubject()))
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

		boolean isLocal = request.getServerName().equals("localhost") || request.getServerName().equals("127.0.0.1");
		ResponseCookie cookie = ResponseCookie.from("refreshToken", payload.get("refreshToken"))
				.httpOnly(true)
				.secure(!isLocal)
				.sameSite("Lax")
				.path("/")
				.maxAge(props.getRefreshTokenExpSeconds())
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		return ResponseEntity.ok(Map.of(
				"accessToken", payload.get("accessToken"),
				"user", UserResponseDto.fromEntity(user)));
	}

	private String readCookie(HttpServletRequest request, String name) {
		if (request.getCookies() == null) {
			return null;
		}
		for (Cookie c : request.getCookies()) {
			if (name.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
				return c.getValue();
			}
		}
		return null;
	}

	private String enc(String v) {
		return URLEncoder.encode(v, StandardCharsets.UTF_8);
	}

	/** 브라우저에서 앱 딥링크로 넘겨주는 작은 페이지 (자동 이동 + 버튼) */
	private String bridgePage(String title, String target) {
		String safeTarget = target.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
		String jsTarget = target.replace("\\", "\\\\").replace("'", "\\'").replace("<", "\\u003c");
		return "<!DOCTYPE html><html lang=\"ko\"><head><meta charset=\"UTF-8\">"
				+ "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>BookStore</title>"
				+ "<style>body{font-family:sans-serif;padding:40px 20px;text-align:center;color:#222}"
				+ "a{display:inline-block;margin-top:18px;padding:12px 22px;background:#1565c0;color:#fff;border-radius:8px;text-decoration:none}</style>"
				+ "</head><body><h2>" + title + "</h2><p>자동으로 이동하지 않으면 아래 버튼을 눌러 주세요.</p>"
				+ "<a href=\"" + safeTarget + "\">앱으로 돌아가기</a>"
				+ "<script>location.replace('" + jsTarget + "');</script></body></html>";
	}
}
