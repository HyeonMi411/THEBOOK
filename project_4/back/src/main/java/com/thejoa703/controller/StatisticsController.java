package com.thejoa703.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thejoa703.service.StatisticsSyncService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Django 대시보드의 "지금 동기화" 요청 수신 (flutter 수업 StatisticsController 와 같은 역할)
 * JWT 대신 서버끼리만 아는 공유 토큰(X-Sync-Token = STATS_SYNC_TOKEN)으로 보호한다.
 */
@Tag(name = "Statistics Api", description = "Django 대시보드 통계 동기화 (서버 간 통신 전용)")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

	private final StatisticsSyncService statisticsSyncService;

	@Operation(summary = "오늘 운영 지표를 Django 로 전송", description = "헤더 X-Sync-Token 이 서버의 STATS_SYNC_TOKEN 과 같아야 합니다.")
	@PostMapping("/sync")
	public ResponseEntity<Map<String, Object>> sync(@RequestHeader(name = "X-Sync-Token", required = false) String token) {
		if (!statisticsSyncService.isValidToken(token)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "동기화 토큰이 올바르지 않습니다."));
		}
		return ResponseEntity.ok(statisticsSyncService.push());
	}
}
