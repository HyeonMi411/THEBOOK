package com.thejoa703.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 국립중앙도서관 도서검색 API 연동 서비스
 * boot1(the703) 의 api/NlBookApiService.java 를 그대로 재현했습니다.
 * (국립중앙도서관 오픈API 는 XML 로 응답합니다)
 */
@Slf4j
@Service
public class NlBookApiService {

    @Value("${nl.api.key}")
    private String apiKey;

    private final RestClient restClient;
    private final XmlMapper xmlMapper = new XmlMapper(); // XML 파싱용

    public NlBookApiService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    /** ⭐ 국립중앙도서관 API 검색 (키워드 또는 KDC 분류명으로 검색, XML 응답) */
    public List<BookNlDto> search(String keyword, int page) {

        URI uri = UriComponentsBuilder
                .fromUriString("https://www.nl.go.kr/NL/search/openApi/search.do")
                .queryParam("key", apiKey)
                .queryParam("apiType", "xml")
                .queryParam("srchTarget", "total")
                .queryParam("kwd", keyword)
                .queryParam("pageSize", 12)   // 화면에 12개씩 노출과 통일
                .queryParam("pageNum", page)
                .build()
                .toUri();

        List<BookNlDto> result = new ArrayList<>();

        try {
            // API 호출
            String responseBody = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            // XML → JSON 트리로 변환
            JsonNode root = xmlMapper.readTree(responseBody);
            JsonNode items = root.path("result").path("item");

            for (JsonNode item : items) {
                BookNlDto dto = new BookNlDto();

                dto.setTitle_info(item.path("title_info").asText());
                dto.setAuthor_info(item.path("author_info").asText());
                dto.setPub_info(item.path("pub_info").asText());
                dto.setPub_year_info(item.path("pub_year_info").asText());
                dto.setIsbn(item.path("isbn").asText());
                dto.setId(item.path("id").asText());
                dto.setImage_url(item.path("image_url").asText());
                dto.setReg_date(item.path("reg_date").asText());
                dto.setKdc_name_1s(item.path("kdc_name_1s").asText());

                result.add(dto);
            }

        } catch (Exception e) {
            log.warn("국립중앙도서관 도서검색 API 호출/파싱 중 오류가 발생했습니다: {}", e.getMessage());
        }

        return result;
    }
}
