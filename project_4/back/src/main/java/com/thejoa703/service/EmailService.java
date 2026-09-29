package com.thejoa703.service;

import java.security.SecureRandom;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원가입 이메일 인증번호 발송 서비스
 * application.yml 의 spring.mail 설정(Gmail SMTP)을 그대로 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    private static final SecureRandom RANDOM = new SecureRandom();

    // 6자리 숫자 인증번호 생성 - Math.random() 대신 SecureRandom 사용 (인증번호 추측 방지)
    public String generateVerificationCode() {
        int code = RANDOM.nextInt(900000) + 100000; // 100000 ~ 999999
        return String.valueOf(code);
    }

    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[BookStore] 이메일 인증번호");
        message.setText(
                "요청하신 이메일 인증번호는 아래와 같습니다.\n\n"
                        + code + "\n\n"
                        + "인증번호는 5분간 유효합니다.\n"
                        + "본인이 요청하지 않았다면 이 메일을 무시하세요."
        );
        try {
            mailSender.send(message);
        } catch (MailAuthenticationException e) {
            // Gmail 계정/앱 비밀번호가 틀렸거나 폐기됨 → 500 대신 원인을 알 수 있는 안내 (GlobalExceptionHandler 가 400 으로 응답)
            log.error("[메일] Gmail 로그인 실패 - GMAIL_USERNAME / GMAIL_APP_PASSWORD 를 확인하세요: {}", e.getMessage());
            throw new IllegalStateException("인증 메일을 보내지 못했습니다. (서버 메일 계정 설정 오류) 관리자에게 문의해 주세요.");
        } catch (MailException e) {
            // SMTP 서버 연결 실패(방화벽/네트워크), 받는 주소 오류 등
            log.error("[메일] 인증 메일 발송 실패 to={} : {}", toEmail, e.getMessage());
            throw new IllegalStateException("인증 메일을 보내지 못했습니다. 이메일 주소를 확인하거나 잠시 후 다시 시도해 주세요.");
        }
    }
}
