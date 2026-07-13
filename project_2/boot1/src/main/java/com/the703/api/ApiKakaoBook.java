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

@Service
public class ApiKakaoBook {

    @Value("${kakaoRestApi}")
    private String kakaoRestApi;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiKakaoBook(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public List<BookKakaoDto> getBooks(String query) {

        // 1. Kakao API URL 구성
        URI uri = UriComponentsBuilder
                .fromUriString("https://dapi.kakao.com/v3/search/book")
                .queryParam("target", "title")
                .queryParam("query", query)
                .build()
                .toUri();

        List<BookKakaoDto> result = new ArrayList<>();

        try {
            // 2. Kakao API 호출
            String responseBody = restClient.get()
                    .uri(uri)
                    .header("Authorization", "KakaoAK " + kakaoRestApi)
                    .retrieve()
                    .body(String.class);

            // 3. JSON 파싱
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode documents = root.path("documents");

            for (JsonNode item : documents) {
            	BookKakaoDto book = new BookKakaoDto();
                book.setTitle(item.path("title").asText());
                book.setThumbnail(item.path("thumbnail").asText()); //##### 추가    BookKakaoDto
                result.add(book);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
    
    public String getCoverByIsbn(String isbn) {

        URI uri = UriComponentsBuilder
                .fromUriString("https://dapi.kakao.com/v3/search/book")
                .queryParam("target", "isbn")
                .queryParam("query", isbn)
                .build()
                .toUri();

        try {
            String responseBody = restClient.get()
                    .uri(uri)
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode docs = root.path("documents");

            if (docs.size() > 0) {
                return docs.get(0).path("thumbnail").asText();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    
    
}
	
// title, author , image	
/*   
 curl -v -G GET "https://dapi.kakao.com/v3/search/book?target=title" \
  --data-urlencode "query=미움받을 용기" \
  -H "Authorization: KakaoAK ${REST_API_KEY}" 
*/





