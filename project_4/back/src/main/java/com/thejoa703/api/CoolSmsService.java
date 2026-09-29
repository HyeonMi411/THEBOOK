package com.thejoa703.api;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/**
 * 문자(SMS) 발송 - CoolSMS(솔라피) REST API v4
 *
 * boot1 수업에서는 net.nurigo:javaSDK 2.2 를 의존성에 넣었는데, 이 SDK 는 오래된 버전이라
 * 공식 REST API 를 RestClient 로 직접 호출한다. (HMAC-SHA256 서명: date + salt 를 API Secret 으로 서명)
 * 키/발신번호가 없으면 발송하지 않고 로그만 남긴다. → 로컬 개발에서도 결제 흐름은 그대로 동작.
 */
@Slf4j
@Service
public class CoolSmsService {

	private static final String SEND_URL = "https://api.coolsms.co.kr/messages/v4/send";

	@Value("${sms.api-key:}")    private String apiKey;
	@Value("${sms.api-secret:}") private String apiSecret;
	@Value("${sms.from:}")       private String from;

	private final RestClient restClient;

	public CoolSmsService(RestClient.Builder builder) {
		this.restClient = builder.build();
	}

	public boolean isConfigured() {
		return !apiKey.isBlank() && !apiSecret.isBlank() && !from.isBlank();
	}

	/** @return 실제로 발송 요청을 보냈으면 true */
	public boolean send(String to, String text) {
		if (!isConfigured()) {
			log.info("[SMS] CoolSMS 키가 없어 문자 발송을 건너뜁니다. to={}", mask(to));
			return false;
		}
		if (to == null || !to.matches("^01\\d{8,9}$")) {
			log.warn("[SMS] 수신번호 형식이 올바르지 않아 발송하지 않습니다. to={}", mask(to));
			return false;
		}
		try {
			restClient.post()
					.uri(SEND_URL)
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", authorization())
					.body(Map.of("message", Map.of(
							"to", to,
							"from", from.replaceAll("[^0-9]", ""),
							"text", text)))
					.retrieve()
					.toBodilessEntity();
			log.info("[SMS] 발송 요청 완료 to={}", mask(to));
			return true;
		} catch (Exception e) {
			log.warn("[SMS] 발송 실패 to={} : {}", mask(to), e.getMessage());
			return false;
		}
	}

	private String authorization() throws Exception {
		String date = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		String salt = UUID.randomUUID().toString().replace("-", "");
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		String signature = HexFormat.of().formatHex(mac.doFinal((date + salt).getBytes(StandardCharsets.UTF_8)));
		return "HMAC-SHA256 apiKey=" + apiKey + ", date=" + date + ", salt=" + salt + ", signature=" + signature;
	}

	/** 로그에 전화번호 전체가 남지 않도록 가운데를 가림 */
	private String mask(String phone) {
		if (phone == null || phone.length() < 8) {
			return "(없음)";
		}
		return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
	}
}
