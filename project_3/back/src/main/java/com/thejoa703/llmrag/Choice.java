package com.thejoa703.llmrag;

public record Choice(
		int index, Message message, String finish_reason
) {}
