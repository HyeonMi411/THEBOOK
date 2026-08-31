package com.thejoa703.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
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
	
	// 이 글에 달린 이미지/해시태그 - MyBatis 는 자동 로딩을 안 해주므로,
	// PostService 에서 ImageMapper/HashtagMapper 로 직접 채워넣습니다.
	private List<Image>  images = new ArrayList<>();
	private List<Hashtag>  hashtags = new ArrayList<>();
}
