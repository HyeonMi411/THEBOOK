package com.thejoa703.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

	// ★한 회원이 여러개의 글을 쓸수 있다.	(SBoard2 : AppUser = N : 1)
	@ManyToOne
	@JoinColumn(name = "APP_USER_ID", nullable = false)
	AppUser user;			// 작성자		@ManyToOne		AppUser

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

	// ★답글형 게시판 : 한 게시글(원글)은 여러개의 답글을 가질 수 있다.  (SBoard2 : SBoard2 = 1 : N, 자기참조)
	@ManyToOne
	@JoinColumn(name = "PARENT_ID")
	SBoard2 parent;			// 부모글(원글) - null 이면 원글, 값이 있으면 답글		@ManyToOne

	// 1. mappedBy = "parent"        하위 SBoard2(답글) 의 parent 필드와 연결 - 읽기만 가능 / 수정 x
	// 2. cascade = CascadeType.ALL  원글 변화(생성,수정,삭제 등)와 연결된 답글에 반영
	// 3. orphanRemoval = true       원글 삭제시 답글들도 깔끔하게 삭제
	@Builder.Default
	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
	List<SBoard2> children = new ArrayList<>();	// 답글목록

	// ★한 게시글은 여러개의 해시태그(꼬리표)를 가질 수 있고, 하나의 해시태그는 여러 게시글에 쓰일 수 있다.  (SBoard2 : Hashtag = N : N)
	@Builder.Default
	@ManyToMany
	@JoinTable(name = "SBOARD2_HASHTAG",
		joinColumns = @JoinColumn(name = "SBOARD2_ID"),
		inverseJoinColumns = @JoinColumn(name = "HASHTAG_ID")
	)
	List<Hashtag> hashtags = new ArrayList<>();
}
