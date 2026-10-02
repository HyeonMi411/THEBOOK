package com.thejoa703.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thejoa703.dto.UtilDto.ExternalBookDto;

import lombok.extern.slf4j.Slf4j;

/**
 * 네이버 책 검색 API (openapi.naver.com/v1/search/book.json) 
 * boot1 ApiNaverBook 에서 setTitle() 을 세 번 호출해 저자/표지가 제목을 덮어쓰던 버그를 고쳐서 가져옴.
 */
@Slf4j
@Service
public class ApiNaverBook {

	@Value("${naver.book.client-id:}")     private String clientId;
	@Value("${naver.book.client-secret:}") private String clientSecret;

	private final RestClient restClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ApiNaverBook(RestClient.Builder builder) {
		this.restClient = builder.build();
	}

	public boolean isConfigured() {
		return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
	}

	public List<ExternalBookDto> search(String query) {
		List<ExternalBookDto> result = new ArrayList<>();
		if (!isConfigured()) {
			return result;
		}
		try {
			URI uri = UriComponentsBuilder.fromUriString("https://openapi.naver.com/v1/search/book.json")
					.queryParam("query", query)
					.queryParam("display", 20)
					.encode()
					.build()
					.toUri();
			String body = restClient.get().uri(uri)
					.header("X-Naver-Client-Id", clientId)
					.header("X-Naver-Client-Secret", clientSecret)
					.retrieve()
					.body(String.class);
			for (JsonNode item : objectMapper.readTree(body).path("items")) {
				ExternalBookDto dto = new ExternalBookDto();
				dto.setSource("naver");
				dto.setTitle(stripTags(item.path("title").asText()));
				dto.setAuthors(stripTags(item.path("author").asText()).replace("^", ", "));
				dto.setPublisher(stripTags(item.path("publisher").asText()));
				dto.setPublishDate(toIsoDate(item.path("pubdate").asText()));
				dto.setIsbn(item.path("isbn").asText());
				dto.setThumbnail(item.path("image").asText());
				dto.setDescription(stripTags(item.path("description").asText()));
				String discount = item.path("discount").asText("");
				dto.setPrice(discount.matches("\\d+") ? Integer.parseInt(discount) : null);
				dto.setLink(item.path("link").asText());
				result.add(dto);
			}
		} catch (Exception e) {
			log.warn("네이버 책 검색 실패: {}", e.getMessage());
		}
		return result;
	}

	private String stripTags(String s) {
		return s == null ? "" : s.replaceAll("<[^>]*>", "").trim();
	}

	/** 20240115 → 2024-01-15 */
	private String toIsoDate(String yyyymmdd) {
		if (yyyymmdd != null && yyyymmdd.matches("\\d{8}")) {
			return yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8);
		}
		return yyyymmdd;
	}
}
