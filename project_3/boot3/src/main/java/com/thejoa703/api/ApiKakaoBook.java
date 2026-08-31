package com.thejoa703.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 카카오 도서검색 API 연동 서비스
 * boot1(the703) 의 api/ApiKakaoBook.java 를 그대로 재현했습니다.
 * (카카오 개발자센터의 REST API 키 하나로 로그인/도서검색 둘 다 사용 가능합니다.
 *  이미 소셜로그인에 등록해두신 KAKAO_CLIENT_ID 를 재사용하도록 기본값을 잡아뒀습니다.)
 */
@Slf4j
@Service
public class ApiKakaoBook {

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApi;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiKakaoBook(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /** 카카오 도서검색 API 호출 - 제목(title) 기준 검색 */
    public List<BookKakaoDto> getBooks(String query) {

        URI uri = UriComponentsBuilder
                .fromUriString("https://dapi.kakao.com/v3/search/book")
                .queryParam("target", "title")
                .queryParam("query", query)
                .build()
                .toUri();

        List<BookKakaoDto> result = new ArrayList<>();

        try {
            String responseBody = restClient.get()
                    .uri(uri)
                    .header("Authorization", "KakaoAK " + kakaoRestApi)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode documents = root.path("documents");

            for (JsonNode item : documents) {
                BookKakaoDto book = new BookKakaoDto();
                book.setTitle(item.path("title").asText());
                book.setContents(item.path("contents").asText());
                book.setUrl(item.path("url").asText());
                book.setIsbn(item.path("isbn").asText());
                book.setDatetime(item.path("datetime").asText());
                book.setPublisher(item.path("publisher").asText());
                book.setPrice(item.path("price").asInt());
                book.setThumbnail(item.path("thumbnail").asText());

                List<String> authors = objectMapper.convertValue(
                        item.path("authors"),
                        new TypeReference<List<String>>() {}
                );
                book.setAuthors(authors);

                result.add(book);
            }

        } catch (Exception e) {
            log.warn("카카오 도서검색 API 호출/파싱 중 오류가 발생했습니다: {}", e.getMessage());
        }

        return result;
    }
}
