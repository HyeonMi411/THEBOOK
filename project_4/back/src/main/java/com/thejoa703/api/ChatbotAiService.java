package com.thejoa703.api;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 상담 챗봇의 ChatGPT 보조 답변 (boot1 ApiOpenAi 방식의 RestClient 호출)
 *
 * 실제 서비스 챗봇처럼 "정해진 FAQ 가 먼저, AI 는 보조" 구조:
 *  - FAQ 로 답을 찾으면 AI 를 호출하지 않는다 (비용 0, 답변 일관성 유지)
 *  - FAQ 로 못 찾은 질문만, FAQ 전체를 근거자료로 넣어서 짧게 답하게 한다
 *  - 근거자료에 없는 내용은 지어내지 말고 상담원 문의를 안내하도록 지시
 *  - 한 사용자(IP)당 1시간 호출 횟수 제한 (Redis 카운터) → 과금 폭주 방지
 *  - 키가 없거나 실패하면 null → 기존처럼 "상담원 문의" 안내
 */
@Slf4j
@Service
public class ChatbotAiService {

	private static final String API_URL = "https://api.openai.com/v1/chat/completions";

	@Value("${openai.api.key:}")        private String apiKey;
	@Value("${openai.api.model:gpt-4o-mini}") private String model;
	@Value("${chatbot.ai-hourly-limit:20}") private int hourlyLimit;

	private final RestClient restClient;
	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ChatbotAiService(RestClient.Builder builder, StringRedisTemplate redis) {
		this.restClient = builder.build();
		this.redis = redis;
	}

	public boolean isConfigured() {
		return apiKey != null && !apiKey.isBlank();
	}

	/** @return AI 답변, 사용할 수 없으면 null */
	public String answer(String question, String knowledge, String clientKey) {
		if (!isConfigured() || !withinLimit(clientKey)) {
			return null;
		}
		String system = "너는 온라인 서점 앱 'BookStore' 의 고객상담 챗봇이다.\n"
				+ "아래 [근거자료]에 있는 내용만 사용해서 한국어 존댓말로 3문장 이내로 답해라.\n"
				+ "근거자료에 없는 기능·정책·가격·날짜는 절대 지어내지 말고 '정확한 안내를 위해 상담원 문의를 이용해 주세요.'라고 답해라.\n"
				+ "개인정보(주민번호, 카드번호, 비밀번호)는 절대 요구하지 마라. 도서 추천 요청에는 앱 기능 안내 범위에서만 답해라.\n\n"
				+ "[근거자료]\n" + knowledge;
		try {
			Map<String, Object> body = Map.of(
					"model", model,
					"max_tokens", 300,
					"temperature", 0.2,
					"messages", List.of(
							Map.of("role", "system", "content", system),
							Map.of("role", "user", "content", question)));
			String response = restClient.post()
					.uri(API_URL)
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + apiKey)
					.body(body)
					.retrieve()
					.body(String.class);
			JsonNode root = objectMapper.readTree(response);
			String text = root.path("choices").path(0).path("message").path("content").asText("").trim();
			return text.isEmpty() ? null : text;
		} catch (Exception e) {
			log.warn("[챗봇 AI] OpenAI 호출 실패: {}", e.getMessage());
			return null;
		}
	}

	private boolean withinLimit(String clientKey) {
		try {
			String key = "chatbot:ai:" + (clientKey == null ? "unknown" : clientKey);
			Long count = redis.opsForValue().increment(key);
			if (count != null && count == 1L) {
				redis.expire(key, Duration.ofHours(1));
			}
			return count != null && count <= hourlyLimit;
		} catch (Exception e) {
			log.warn("[챗봇 AI] 호출 제한 확인 실패 - AI 답변을 건너뜁니다: {}", e.getMessage());
			return false;   // Redis 장애 시 안전하게 AI 미사용
		}
	}
}
