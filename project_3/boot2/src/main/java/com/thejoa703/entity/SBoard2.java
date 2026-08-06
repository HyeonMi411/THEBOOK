package com.thejoa703.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
@AllArgsConstructor @Builder
public class SBoard2 {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sboard2_seq")
	@SequenceGenerator(name = "sboard2_seq", sequenceName = "SBOARD2_SEQ", allocationSize = 1)
	@Column(name = "ID")
	private Long id;

	// ★ 게시글 여러개는 한 명의 회원에 속한다. (N:1)
	// FetchType.LAZY : 게시글 조회할 때 회원 정보를 즉시 안 가져오고, 필요할 때만 가져옴 (성능↑)
	// JoinColumn     : SBOARD2 테이블의 실제 FK 컬럼명 지정
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "APP_USER_ID", nullable = false)
	private AppUser user;

	@Column(length = 1000, nullable = false)
	private String btitle;

	@Lob
	@Column(nullable = false)
	private String bcontent;   // CLOB

	@Column(length = 255, nullable = false)
	private String bpass;

	@Column(length = 255)
	private String bfile;

	@Column
	private Integer bhit;

	@Column(length = 255, nullable = false)
	private String bip;

	@Column(name = "CREATED_AT")
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
		if (this.bhit == null) {
			this.bhit = 0;
		}
	}
}
