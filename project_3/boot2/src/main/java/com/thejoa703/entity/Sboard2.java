package com.thejoa703.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 공지사항 게시판 (SBOARD2) - 글쓰기는 관리자(ROLE_ADMIN)만 가능
@Entity
@Table(name = "SBOARD2")
@Getter @Setter @NoArgsConstructor
public class Sboard2 {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sboard2_seq")
	@SequenceGenerator(name = "sboard2_seq", sequenceName = "SBOARD2_SEQ", allocationSize = 1)
	@Column(name = "ID")
	private Long id;

	// ★공지글 작성은 관리자만 가능 -> 작성한 관리자와 연결
	@ManyToOne
	@JoinColumn(name = "APP_USER_ID", nullable = false)
	private AppUser user;          // 이 공지글을 작성한 관리자(AppUser)

	@Column(length = 1000, nullable = false)
	private String btitle;         // 제목

	@Lob // 대용량데이터처리 - CLOB(문자열)
	@Column(nullable = false)
	private String bcontent;       // 내용(긴 텍스트)

	@Column(length = 255, nullable = false)
	private String bpass;          // 글 비밀번호(수정/삭제용)

	@Column(length = 255)
	private String bfile;          // 첨부파일 경로

	@Column
	private Integer bhit = 0;      // 조회수

	@Column(length = 255, nullable = false)
	private String bip;            // 작성자 IP

	@Column(name = "CREATED_AT")
	private LocalDateTime createdAt; // 작성일시

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
		if (this.bhit == null) {
			this.bhit = 0;
		}
	}
}
