package com.thejoa703.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableScheduling : 도서 뉴스 크롤링(30분), Django 통계 동기화(매일 23:50) 스케줄러 (boot1 ApiScheduledTask 방식)
 * @EnableAsync      : 결제완료 이메일/문자 발송을 결제 응답과 분리 (외부 API 가 느려도 결제 화면이 늦어지지 않도록)
 */
@Configuration
@EnableScheduling
@EnableAsync
public class AsyncSchedulingConfig {
}
