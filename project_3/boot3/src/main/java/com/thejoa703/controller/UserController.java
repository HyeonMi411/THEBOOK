package com.thejoa703.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.dto.LoginRequest;
import com.thejoa703.dto.UserDto.SignupPreviewDto;
import com.thejoa703.dto.UserDto.SocialSignupCompleteRequestDto;
import com.thejoa703.dto.UserDto.UserRequestDto;
import com.thejoa703.dto.UserDto.UserResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.security.JwtProperties;
import com.thejoa703.security.JwtProvider;
import com.thejoa703.security.TokenStore;
import com.thejoa703.service.UserService;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Tag( name="User Api" , description="회원 인증 및 관리 관련 API (Session & Swagger  지원)"  )   //swagger
@RestController       // @Controller + @ResponseBody
@RequestMapping("/auth")     // /api/users
@RequiredArgsConstructor 
public class UserController { 
    private final JwtProperties props;        	// 1. JWT 출입증 ( 설정값)    
    private final JwtProvider   jwtProvider;	// 2. JWT 토큰생성/검증 ( access Token / refresh Token)  
    private final TokenStore    tokenStore;	  	// 3. jwt 저장소
	private final UserService   userService;	//@Autowired

	// 카카오 REST API 키 - "카카오계정과 함께 로그아웃" URL을 서버에서 완성해서 내려주기
	// 위한 용도입니다. (REST API 키 자체는 공개돼도 되는 값이라 노출 문제 없음)
	@Value("${kakao.rest-api-key:}")
	private String kakaoRestApiKey;

	@Value("${app.frontend-base-url:http://localhost:3000}")
	private String frontendBaseUrl;
	
	// 사용자 등록 (회원가입)
	// 회원가입
	@Operation( summary="회원가입" , description = "새로운 사용자를 등록합니다.")
	@PostMapping(  value="/signup" , consumes= MediaType.MULTIPART_FORM_DATA_VALUE)   
	public    ResponseEntity<UserResponseDto>  createUser(
			@ModelAttribute UserRequestDto  request , // multipart/form-data
			@Parameter(description="프로필 이미지 파일")  // swagger
			@RequestPart(name="ufile" , required=false)  MultipartFile  ufile
	){  //return  ResponseEntity.status(HttpStatus.CREATED).body(response);   // HttpStatus.CREATED 201
		return   ResponseEntity.ok(   userService.createUser(request , ufile)    );
	}
	
	// 이메일중복확인 
	@Operation( summary="이메일 중복 확인" , description = "사용 중인 이메일인지 중복 여부를 확인합니다.")
	@GetMapping("/check-email") 
	public   ResponseEntity<Boolean>   checkEmail(
			@Parameter(description="확인할 이메일")  @RequestParam("email")  String email 
	){	
		return  ResponseEntity.ok(  userService.existsByEmail(email));
	}
	
	// 닉네임중복확인 
	@Operation( summary="닉네임 중복 확인" , description = "사용 중인 닉네임인지 중복 여부를 확인합니다.")
	@GetMapping("/check-nickname") 
	public   ResponseEntity<Boolean>   checkNickname(
			@Parameter(description="확인할 닉네임")  @RequestParam("nickname")  String nickname 
	){	
		return  ResponseEntity.ok(  userService.existsByNickname(nickname));
	}
	
	// 로그인
	//	@Operation( summary="로그인" , description = "이메일과 비밀번호로 로그인하여 세션을 생성합니다.")
	//	@PostMapping(value="/login"  , consumes = MediaType.APPLICATION_JSON_VALUE) 
	//	public   ResponseEntity<UserResponseDto>   login(
	//			@RequestBody LoginRequest request,
	//			HttpSession session   // jakarta.servlet.http.HttpSession
	//	){	
	//		//Long userId = (Long)session.getAttribute("LOGIN_USER_ID");
	//		//if( userId == null ) {  return  ResponseEntity.status(401).build();  }   // 권한없음.
	//		//return  ResponseEntity.ok(  userService.getUser(userId));  
	//		UserResponseDto  user	 =	userService.login(request);
	//		session.setAttribute("LOGIN_USER_ID", user.getId());  //세션셋팅
	//		return  ResponseEntity.ok(  user );
	//	}
	
