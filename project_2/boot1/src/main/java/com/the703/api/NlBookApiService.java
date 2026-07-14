package com.the703.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@Service
public class NlBookApiService {

    @Value("${nl.api.key}")
    private String apiKey;

    private final RestClient restClient;
    private final XmlMapper xmlMapper = new XmlMapper(); // XML 파싱용

    public NlBookApiService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    /**
     * ⭐ 국립중앙도서관 API 검색 (XML)
     */
    public List<BookNlDto> search(String keyword, int page) {

        URI uri = UriComponentsBuilder
                .fromUriString("https://www.nl.go.kr/NL/search/openApi/search.do")
                .queryParam("key", apiKey)
                .queryParam("apiType", "xml")
                .queryParam("srchTarget", "total")
                .queryParam("kwd", keyword)
                .queryParam("pageSize", 10)
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

            // XML → JSON 변환
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

                // 상세조회 API를 사용하면 아래 필드도 채울 수 있음
                // dto.setSubject_info(item.path("subject_info").asText());
                // dto.setPage_info(item.path("page_info").asText());
                // dto.setPrice_info(item.path("price_info").asText());

                result.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
