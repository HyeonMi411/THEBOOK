package com.thejoa703.controller;

import java.util.Map;

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
import com.thejoa703.dto.UserDto.UserRequestDto;
import com.thejoa703.dto.UserDto.UserResponseDto;
import com.thejoa703.security.JwtProperties;
import com.thejoa703.security.JwtProvider;
import com.thejoa703.security.TokenStore;
import com.thejoa703.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Tag( name="User Api" , description="회원 인증 및 관리 관련 API (Session & Swagger  지원)"  )   //swagger
@RestController       // @Controller + @ResponseBody
@RequestMapping("/auth")     //     /api/users
@RequiredArgsConstructor 
public class UserController { 
    private final JwtProperties props;        	// 1. JWT 출입증 ( 설정값)    
    private final JwtProvider   jwtProvider;	// 2. JWT 토큰생성/검증 ( access Token / refresh Token)  
    private final TokenStore    tokenStore;	  	// 3. jwt 저장소
	private final UserService   userService;	//@Autowired
	
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
            HttpServletRequest httpRequest,	// ★로컬(HTTP) 환경 여부 판별용
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
        //  org.springframework.http.ResponseCooke         
        // ★secure(true) 를 무조건 고정하면, http://localhost 같은 비HTTPS 로컬 개발환경에서는
        //   브라우저가 이 쿠키를 아예 서버로 전송하지 않습니다. 그러면 /auth/refresh 호출 시
        //   쿠키가 없어서 항상 실패하고, 프론트(axios 인터셉터)가 이를 "재로그인 필요"로
        //   판단해 강제 로그아웃시켜 버립니다. OAuth2SuccessHandler 와 동일하게 로컬環境
        //   여부를 판별해서 로컬에서는 secure=false 로 설정합니다.
        boolean isLocal = httpRequest.getServerName().equals("localhost") || httpRequest.getServerName().equals("127.0.0.1");
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)		// js 접근불가   
                .secure(!isLocal)  	   // ★로컬(http)에서는 false, 배포환경(https)에서는 true
                .sameSite("Lax")  // ★"Strict" 는 카카오페이 결제처럼 외부(카카오) 도메인을 거쳐
                                  //   돌아오는 리다이렉트 흐름에서 쿠키가 누락될 수 있어 완화
                .path("/")   // 전체경로 적용
                .maxAge(props.getRefreshTokenExpSeconds())		// 만료시간설정  
                .build();
        //  org.springframework.http.HttpHeaders         
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        //4. 사용자 정보 반환
        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "user", user
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
        var claims = jwtProvider.parse(refreshToken).getBody();
        String userId = claims.getSubject();

        tokenStore.deleteRefreshToken(userId);	// redis 제거
 
        boolean isLocal = httpRequest.getServerName().equals("localhost") || httpRequest.getServerName().equals("127.0.0.1");
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(!isLocal)  // ★login() 과 동일한 이유로 로컬환경 여부에 따라 분기
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
        	//  AUTHORIZATION 헤더에서 AccessToken 확인               
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

	
}
//
//1. User Api    - 사용자 관련 API
//- POST	/api/users		    회원가입       ※ 기능 : createUser
//- GET		/api/users/{id}		사용자 단건조회  ※ 기능 :  getUser