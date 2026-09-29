package com.thejoa703.security;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 이메일 인증번호 / 인증완료 상태 저장소 (TokenStore 와 동일한 패턴)
 * - 인증번호(code)는 발송 후 code-exp-seconds(기본 5분) 동안만 유효
 * - 인증번호 확인에 성공하면 verified 플래그를 verified-exp-seconds(기본 30분) 동안 세워두고,
 *   이 시간 안에 회원가입(POST /auth/signup)을 완료해야 함
 */
@Component
@RequiredArgsConstructor
public class EmailVerificationStore {

    private final StringRedisTemplate stringRedisTemplate;

    // 인증번호 저장
    public void saveCode(String email, String code, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(codeKey(email), code, ttlSeconds, TimeUnit.SECONDS);
    }

    // 인증번호 조회
    public String getCode(String email) {
        return stringRedisTemplate.opsForValue().get(codeKey(email));
    }

    // 인증번호 삭제 - 확인 성공/실패 여부와 무관하게, 한 번 확인을 시도하면 재사용 못 하도록 즉시 삭제
    public void deleteCode(String email) {
        stringRedisTemplate.delete(codeKey(email));
    }

    // 인증완료 상태 저장
    public void markVerified(String email, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(verifiedKey(email), "true", ttlSeconds, TimeUnit.SECONDS);
    }

    // 인증완료 여부 확인
    public boolean isVerified(String email) {
        return "true".equals(stringRedisTemplate.opsForValue().get(verifiedKey(email)));
    }

    // 인증완료 상태 삭제 - 회원가입이 끝나면 더 이상 필요 없으므로 정리
    public void clearVerified(String email) {
        stringRedisTemplate.delete(verifiedKey(email));
    }

    private String codeKey(String email) {
        return "email-verify-code:" + email;
    }

    private String verifiedKey(String email) {
        return "email-verified:" + email;
    }
}
