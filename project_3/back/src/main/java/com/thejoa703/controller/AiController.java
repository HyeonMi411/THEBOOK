package com.thejoa703.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.dto.AiChatDto.AskResponseDto;
import com.thejoa703.llmrag.AiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

/**
 * AI RAG(문서 기반 질의응답) 챗봇 API.
 * PDF 를 직접 첨부하면 그 내용을, 첨부하지 않으면 서버가 미리 읽어둔
 * 회사소개 PDF(있는 경우)를 컨텍스트로 삼아 OpenAI GPT 에게 질문.
 * OpenAI API 호출은 건당 비용이 발생하므로, 로그인한 사용자만 사용할 수 있게 제한.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

	private final AiService aiService;

	@Operation(
			summary = "AI 문서 기반 질의응답 (RAG)",
			description = "PDF 를 첨부하면 그 문서 내용을, 첨부하지 않으면 서버에 미리 등록된 "
					+ "회사소개 PDF(있는 경우)를 근거로 OpenAI GPT 가 답변합니다. 로그인 필요."
	)
	@PostMapping(value = "/rag/ask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<AskResponseDto> askWithPdf(
			Authentication authentication,
			@Parameter(description = "근거로 삼을 PDF 파일 (생략하면 서버의 기본 컨텍스트 사용)")
			@RequestPart(value = "file", required = false) MultipartFile file,
			@Parameter(description = "질문 내용") @RequestParam("question") String question
	) {
		if (authentication == null) {
			return ResponseEntity.status(401).build();
		}
		try {
			String context = (file != null && !file.isEmpty())
					? aiService.extractTextFromPdf(file)
					: aiService.getCompanyContext();

			if (context == null || context.isBlank()) {
				return ResponseEntity.ok(new AskResponseDto(
						"참고할 문서가 없습니다. PDF 를 첨부하고 다시 질문해주세요."));
			}

			String answer = aiService.askToGptWithContext(context, question);
			return ResponseEntity.ok(new AskResponseDto(answer));
		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(new AskResponseDto("답변 생성 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}
}
