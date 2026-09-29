package com.thejoa703.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상담 챗봇 질문 로그 (CHATBOT_LOGS) - Django 대시보드의 "많이 묻는 질문 / 답 못한 질문" 분석용
 * 누가 물었는지(사용자 ID·IP)는 저장하지 않는다. 질문 원문만 200자 이내로 저장.
 */
@Entity
@Table(name = "CHATBOT_LOGS")
@Getter @Setter @NoArgsConstructor
public class ChatbotLog {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chatbot_log_seq")
	@SequenceGenerator(name = "chatbot_log_seq", sequenceName = "CHATBOT_LOG_SEQ", allocationSize = 1)
	private Long id;

	@Column(name = "QUESTION", length = 200, nullable = false)
	private String question;

	@Column(name = "FAQ_ID", length = 50)
	private String faqId;          // 매칭된 FAQ (없으면 null)

	@Column(name = "SOURCE", length = 10, nullable = false)
	private String source;         // menu(버튼) | faq(검색) | ai(ChatGPT) | none(답 못함)

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() { this.createdAt = LocalDateTime.now(); }

	public ChatbotLog(String question, String faqId, String source) {
		this.question = question.length() > 200 ? question.substring(0, 200) : question;
		this.faqId = faqId;
		this.source = source;
	}
}