    @Operation(summary = "로그인 (Access Token + Refresh Token 발급)")
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,	// 로컬(HTTP) 환경 여부 판별용
            HttpServletResponse response	// 응답객체( 쿠키설정 )   
    ) { // 1. 사용자인증처리
        UserResponseDto user = userService.login(request);
        // 2-1. Access Token 생성 ( 사용자 id + 역할)
        String accessToken = jwtProvider.createAccessToken(
                user.getId().toString(),
                Map.of("role", user.getRole())
        ); 
        // 2-2. Refresh Token -  room 아예빼기 ( checkout )
        String refreshToken = jwtProvider.createRefreshToken(user.getId().toString());
        // 2-3. redis 에 저장 
        tokenStore.saveRefreshToken(
                user.getId().toString(),
                refreshToken,
                (long) props.getRefreshTokenExpSeconds()
        );
        //3. 쿠키설정
        // org.springframework.http.ResponseCooke         
        // secure(true) 를 무조건 고정하면, http://localhost 같은 비HTTPS 로컬 개발환경에서는
        // 브라우저가 이 쿠키를 아예 서버로 전송하지 않습니다. 그러면 /auth/refresh 호출 시
        // 쿠키가 없어서 항상 실패하고, 프론트(axios 인터셉터)가 이를 "재로그인 필요"로
        // 판단해 강제 로그아웃시켜 버립니다. OAuth2SuccessHandler 와 동일하게 로컬環境
        // 여부를 판별해서 로컬에서는 secure=false 로 설정합니다.
        boolean isLocal = httpRequest.getServerName().equals("localhost") || httpRequest.getServerName().equals("127.0.0.1");
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)		// js 접근불가   
                .secure(!isLocal)  	   // 로컬(http)에서는 false, 배포환경(https)에서는 true
                .sameSite("Lax")  // "Strict" 는 카카오페이 결제처럼 외부(카카오) 도메인을 거쳐
                                  // 돌아오는 리다이렉트 흐름에서 쿠키가 누락될 수 있어 완화
                .path("/")   // 전체경로 적용
                .maxAge(props.getRefreshTokenExpSeconds())		// 만료시간설정  
                .build();
        // org.springframework.http.HttpHeaders         
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        //4. 사용자 정보 반환
        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "user", user
        ));
    }

    // 소셜로그인 "가입확인(추가정보 입력)" 화면에서, 처음 온 사용자에게 보여줄 기본정보 조회
    @Operation(
            summary = "소셜 가입확인용 미리보기 조회",
            description = "OAuth2SuccessHandler 가 신규 소셜회원에게 발급한 signupToken 을 검증하고, "
                    + "화면에 미리 채워줄 이메일/기본닉네임/프로필이미지를 돌려줍니다. DB에는 아직 저장하지 않습니다."
    )
    @GetMapping("/social/preview")
    public ResponseEntity<SignupPreviewDto> socialSignupPreview(
            @Parameter(description = "OAuth2SuccessHandler 가 발급한 가입확인용 임시토큰") @RequestParam(name = "signupToken") String signupToken
    ) {
        Claims claims = jwtProvider.parseSignupToken(signupToken).getBody();
        return ResponseEntity.ok(SignupPreviewDto.builder()
                .email(claims.get("email", String.class))
                .provider(claims.get("provider", String.class))
                .nicknameSuggestion(claims.get("nickname", String.class))
                .image(claims.get("image", String.class))
                .build());
    }

    // 소셜로그인 "가입확인(추가정보 입력)" 완료 - 사용자가 닉네임을 확인/수정하고 제출하면
    // 이 시점에 비로소 실제로 회원가입(DB저장)이 이루어지고, 곧바로 로그인 처리됩니다.
    @Operation(
            summary = "소셜 가입확인 완료 (실제 회원가입 + 로그인)",
            description = "signupToken 을 검증한 뒤, 사용자가 입력한 닉네임으로 최종 회원가입을 완료하고 "
                    + "일반 로그인과 동일하게 accessToken 발급 + refreshToken 쿠키 설정을 합니다."
    )
    @PostMapping("/social/signup")
    public ResponseEntity<Map<String, Object>> socialSignupComplete(
            @Valid @RequestBody SocialSignupCompleteRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        Claims claims = jwtProvider.parseSignupToken(request.getSignupToken()).getBody();
        String email      = claims.get("email", String.class);
        String provider   = claims.get("provider", String.class);
        String providerId = claims.get("providerId", String.class);
        String image      = claims.get("image", String.class);

        // 같은 signupToken 으로 중복 제출하는 경우(새로고침 등) 대비 - 이미 가입돼있으면 재가입 안함
        AppUser user = userService.findByEmailAndProvider(email, provider)
                .orElseGet(() -> userService.saveSocialUser(email, provider, providerId, request.getNickname(), image));

        // ↓↓↓ 여기부터는 login() 과 동일한 방식으로 JWT 발급 + 쿠키설정
        String accessToken = jwtProvider.createAccessToken(user.getId().toString(), Map.of(
                "nickname", user.getNickname(),
                "provider", user.getProvider(),
                "role", user.getRole(),
                "email", user.getEmail()
        ));
        String refreshToken = jwtProvider.createRefreshToken(user.getId().toString());
        tokenStore.saveRefreshToken(user.getId().toString(), refreshToken, (long) props.getRefreshTokenExpSeconds());

        boolean isLocal = httpRequest.getServerName().equals("localhost") || httpRequest.getServerName().equals("127.0.0.1");
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(!isLocal)
                .sameSite("Lax")
                .path("/")
                .maxAge(props.getRefreshTokenExpSeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "user", UserResponseDto.fromEntity(user)
        ));
    }
	
	
	// 로그아웃 
