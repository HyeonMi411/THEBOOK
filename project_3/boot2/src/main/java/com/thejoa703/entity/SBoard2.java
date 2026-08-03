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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SBOARD2")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class SBoard2 {

	@Id		// jakarta.persistence.Id;
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sboard2_seq")
	@SequenceGenerator(name = "sboard2_seq", sequenceName = "SBOARD2_SEQ", allocationSize = 1)
	@Column(name = "ID")
	Long id;

	@ManyToOne
	@JoinColumn(name = "APP_USER_ID", nullable = false)
	AppUser user;			// @ManyToOne / @OneToMany	AppUser

	@Column(name = "BTITLE", nullable = false, length = 1000)
	String btitle;			// 게시글 제목

	@Lob
	@Column(name = "BCONTENT", nullable = false)
	String bcontent;		// 게시글 내용 ( 긴텍스트 )

	@Column(name = "BPASS", nullable = false, length = 255)
	String bpass;			// 게시글 비밀번호

	@Column(name = "BFILE", length = 255)
	String bfile;			// 첨부파일 경로

	@Column(name = "BHIT")
	Long bhit;				// 조회수

	@Column(name = "BIP", nullable = false, length = 255)
	String bip;				// 작성자 IP

	@Column(name = "CREATED_AT")
	LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
		if (this.bhit == null) this.bhit = 0L;
	}
}
