package com.the703.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.the703.api.ApiEmail;
import com.the703.api.ApiNaverBook;
import com.the703.api.ApiOpenAi;
import com.the703.api.BookDto;

@Controller
@RequestMapping("/api/util")		//★1
public class ApiUtilController {
	//////////1. mail
	// http://localhost:8080/api/util/mail
	@Autowired  ApiEmail apiemail;
	@GetMapping("/mail")  String get_mail() {  return "util/mail"; }		
	
	@PostMapping("/mail")
	public String post_mail( String subject , String content, String email ) {
		apiemail.sendMail(subject, content, email);
		return "util/mail_result";
	}
		
	////////////2. 스케쥴러
	////////////3. book
	// http://localhost:8080/api/util/books/json?search=spring
	@Autowired ApiNaverBook apibook;
	@GetMapping("/books") public String books() {	return "util/books";}
	
	@GetMapping(value="/books/json" , produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public List<BookDto> books_json( @RequestParam String search) {		
		return apibook.getBooks(search);
	}
	
	////////////4. openai
	// http://localhost:8080/api/util/openai
	@Autowired ApiOpenAi ApiOpenAi;
	
	@GetMapping("openai")  public String openai_get() {	return "util/openai";}
	
	@PostMapping(value="/openai" , produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String openai_post( @RequestBody String content ) {	return ApiOpenAi.getAIResponse(content);}
	
}






