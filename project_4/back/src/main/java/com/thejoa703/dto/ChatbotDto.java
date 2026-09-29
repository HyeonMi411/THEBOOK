package com.thejoa703.dto;

import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 상담 챗봇 요청/응답 DTO */
public class ChatbotDto {

	/** 질문 요청 - 버튼을 누른 경우 faqId, 직접 입력한 경우 message 를 보낸다. */
	@Getter @Setter @NoArgsConstructor
	public static class AskRequest {
		@Size(max = 200, message = "질문은 200자를 넘을 수 없습니다.")
		private String message;

		@Size(max = 50, message = "잘못된 요청입니다.")
		private String faqId;
	}

	/** 답변 아래에 보여줄 "이런 질문은 어떠세요?" 버튼 한 개 */
	@Getter @AllArgsConstructor
	public static class Suggestion {
		private final String faqId;
		private final String label;
	}

	/**
	 * 챗봇 답변.
	 * handoff=true 면 앱에서 "상담원(이메일) 문의" 버튼을 함께 보여준다.
	 * source : faq(자주 묻는 질문) | ai(ChatGPT 보조 답변 - 앱에 "AI 답변" 표시) | none(답을 찾지 못함)
	 */
	@Getter @AllArgsConstructor
	public static class Answer {
		private final String answer;
		private final List<Suggestion> suggestions;
		private final boolean handoff;
		private final String contactEmail;
		private final String source;
	}

	/** 메뉴 화면의 카테고리(버튼 묶음) */
	@Getter @AllArgsConstructor
	public static class Category {
		private final String id;
		private final String label;
		private final List<Suggestion> items;
	}
}
