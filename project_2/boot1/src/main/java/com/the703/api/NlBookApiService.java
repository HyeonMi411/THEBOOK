package com.the703.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.the703.dto.BookDto;

@Service
public class NlBookApiService {

    @Value("${nl.api.key}")
    private String apiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NlBookApiService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    /**
     * 국립중앙도서관 API 검색
     * @param keyword 검색어
     * @param page 페이지 번호
     * @return List<BookDto>
     */
    public List<BookDto> search(String keyword, int page) {
    	
        // 1) URL 생성 (자동 인코딩)
        URI uri = UriComponentsBuilder
                .fromUriString("https://www.nl.go.kr/NL/search/openApi/search.do")
                .queryParam("key", apiKey)
                .queryParam("apiType", "json")
                .queryParam("srchTarget", "total")
                .queryParam("kwd", keyword)
                .queryParam("pageSize", 10)
                .queryParam("pageNum", page)
                .build()
                .toUri();

        List<BookDto> result = new ArrayList<>();

        try {
            // 2) API 호출        	
            String responseBody = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
            
            // 3) JSON 파싱
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode docs = root.path("result").path("docs");

            for (JsonNode item : docs) {

                BookDto dto = new BookDto();

                dto.setTitle(item.path("title_info").asText());
                dto.setAuthor(item.path("author_info").asText());
                dto.setPublisher(item.path("pub_info").asText());
                dto.setPublishDate(item.path("pub_year_info").asText());
                dto.setCategory("국립중앙도서관");

                // ⭐ ISBN 추출
                dto.setIsbn(item.path("isbn").asText());

                result.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

}
