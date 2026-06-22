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

import com.the703.dto.BookDto;
import com.the703.service.BookService;
import com.the703.util.PagingUtil;

@Controller
public class BookController {  
	
	@Autowired BookService  service;
	//■1.  전체리스트
	//	@RequestMapping("/board/list.do")
	//	public String list(Model model) {  
	//		model.addAttribute("list", service.selectAll());  
	//		return  "board/list";   
	//	}
	
	@RequestMapping("/book/list")
	public String list( Model model  , @RequestParam(value="pstartno" , defaultValue = "1" ) int pstartno ) {
		model.addAttribute("paging" , new PagingUtil( service.findAllCnt() , pstartno) );  /*  service전체갯수 */
		model.addAttribute("list"   , service.findAll(pstartno));     /*  list10 */
		return  "book/list";   
	} 
	
	// 글쓰기 폼경로 
	
	@RequestMapping(value="/book/write", method=RequestMethod.GET)
	public String write( ) {
		return  "book/write";   
	} 
	
	// 글쓰기 기능
	//@PreAuthorize("hasAnyRole('ROLE_ADMIN' , 'ROLE_MEMBER')")  //4. 안에 있는 권한중
	//@PreAuthorize("isAuthenticated()  and  hasRole('ROLE_ADMIN')") //3. 로그인 + ADMIN 권한이 있다면

	//@PreAuthorize("isAnonymous()")       // 1. 로그인하지 않은 사용자
	@PreAuthorize("isAuthenticated()") // 2. 로그인을 했다면	
	@RequestMapping(value="/book/write"
							, method=RequestMethod.POST , headers=("content-type=multipart/*"))
	public String write_post(BookDto dto ,
			 @RequestParam("file") MultipartFile file   ,
			RedirectAttributes rttr) {
		String result ="글쓰기 실패";
		
		if(service.insert(dto , file) > 0) {  result = "글쓰기 성공"; }
		rttr.addFlashAttribute("result", result);  // Flash - 1번만동작
		return "redirect:/book/list";		
	} 
	
	
	// 상세보기   #   - BoardCtonroller 참고
	@RequestMapping("/book/detail")
	public String detail(BookDto dto ,  Model model ) {
		model.addAttribute("book" , service.detail(dto));
		return  "book/detail";   
	} 	
	 
	// 글수정 폼경로  #   ( 서비스연동하지말고)  - BoardCtonroller 참고
	@RequestMapping( value="/book/edit" , method = RequestMethod.GET)
	public String edit(BookDto dto , Model model) {
		model.addAttribute("book" , service.editView(dto));
		return  "book/edit";   
	} 	
	
	// 글수정 기능   (post)
	@RequestMapping( value= "/book/edit" , method = RequestMethod.POST)
	public String edit_post(
			BookDto dto,
			@RequestParam("file")  MultipartFile file, 
			RedirectAttributes rttr) { 
		// 알림창
		String result = "비밀번호 확인!";
		if( service.edit(dto , file) > 0 ) {  result = "수정성공";  }
		rttr.addFlashAttribute("result", result);
		
		return "redirect:/book/detail?bookId=" + dto.getBookId();
	} 	
		
	// 글 삭제  기능 
	@RequestMapping( value="/book/delete" , method = RequestMethod.GET)
	public String delete_post(BookDto dto, RedirectAttributes rttr) {  
		String result = "비밀번호 확인!";
		if( service.delete(dto) > 0 ) {  result = "삭제성공";  }
		rttr.addFlashAttribute("result", result);
		
		return  "redirect:/book/list";  
	}	
		
}
 