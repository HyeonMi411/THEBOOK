package com.thejoa703.service;

import java.security.SecureRandom;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 회원가입 이메일 인증번호 발송 서비스
 * application.yml 의 spring.mail 설정(Gmail SMTP)을 그대로 사용
 */
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
        mailSender.send(message);
    }
}
