package com.thejoa703.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thejoa703.dto.ChatbotDto.Answer;
import com.thejoa703.dto.ChatbotDto.AskRequest;
import com.thejoa703.dto.ChatbotDto.Category;
import com.thejoa703.service.ChatbotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 상담 챗봇 API (로그인 없이 사용 가능 - SecurityConfig 에서 permitAll)
 *
 * GET  /api/chatbot/menu  카테고리별 질문 버튼 목록
 * POST /api/chatbot/ask   {"faqId":"shop-pay"} 또는 {"message":"환불하고 싶어요"}
 */
@Tag(name = "Chatbot Api", description = "상담 챗봇 API (버튼 메뉴 + FAQ 검색 + AI 보조 답변 + 상담원 문의 안내)")
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

	private final ChatbotService chatbotService;

	@Operation(summary = "챗봇 메뉴", description = "카테고리별 자주 묻는 질문 버튼 목록")
	@GetMapping("/menu")
	public ResponseEntity<List<Category>> menu() {
		return ResponseEntity.ok(chatbotService.getMenu());
	}

	@Operation(summary = "챗봇에게 질문", description = "faqId(버튼) 또는 message(직접 입력) 중 하나를 보냅니다.")
	@PostMapping("/ask")
	public ResponseEntity<Answer> ask(@Valid @RequestBody AskRequest request,
			jakarta.servlet.http.HttpServletRequest httpRequest) {
		// IP 는 AI 보조답변 호출 횟수 제한에만 쓰고 저장하지 않는다 (nginx 뒤에서도 forward-headers-strategy 로 실제 IP)
		return ResponseEntity.ok(chatbotService.ask(request, httpRequest.getRemoteAddr()));
	}
}
