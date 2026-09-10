package com.thejoa703.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AiChatDto {

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AskResponseDto {
		private String answer;
	}
}
