package com.thejoa703.security;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
 
//2. 토큰 발급/검증
@Component
public class JwtProvider {
    private final JwtProperties props;		//토큰-출입증  
    private final SecretKey key;   // JWT에 서명에 사용할  key
    
    // 생성자 
    public JwtProvider(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes());	//서명용키를 생성  
    }
    
    // AccessToken 생성  
    public String createAccessToken(String subject, Map<String, Object> claims) {  
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getAccessTokenExpSeconds());
        return Jwts.builder()
                .setIssuer(props.getIssuer())  // 발급자
                .setSubject(subject)     // 사용자
                .addClaims(claims)       // 추가정보 
                .setIssuedAt(Date.from(now))   // 발급시간
                .setExpiration(Date.from(exp))  // 만료시간
                .signWith(key, SignatureAlgorithm.HS256)   // HS256 알고리즘 서명
                .compact();
    }    
    // RefreshToken 생성     
    public String createRefreshToken(String subject) {   
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getRefreshTokenExpSeconds());	// 만료시간 더길게   
        return Jwts.builder()
                .setIssuer(props.getIssuer())
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    } 

    // 소셜로그인 "가입확인(추가정보 입력)" 임시토큰 생성
    // 신규 소셜회원은 인증만 통과했을 뿐, 아직 우리 서비스 회원으로 정식 가입된 게
    // 아닙니다. 이 토큰에 이메일/제공자/닉네임(기본값)/프로필이미지를 담아뒀다가,
    // 사용자가 닉네임을 확인/수정하고 "가입완료"를 눌렀을 때만 실제 DB에 저장합니다.
    // accessToken 보다 훨씬 짧게(10분) 만료시켜서, 그 사이에 완료 안 하면 다시
    // 소셜로그인부터 시작해야 하도록 합니다.
    public String createSignupToken(Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(600); // 10분
        return Jwts.builder()
                .setIssuer(props.getIssuer())
                .setSubject("SOCIAL_SIGNUP") // 일반 accessToken과 용도가 다름을 구분하는 subject
                .addClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰 파싱과 검증
    public Jws<Claims> parse(String token) {	// JWT문자열  
        return Jwts.parserBuilder()
                .setSigningKey(key)   // 서명용키를 이용해서 토큰 검증
                .requireIssuer(props.getIssuer())   //발급자와 일치하는지 확인
                .build()
                .parseClaimsJws(token);
    }

    // 가입확인 임시토큰 전용 파싱 - subject 가 "SOCIAL_SIGNUP" 인지까지 검증해서,
    // 일반 accessToken/refreshToken 을 이 API에 잘못 넣는 것을 막습니다.
    public Jws<Claims> parseSignupToken(String token) {
        Jws<Claims> jws = parse(token);
        if (!"SOCIAL_SIGNUP".equals(jws.getBody().getSubject())) {
            throw new io.jsonwebtoken.JwtException("가입확인용 토큰이 아닙니다.");
        }
        return jws;
    }
}
