package com.thejoa703.llmrag;

import lombok.Value;

@Value
public class Message {
	String role;      // "system" / "user" / "assistant"
	String content;
}