//	@Operation( summary="로그아웃" , description = "현재 세션을 만료시켜 로그아웃합니다.")
//	@PostMapping("/logout")
//	public    ResponseEntity<Void> logout(HttpSession session){
//		session.invalidate();
//		return ResponseEntity.noContent().build();
//	}
    
    //org.springframework.web.bind.annotation.CookieValue    
    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
          @CookieValue(name = "refreshToken", required = false) String refreshToken,
                                       HttpServletRequest httpRequest,
                                       HttpServletResponse response) {
        // refreshToken 쿠키가 없거나(required=false 라 null 가능) 이미 만료/변조되어
        // 파싱이 실패하더라도, 로그아웃 요청 자체는 항상 성공해야 합니다. 로그아웃의
        // 목적은 "클라이언트 상태를 지우는 것"이라, 서버측 Redis 토큰 삭제는
        // "가능하면 같이 해주는 것"이지 실패의 이유가 되면 안 됩니다.
        // (이 방어 코드가 없으면 refreshToken 이 없을 때 jwtProvider.parse(null) 이
        // 예외를 던지고, GlobalExceptionHandler 가 이를 400 으로 응답해서 로그아웃
        // 버튼을 눌러도 항상 실패하는 문제가 있었습니다 - provider 와 무관하게 전부
        // 여기서 막혔던 것입니다)
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                var claims = jwtProvider.parse(refreshToken).getBody();
                String userId = claims.getSubject();
                tokenStore.deleteRefreshToken(userId);	// redis 제거
            } catch (Exception e) {
                // 이미 만료됐거나 유효하지 않은 토큰이어도 로그아웃 자체는 계속 진행합니다.
            }
        }

        boolean isLocal = httpRequest.getServerName().equals("localhost") || httpRequest.getServerName().equals("127.0.0.1");
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(!isLocal)  // login() 과 동일한 이유로 로컬환경 여부에 따라 분기
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.noContent().build();
    }   
    
	
	// 마이페이지       
