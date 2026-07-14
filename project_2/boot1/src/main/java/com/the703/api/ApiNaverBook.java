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
public class ApiNaverBook {
	@Value("${book.clientId}")     private String clientId;
	@Value("${book.clientSecret}") private String clientSecret;
	
	private final RestClient   restClient;
	private final ObjectMapper objectMapper = new ObjectMapper();
		
	public ApiNaverBook(RestClient.Builder restClientBuilder) {
		super();
		this.restClient = restClientBuilder.build();		
	}	
	
	public List<BookDto>  getBooks(String query){	
		//1. 주소  - UriComponentsBuilder (자동인코딩)
		java.net.URI uri = UriComponentsBuilder
				.fromUriString("https://openapi.naver.com/v1/search/book.json")
				.queryParam("query", query)
				.build()  
				.toUri();
		
		//2. 설정값 : responseBody
		List<BookDto>  result = new ArrayList<>();
		try {
			String  responseBody  = restClient.get()
										.uri(uri)
										.header("X-Naver-Client-Id" , clientId )
										.header("X-Naver-Client-Secret" , clientSecret)
										.retrieve()		// 설정내용을 http요청주고 응답 받아와 
										.body(String.class);		
			//3. 값받기		
			JsonNode root = objectMapper.readTree(responseBody);	System.out.println("....." + root );
			for( JsonNode item : root.path("items")) {
				BookDto book = new BookDto();
				book.setTitle(item.path("title").asText());
				book.setTitle(item.path("image").asText());
				book.setTitle(item.path("author").asText());
				result.add(book);				
			}
		
		}catch(Exception e) {e.printStackTrace(); }		
		
		return result;		
	}
	
	public String getCoverByIsbn(String isbn) {

	    URI uri = UriComponentsBuilder
	            .fromUriString("https://openapi.naver.com/v1/search/book.json")
	            .queryParam("query", isbn)
	            .queryParam("display", 1)
	            .build()
	            .toUri();

	    try {
	        String responseBody = restClient.get()
	                .uri(uri)
	                .header("X-Naver-Client-Id", clientId)
	                .header("X-Naver-Client-Secret", clientSecret)
	                .retrieve()
	                .body(String.class);

	        JsonNode root = objectMapper.readTree(responseBody);
	        JsonNode items = root.path("items");

	        if (items.size() > 0) {
	            return items.get(0).path("image").asText();  // ⭐ 네이버 표지 URL
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return null;
	}
	
}


// title, author , image	
/*
> GET /v1/search/book.xml?query=%EC%A3%BC%EC%8B%9D&display=10&start=1 HTTP/1.1
> Host: openapi.naver.com
> User-Agent: curl/7.49.1
> Accept: * / *
> X-Naver-Client-Id: {애플리케이션 등록 시 발급받은 클라이언트 아이디 값}
> X-Naver-Client-Secret: {애플리케이션 등록 시 발급받은 클라이언트 시크릿 값}
>
요청 예
curl "https://openapi.naver.com/v1/search/book.xml?query=%EC%A3%BC%EC%8B%9D&display=10&start=1" \
    -H "X-Naver-Client-Id: {애플리케이션 등록 시 발급받은 클라이언트 아이디 값}" \
    -H "X-Naver-Client-Secret: {애플리케이션 등록 시 발급받은 클라이언트 시크릿 값}" -v
*/





