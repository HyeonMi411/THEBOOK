package com.thejoa703.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/**
 * 앱 관리자 통계 화면용 API.
 * 앱은 Django 주소나 동기화 토큰을 모르고, 평소처럼 JWT 로 이 API 만 호출한다.
 * 관리자인지 확인한 뒤 Spring Boot 가 X-Sync-Token 을 붙여 Django(/api/stats/)의 집계 결과를 대신 받아 전달.
 * (Django = pandas 분석 서버, Spring Boot = 인증·서비스 서버 로 역할 분리)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminStatsController {

	/** .env 의 DJANGO_SYNC_URL (예: http://localhost:8000/api/statistics/) 에서 Django 주소를 얻는다 */
	@Value("${DJANGO_SYNC_URL:}") private String djangoSyncUrl;
	@Value("${STATS_SYNC_TOKEN:}") private String syncToken;

	private final RestClient restClient;

	public AdminStatsController(RestClient.Builder builder) {
		this.restClient = builder.build();
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> stats() {
		if (djangoSyncUrl.isBlank() || syncToken.isBlank()) {
			return ResponseEntity.status(503)
					.body("{\"db_error\":\"DJANGO_SYNC_URL / STATS_SYNC_TOKEN 이 설정되지 않았습니다.\"}");
		}
		String statsUrl = djangoSyncUrl.replaceAll("/api/statistics/?$", "") + "/api/stats/";
		try {
			String body = restClient.get().uri(statsUrl)
					.header("X-Sync-Token", syncToken)
					.retrieve()
					.body(String.class);
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
		} catch (Exception e) {
			log.warn("Django 통계 조회 실패: {}", e.getMessage());
			return ResponseEntity.status(502)
					.body("{\"db_error\":\"통계 서버(Django)에 연결하지 못했습니다.\"}");
		}
	}
}
