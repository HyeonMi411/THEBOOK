package com.thejoa703.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="POSTS")
@Getter  @Setter
public class Post {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE , generator = "post_seq")
	@SequenceGenerator(name = "post_seq" , sequenceName ="POST_SEQ" , allocationSize = 1)
	private Long id;
	
	@Column
	private boolean  deleted=false;
	
	@Column(nullable = false,  name = "CREATED_At")
	private LocalDateTime createdAt;
	
	@Column(nullable = false,  name = "UPDATED_At")
	private LocalDateTime updatedAt;
	
	@Lob // 대용량데이터처리 - CLOB(문자열)
	@Column(nullable = false)
	private String content;
	
	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}
	
	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
	 
	// 한 사람이 여러 글을 쓸수 있다.   (Post)
	@ManyToOne
	@JoinColumn(name="APP_USER_ID" , nullable = false)
	private AppUser user; 
	
	// 이 글에 달린 이미지 - Image 는 단순 CRUD 라 JPA Repository 를 씁니다. cascade 는
	// 일부러 걸지 않고, PostService 에서 이미지 교체(전체삭제 후 재등록)를 명시적으로 관리합니다.
	@OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
	private List<Image>  images = new ArrayList<>();

	// 이 글에 달린 해시태그 - Hashtag/POST_HASHTAG(다대다 조인테이블)는 관리 로직이
	// 복잡해서 Mapper(HashtagMapper)로 처리합니다. PostService 에서 직접 채워넣습니다.
	private List<Hashtag>  hashtags = new ArrayList<>();
}
