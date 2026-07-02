package com.the703.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.the703.api.ApiEmail;

@Controller
@RequestMapping("/api/util")
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
}
