package com.thejoa703.llmrag;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

/**
 * PDF 문서 내용을 근거(컨텍스트)로 삼아 OpenAI GPT 에게 질의응답하는 서비스.
 * 사용자가 PDF 를 직접 첨부하면 그 내용을, 첨부하지 않으면 서버 기동 시
 * RagInitializer 가 미리 읽어둔 회사소개 PDF(companyContext)를 컨텍스트로 사용.
 */
@Service
@RequiredArgsConstructor
public class AiService {

	private final RestClient openAiRestClient;

	// RagInitializer 가 서버 기동 시 채워두는 기본 컨텍스트 (docs/company.pdf 가 없으면 빈 문자열)
	private volatile String companyContext = "";

	// 비용 통제용 상한값 - gpt-4o-mini 기준으로도 컨텍스트/응답이 커질수록 비용이 커지므로 제한
	private static final int MAX_CONTEXT_CHARS = 6000;   // 입력 컨텍스트 최대 글자수
	private static final int MAX_RESPONSE_TOKENS = 500;  // 응답 최대 토큰수 (대략 400자 내외)

	public void setCompanyContext(String context) {
		this.companyContext = context;
	}

	public String getCompanyContext() {
		return companyContext;
	}

	// 1. 업로드된 pdf파일에서 원문텍스트 추출 - retrieval 역할수행 / 컨텍스트 증강
	public String extractTextFromPdf(MultipartFile file) throws IOException {
		try (PDDocument document = Loader.loadPDF(file.getBytes())) {
			PDFTextStripper stripper = new PDFTextStripper();
			return stripper.getText(document); // pdf전체 텍스트추출
		}
	}

	// 로컬 고정 파일(회사소개 PDF)용 - RagInitializer 에서 서버 기동 시 1회 호출
	public String extractTextFromPdf(InputStream inputStream) throws IOException {
		try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
			PDFTextStripper stripper = new PDFTextStripper();
			return stripper.getText(document);
		}
	}

	// 2. 연동후 결과 도출
	public String askToGptWithContext(String context, String question) {
		String systemInstruction = "당신은 업로드된 문서내용을 기반으로 답변하는 전문 비서입니다. 간결하게 답변하세요.";
		// 컨텍스트가 너무 길면(사용자가 큰 PDF를 첨부한 경우) 입력 토큰 비용이 커지므로
		// 앞부분 일정 길이까지만 사용. 회사소개처럼 짧은 문서는 대부분 이 범위 안에 들어옴.
		String trimmedContext = context.length() > MAX_CONTEXT_CHARS
				? context.substring(0, MAX_CONTEXT_CHARS)
				: context;
		String userPrompt = "--- [문서 내용] ---\n%s\n --- [질문] ---\n%s".formatted(trimmedContext, question);
		List<Message> messages = List.of(
				new Message("system", systemInstruction),
				new Message("user", userPrompt)
		);
		// max_tokens 로 응답 길이를 제한해서, 답변이 길어질수록 커지는 출력 토큰 비용을 통제
		LlmRagRequest requestBody = new LlmRagRequest("gpt-4o-mini", messages, MAX_RESPONSE_TOKENS);

		LlmRagResponse response = openAiRestClient.post()
				.uri("/chat/completions")
				.body(requestBody)
				.retrieve()
				.body(LlmRagResponse.class);

		if (response != null && !response.choices().isEmpty()) {
			return response.choices().get(0).message().getContent();
		}
		return "AI 응답을 생성하지 못했습니다.";
	}
}
