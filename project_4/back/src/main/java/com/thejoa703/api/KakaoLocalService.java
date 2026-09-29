package com.thejoa703.api;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thejoa703.dto.UtilDto.GeocodeDto;

import lombok.extern.slf4j.Slf4j;

/**
 * 카카오 로컬 API - 주소 → 좌표 변환
 * 주문서에서 우편번호 검색으로 고른 배송지를 지도(매장 지도 화면)에 표시할 때 사용.
 * 카카오 도서검색과 같은 REST API 키(kakao.rest-api-key)를 그대로 사용한다.
 */
@Slf4j
@Service
public class KakaoLocalService {

	@Value("${kakao.rest-api-key:}")
	private String restApiKey;

	private final RestClient restClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public KakaoLocalService(RestClient.Builder builder) {
		this.restClient = builder.build();
	}

	public GeocodeDto geocode(String address) {
		if (address == null || address.isBlank() || restApiKey == null || restApiKey.isBlank()) {
			return new GeocodeDto(false, address, null, null);
		}
		try {
			URI uri = UriComponentsBuilder.fromUriString("https://dapi.kakao.com/v2/local/search/address.json")
					.queryParam("query", address.trim())
					.encode()
					.build()
					.toUri();
			String body = restClient.get().uri(uri)
					.header("Authorization", "KakaoAK " + restApiKey)
					.retrieve()
					.body(String.class);
			JsonNode docs = objectMapper.readTree(body).path("documents");
			if (docs.isArray() && docs.size() > 0) {
				JsonNode first = docs.get(0);
				return new GeocodeDto(true, first.path("address_name").asText(address),
						first.path("y").asDouble(), first.path("x").asDouble());
			}
		} catch (Exception e) {
			log.warn("카카오 주소검색 실패: {}", e.getMessage());
		}
		return new GeocodeDto(false, address, null, null);
	}
}
