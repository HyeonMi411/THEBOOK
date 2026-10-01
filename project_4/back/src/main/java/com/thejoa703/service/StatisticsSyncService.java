package com.thejoa703.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thejoa703.entity.OrderStatus;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Boot → Django 대시보드 운영 지표 동기화
 *
 * flutter 수업(track011)의 StatisticsSyncService 는 게시글 전체 개수 하나만 보냈는데,
 * 여기서는 "오늘 하루" 운영 지표(가입/게시글/좋아요/댓글/결제/매출/챗봇 질문)를 모아서 보낸다.
 * Django 는 이 값을 자체 DB(ServiceLog)에 날짜별로 쌓아 추이 그래프를 그린다.
 *
 *  - 매일 23:50 자동 푸시 (DJANGO_SYNC_URL 이 비어 있으면 건너뜀)
 *  - Django 대시보드의 "지금 동기화" 버튼 → POST /api/statistics/sync (공유 토큰 필수)
 *
 * [수정] Map 을 그대로 body 로 넘기면 길이를 모르는 chunked 전송이 되어
 *        Django 개발 서버(runserver)가 본문을 읽지 못함(400 Bad request syntax).
 *        → JSON 을 byte[] 로 먼저 만들고 Content-Length 를 명시해서 보낸다.
 */
@Slf4j
@Service
public class StatisticsSyncService {

        private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

        @Value("${statistics.django-sync-url:}") private String djangoSyncUrl;
        @Value("${statistics.sync-token:}")      private String syncToken;

        private final EntityManager em;
        private final RestClient restClient;
        private final ObjectMapper objectMapper = new ObjectMapper();

        public StatisticsSyncService(EntityManager em, RestClient.Builder builder) {
                this.em = em;
                this.restClient = builder.build();
        }

        public boolean isValidToken(String token) {
                return syncToken != null && !syncToken.isBlank() && syncToken.equals(token);
        }

        /** 오늘(서울 기준) 운영 지표 */
        @Transactional(readOnly = true)
        public Map<String, Object> collectToday() {
                LocalDate today = LocalDate.now(SEOUL);
                LocalDateTime from = today.atStartOfDay();
                LocalDateTime to = from.plusDays(1);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("date", today.toString());
                m.put("newUsers", count("SELECT COUNT(u) FROM AppUser u WHERE u.createdAt >= :f AND u.createdAt < :t", from, to));
                m.put("newPosts", count("SELECT COUNT(p) FROM Post p WHERE p.createdAt >= :f AND p.createdAt < :t", from, to));
                m.put("likes", count("SELECT COUNT(l) FROM PostLike l WHERE l.createdAt >= :f AND l.createdAt < :t", from, to));
                m.put("comments", count("SELECT COUNT(c) FROM Comment c WHERE c.createdAt >= :f AND c.createdAt < :t", from, to));
                m.put("paidOrders", em.createQuery(
                                "SELECT COUNT(o) FROM Orders o WHERE o.orderStatus = :s AND o.approvedAt >= :f AND o.approvedAt < :t", Long.class)
                                .setParameter("s", OrderStatus.PAID).setParameter("f", from).setParameter("t", to).getSingleResult());
                Number sales = em.createQuery(
                                "SELECT SUM(o.totalAmount) FROM Orders o WHERE o.orderStatus = :s AND o.approvedAt >= :f AND o.approvedAt < :t", Number.class)
                                .setParameter("s", OrderStatus.PAID).setParameter("f", from).setParameter("t", to).getSingleResult();
                m.put("sales", sales == null ? 0L : sales.longValue());   // 결제가 없으면 SUM 은 null
                m.put("chatbotQuestions", count("SELECT COUNT(c) FROM ChatbotLog c WHERE c.createdAt >= :f AND c.createdAt < :t", from, to));
                m.put("chatbotUnanswered", em.createQuery(
                                "SELECT COUNT(c) FROM ChatbotLog c WHERE c.source = 'none' AND c.createdAt >= :f AND c.createdAt < :t", Long.class)
                                .setParameter("f", from).setParameter("t", to).getSingleResult());
                return m;
        }

        private long count(String jpql, LocalDateTime from, LocalDateTime to) {
                Long v = em.createQuery(jpql, Long.class).setParameter("f", from).setParameter("t", to).getSingleResult();
                return v == null ? 0L : v;
        }

        @Scheduled(cron = "0 50 23 * * *", zone = "Asia/Seoul")
        public void scheduledPush() {
                push();
        }

        /** @return 전송한 지표 (URL 미설정/실패 시에도 수집한 값은 돌려줌) */
        public Map<String, Object> push() {
                Map<String, Object> stats = collectToday();
                if (djangoSyncUrl == null || djangoSyncUrl.isBlank()) {
                        log.info("[통계] DJANGO_SYNC_URL 미설정 - 전송 생략 {}", stats);
                        return stats;
                }
                try {
                        // 길이를 알 수 있도록 JSON 을 먼저 byte[] 로 직렬화 (chunked 전송 방지)
                        byte[] json = objectMapper.writeValueAsBytes(stats);
                        restClient.post()
                                        .uri(djangoSyncUrl)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .contentLength(json.length)
                                        .header("X-Sync-Token", syncToken == null ? "" : syncToken)
                                        .body(json)
                                        .retrieve()
                                        .toBodilessEntity();
                        log.info("[통계] Django 전송 성공 {}", stats);
                } catch (Exception e) {
                        log.warn("[통계] Django 전송 실패: {}", e.getMessage());
                }
                return stats;
        }
}
