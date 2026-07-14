package com.the703.api;
import com.fasterxml.jackson.core.type.TypeReference;
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
            	// 기본 매핑
                book.setTitle(item.path("title").asText());
                book.setContents(item.path("contents").asText());
                book.setUrl(item.path("url").asText());
                book.setIsbn(item.path("isbn").asText());
                book.setDatetime(item.path("datetime").asText());
                book.setPublisher(item.path("publisher").asText());
                book.setPrice(item.path("price").asInt());
                book.setThumbnail(item.path("thumbnail").asText());
                
                // List 타입 처리 (authors)
                List<String> authors = objectMapper.convertValue(
                        item.path("authors"), 
                        new TypeReference<List<String>>() {}
                );
                book.setAuthors(authors);

                // DTO에 정의된 나머지 필드들은 카카오 API 응답에 기본적으로 포함되지 않으므로
                // 필요 시 별도 로직으로 설정하거나 초기값 상태로 둡니다.
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
                    .header("Authorization", "KakaoAK " + kakaoRestApi)
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





