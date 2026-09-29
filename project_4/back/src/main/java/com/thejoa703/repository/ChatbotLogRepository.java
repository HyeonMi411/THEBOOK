package com.thejoa703.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.ChatbotLog;

@Repository
public interface ChatbotLogRepository extends JpaRepository<ChatbotLog, Long> {
	long countByCreatedAtAfter(LocalDateTime from);
}