//	@Operation( summary="현재 로그인한 사용자 정보조회" , description = "세션기반으로 현재 로그인된 사용자의 정보를 조회")
//	@GetMapping("/me") 
//	public    ResponseEntity<UserResponseDto>  getUser( HttpSession session ){
//		
//		Long userId = (Long)session.getAttribute("LOGIN_USER_ID");
//		if( userId == null ) {  return  ResponseEntity.status(401).build();  }   // 권한없음.
//		return  ResponseEntity.ok(  userService.getUser(userId));  
//	} 
	    
    @Operation(summary = "현재 로그인한 사용자 정보 조회")
    @GetMapping("/me")					// jakarta.servlet.http.HttpServletRequest
    public ResponseEntity<UserResponseDto> me(HttpServletRequest request,
                 @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        try { 
        	// AUTHORIZATION 헤더에서 AccessToken 확인               
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);	// Bearer  제거
                var claims = jwtProvider.parse(token).getBody();  // 토큰 파싱
                String userId = claims.getSubject();  // 사용자 id 추출
                UserResponseDto user = userService.getUser(Long.valueOf(userId));	// 사용자 조회  
                return ResponseEntity.ok(user);	// 사용자 반환
            }  
            if (refreshToken != null) {
                var claims = jwtProvider.parse(refreshToken).getBody();
                String userId = claims.getSubject();	// 사용자 id추출
                UserResponseDto user = userService.getUser(Long.valueOf(userId));	// 사용자 조회  
                return ResponseEntity.ok(user);
            }
            return ResponseEntity.status(401).build();  //인증 실패 401
        } catch (Exception e) {
            return ResponseEntity.status(401).build();  //예외 발생시 인증실패
        }
    }
    	
	
	// 닉네임수정
	@Operation( summary="닉네임 변경" , description = "특정 사용자의 닉네임을 변경합니다.")
	@PatchMapping("/{userId}/nickname")
	public    ResponseEntity<UserResponseDto>  updateNickname(
			@Parameter(description = "사용자 ID")    @PathVariable("userId") Long userId ,
			@Parameter(description = "변경할 닉네임")  @RequestParam("nickname") String nickname
	){
		return ResponseEntity.ok(   userService.updateNickname(userId, nickname) );
	}
	
	// 이미지프로필수정
	@Operation( summary="프로필 이미지 업로드/교체" , description = "특정 사용자의 프로필이미지를 변경합니다.")
	@PatchMapping(value="/{userId}/profile-image" , consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public    ResponseEntity<UserResponseDto>  updateProfileImage(
			@Parameter(description = "사용자 ID")    @PathVariable("userId") Long userId ,
			@Parameter(description = "변경할 닉네임")  @RequestParam("ufile") MultipartFile ufile
	){
		return ResponseEntity.ok(   userService.updateProfileImage(userId, ufile) );
	}
	
	
	//	// 탈퇴
	//	@Operation( summary="회원 탈퇴" , description = "로그인된 사용자 계정을 삭제하고 세션을 만료시킵니다.")
	//	@DeleteMapping("/me")
	//	public    ResponseEntity<Void>  deleteMe(HttpSession session){
	//		Long userId = (Long)session.getAttribute("LOGIN_USER_ID");
	//		if( userId == null ) {  return  ResponseEntity.status(401).build();  }   // 권한없음.
	//		
	//		userService.deleteById(userId);
	//		session.invalidate();
	//		return  ResponseEntity.noContent().build();
	//	}
	
    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(HttpServletRequest request,
                                         HttpServletResponse response,
                                         @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        try { 
        	// AccessToken 확인
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).build();
            }  
            // AccessToken 추출
            String accessToken = authHeader.substring(7);	// (Bearer 공백)  빼고 키추출
            var claims = jwtProvider.parse(accessToken).getBody();
            String userId = claims.getSubject();
            // 해당유저삭제 
            userService.deleteById(Long.valueOf(userId));	//##
            // refreshToken삭제 
            if (refreshToken != null) {
                tokenStore.deleteRefreshToken(userId);
            } 
            
            // 쿠키삭제
            boolean isLocal = request.getServerName().equals("localhost") || request.getServerName().equals("127.0.0.1");
            ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(!isLocal)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }
   


    @Operation(summary = "Access Token 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@CookieValue("refreshToken") String refreshToken) {
        var claims = jwtProvider.parse(refreshToken).getBody();
        String userId = claims.getSubject();

        String stored = tokenStore.getRefreshToken(userId);
        if (stored == null || !stored.equals(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }

        String role = userService.findRoleByUserId(Long.valueOf(userId));	//####

        String newAccessToken = jwtProvider.createAccessToken(
                userId,
                Map.of("role", role)
        );

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }	

	// 소셜 로그아웃 - "카카오계정과 함께 로그아웃" URL을 완성해서 돌려줍니다.
	// 우리 서비스 로그아웃(POST /auth/logout)만으로는 우리 서비스의 JWT/쿠키만
	// 지워질 뿐, 브라우저에 남아있는 카카오/네이버 자체 로그인 세션은 그대로입니다.
	// 그래서 "카카오로 로그인" 버튼을 다시 누르면 비밀번호 확인 없이 곧바로 재로그인
	// 되어버려서 "로그아웃이 안 된 것처럼" 느껴집니다.
	// - 카카오 : 공식적으로 "카카오계정과 함께 로그아웃" 기능을 제공합니다
	// (GET https://kauth.kakao.com/oauth/logout?client_id=...&logout_redirect_uri=...)
	// → 이 URL로 이동하면 브라우저의 카카오계정 세션 자체가 만료됩니다.
	// - 네이버 : 이런 공식 OAuth 로그아웃 엔드포인트를 제공하지 않습니다(연동해제만 제공).
	// 그래서 네이버는 "완전 자동 로그아웃"이 불가능하다는 걸 그대로 응답에 담아
	// 프론트가 사용자에게 안내할 수 있게 합니다.
	@Operation(
			summary = "소셜 로그아웃 URL 조회",
			description = "provider 별로 브라우저에 남아있는 소셜 계정 세션까지 끊는 방법을 안내합니다. "
					+ "kakao 는 이동할 로그아웃 URL을, 그 외(google/naver 등)는 자동 로그아웃을 지원하지 않는다는 안내를 돌려줍니다."
	)
	@GetMapping("/social/logout-url")
	public ResponseEntity<Map<String, Object>> socialLogoutUrl(
			@Parameter(description = "소셜 제공자 (kakao/google/naver)") @RequestParam(name = "provider") String provider
	) {
		if ("kakao".equalsIgnoreCase(provider)) {
			String logoutRedirectUri = frontendBaseUrl + "/login";
			String url = "https://kauth.kakao.com/oauth/logout?client_id=" + kakaoRestApiKey
					+ "&logout_redirect_uri=" + logoutRedirectUri;
			return ResponseEntity.ok(Map.of("supported", true, "logoutUrl", url));
		}
		if ("naver".equalsIgnoreCase(provider)) {
			// 네이버는 카카오 같은 "공식" OAuth 로그아웃 엔드포인트를 제공하지 않습니다.
			// 다만 네이버 자체 웹사이트의 로그아웃 페이지(nidlogin.logout)로 이동시키면
			// 부수적으로 브라우저의 네이버 로그인 세션이 함께 만료되는 것으로 널리
			// 알려져 있습니다(비공식 - 네이버가 이 동작을 문서로 보장하지는 않습니다).
			String returnUrl = frontendBaseUrl + "/login";
			String url = "https://nid.naver.com/nidlogin.logout?returl=" + returnUrl;
			return ResponseEntity.ok(Map.of(
					"supported", true,
					"logoutUrl", url,
					"note", "네이버는 비공식 방식이라 100% 보장되지는 않습니다."
			));
		}
		// 구글은 accounts.google.com/Logout 이 있지만, 이건 Gmail 등 구글 전체
		// 서비스 로그인까지 함께 끊어버려서 사용자 경험상 권장되지 않습니다. 대신
		// application-oauth.yml 의 authorization-uri 에 prompt=select_account 를
		// 추가해서, 로그아웃 후 "구글로 로그인"을 다시 눌렀을 때 자동 재로그인되지
		// 않고 항상 계정 선택 화면이 뜨도록 이미 처리해뒀습니다.
		return ResponseEntity.ok(Map.of(
				"supported", false,
				"message", provider + " 은(는) 소셜 계정 자체를 자동으로 로그아웃할 수 있는 공식 방법을 제공하지 않습니다. "
						+ "완전히 로그아웃하시려면 " + provider + " 사이트에서 직접 로그아웃해주세요. "
						+ "(다음 로그인 시에는 재인증 화면이 뜨도록 이미 설정되어 있습니다)"
		));
	}

}
//
//1. User Api    - 사용자 관련 API
//- POST	/api/users		    회원가입        기능 : createUser
//- GET		/api/users/{id}		사용자 단건조회   기능 :  getUser