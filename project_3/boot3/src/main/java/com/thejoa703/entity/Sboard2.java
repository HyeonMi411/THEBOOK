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
import lombok.Setter;

/**
 * 공지사항(게시판) 엔티티 (SBOARD2)
 * - BCONTENT 컬럼은 CLOB(대용량 문자열) 이므로 @Lob 처리
 * - 글쓰기는 관리자(ROLE_ADMIN)만 가능하도록 작성자(AppUser)와 연결
 * - 찜(Like) / 좋아요 관련 기능은 제외
 */
@Entity
@Table(name = "SBOARD2")
@Getter @Setter
public class Sboard2 {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sboard2_seq")
	@SequenceGenerator(name = "sboard2_seq", sequenceName = "SBOARD2_SEQ", allocationSize = 1)
	private Long id;

	@Column(length = 1000, nullable = false)
	private String btitle;

	@Lob // 대용량데이터처리 - CLOB(문자열) : 공지사항 본문
	@Column(nullable = false)
	private String bcontent;

	@Column(length = 255)
	private String bpass; // 레거시 호환용 컬럼 (관리자-회원 연동 이후에는 사용하지 않음)

	@Column(length = 255)
	private String bfile; // 첨부파일 경로 (/uploads 에 저장)

	@Column
	private Integer bhit = 0; // 조회수

	@Column(length = 255, nullable = false)
	private String bip; // 작성 IP

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
		if (this.bhit == null) { this.bhit = 0; }
	}

	// ★공지사항 글쓰기는 관리자만 가능하다 - 작성한 관리자 (AppUser)
	@ManyToOne
	@JoinColumn(name = "APP_USER_ID", nullable = false)
	private AppUser user;
}
