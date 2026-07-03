package com.the703.controller;

import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.the703.api.ApiEmail;
import com.the703.api.ApiNaverBook;
import com.the703.api.ApiOpenAi;
import com.the703.api.BookDto;
import com.the703.llmrag.AiService;

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
	
	
	
	   /////////5. LLM-RAG
    @Autowired AiService aiService;

    @GetMapping("/rag/chat")  ///		http://localhost:8080/api/util/rag/chat / http://localhost:8080/api/util/rag/ask
    public String chatPage() {    return "util/rag";  }

    @PostMapping("/rag/ask")   /// /api/util/rag/ask
    public String askWithPdf(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("question") String question,
            Model model) {
        try {
            String context;
            String fileName;
            
            //1. 사용자가 화면에서 새파일을 업로드한 경우
            if (file != null && !file.isEmpty()) {   
                context = aiService.extractTextFromPdf(file);
                fileName = file.getOriginalFilename();
                System.out.println("새로운 파일 업로드됨: " + fileName);
                
            } else { 
               //2. 파일업로드가 없으면  docs/company.pdf
                ClassPathResource resource = new ClassPathResource("docs/company.pdf");   //##
                if (!resource.exists()) { 
                    throw new IllegalArgumentException("기본 고정 PDF 파일(src/main/resources/docs/company.pdf)이 존재하지 않습니다.");
                }
                // 내장된 pdf의  스트림 전달
                try (InputStream is = resource.getInputStream()) {
                    context = aiService.extractTextFromPdf(is);
                }
                fileName = "company.pdf";  //##
                System.out.println("파일 업로드 없음: 프로젝트 내장 문서를 기본 컨텍스트로 사용합니다.");
            }
            //3. 컨텍스트와 질문을 결합하여  ai호출해서 답변
            String aiAnswer = aiService.askToGptWithContext(context, question);
            //4. 답변바인딩
            model.addAttribute("question", question);
            model.addAttribute("answer", aiAnswer);
            model.addAttribute("fileName", fileName);

        } catch (Exception e) {
            model.addAttribute("question", question);
            model.addAttribute("answer", "서버 처리 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("fileName", "오류 발생");
        }
      return "util/rag";      
  }
	
	
}






