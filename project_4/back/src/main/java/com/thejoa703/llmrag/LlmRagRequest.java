package com.thejoa703.llmrag;

import java.util.List;

import lombok.Value;

@Value
public class LlmRagRequest {
	String model;
	List<Message> messages;
	int max_tokens; // 응답 길이를 제한해서 건당 비용을 예측 가능한 범위로 통제
}
