package com.the703.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.the703.dto.BoardDto;
import com.the703.service.BoardService;
import com.the703.util.PagingUtil;

@Controller
public class BookController {  
	
	@Autowired BoardService  service;
	
	//■1.  전체리스트
	//	@RequestMapping("/board/list.do")
	//	public String list(Model model) {  
	//		model.addAttribute("list", service.selectAll());  
	//		return  "board/list";   
	//	}
	
	@RequestMapping("/book/list")
	public String list( ) {
		return  "book/list";   
	} 
	
}
 