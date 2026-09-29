package com.thejoa703.api;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thejoa703.dto.UtilDto.OcrResultDto;

import lombok.extern.slf4j.Slf4j;

/**
 * 네이버 CLOVA OCR (General) - 책 표지 사진으로 도서 검색
 *
 * boot1 ApiOcrService 는 multipart 로 DTO 객체를 그대로 넣었는데, CLOVA 는 JSON 본문에
 * base64 이미지를 넣는 방식도 지원하므로 그 방식으로 단순화했다.
 * 인식된 글자 중 "글자 높이가 가장 큰 줄"(= 보통 책 제목)을 추천 검색어로 돌려준다.
 */
@Slf4j
@Service
public class ClovaOcrService {

	private static final long MAX_BYTES = 5L * 1024 * 1024;

	@Value("${naver.ocr.secret:}")     private String secret;
	@Value("${naver.ocr.invoke-url:}") private String invokeUrl;

	private final RestClient restClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ClovaOcrService(RestClient.Builder builder) {
		this.restClient = builder.build();
	}

	public boolean isConfigured() {
		return secret != null && !secret.isBlank() && invokeUrl != null && invokeUrl.startsWith("https://");
	}

	public OcrResultDto recognize(MultipartFile file) {
		if (!isConfigured()) {
			throw new IllegalStateException("표지 사진 검색(OCR) 설정이 되어 있지 않습니다. 관리자에게 문의해 주세요.");
		}
		String format = validateImage(file);
		try {
			Map<String, Object> body = Map.of(
					"version", "V2",
					"requestId", UUID.randomUUID().toString(),
					"timestamp", System.currentTimeMillis(),
					"lang", "ko",
					"images", List.of(Map.of(
							"format", format,
							"name", "book_cover",
							"data", Base64.getEncoder().encodeToString(file.getBytes()))));
			String response = restClient.post()
					.uri(invokeUrl)
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-OCR-SECRET", secret)
					.body(body)
					.retrieve()
					.body(String.class);
			return parse(response);
		} catch (IllegalArgumentException | IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			log.warn("CLOVA OCR 호출 실패: {}", e.getMessage());
			throw new IllegalStateException("사진에서 글자를 읽지 못했습니다. 잠시 후 다시 시도해 주세요.");
		}
	}

	private OcrResultDto parse(String response) throws Exception {
		JsonNode image = objectMapper.readTree(response).path("images").path(0);
		List<String> lines = new ArrayList<>();
		List<Double> heights = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		double heightSum = 0;
		int count = 0;
		for (JsonNode field : image.path("fields")) {
			String text = field.path("inferText").asText("");
			if (!text.isBlank()) {
				if (line.length() > 0) {
					line.append(' ');
				}
				line.append(text);
				heightSum += fieldHeight(field);
				count++;
			}
			if (field.path("lineBreak").asBoolean(false) && line.length() > 0) {
				lines.add(line.toString());
				heights.add(count == 0 ? 0 : heightSum / count);
				line.setLength(0);
				heightSum = 0;
				count = 0;
			}
		}
		if (line.length() > 0) {
			lines.add(line.toString());
			heights.add(count == 0 ? 0 : heightSum / count);
		}
		if (lines.isEmpty()) {
			throw new IllegalStateException("사진에서 글자를 찾지 못했습니다. 표지가 잘 보이게 다시 찍어 주세요.");
		}
		int best = 0;
		for (int i = 1; i < lines.size(); i++) {
			if (lines.get(i).replace(" ", "").length() >= 2 && heights.get(i) > heights.get(best)) {
				best = i;
			}
		}
		String keyword = lines.get(best).length() > 40 ? lines.get(best).substring(0, 40) : lines.get(best);
		return new OcrResultDto(lines, keyword);
	}

	private double fieldHeight(JsonNode field) {
		double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
		for (JsonNode v : field.path("boundingPoly").path("vertices")) {
			double y = v.path("y").asDouble();
			min = Math.min(min, y);
			max = Math.max(max, y);
		}
		return max > min ? max - min : 0;
	}

	/** 크기(5MB)와 실제 파일 시그니처(JPEG/PNG)를 확인 - 확장자만 바꾼 파일 차단 */
	private String validateImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("사진 파일을 선택해 주세요.");
		}
		if (file.getSize() > MAX_BYTES) {
			throw new IllegalArgumentException("사진은 5MB 이하만 올릴 수 있습니다.");
		}
		try {
			byte[] head = new byte[8];
			int read = file.getInputStream().read(head);
			if (read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
				return "jpg";
			}
			if (read >= 4 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
				return "png";
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("사진 파일을 읽을 수 없습니다.");
		}
		throw new IllegalArgumentException("JPG 또는 PNG 사진만 올릴 수 있습니다.");
	}
}
