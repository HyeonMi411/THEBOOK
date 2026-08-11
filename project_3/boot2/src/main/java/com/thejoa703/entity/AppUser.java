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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; 

@Entity
@Getter @Setter @NoArgsConstructor 
@AllArgsConstructor  @Builder
public class AppUser {
    
	@Id   // jakarta.persistence.Id;
	@GeneratedValue(strategy = GenerationType.SEQUENCE  , generator = "appuser_seq" )
	@SequenceGenerator(name = "appuser_seq" , sequenceName = "APPUSER_SEQ" , allocationSize = 1)
	@Column(name="APP_USER_ID")
	private Long id;
	
	@Column(length = 120,  nullable = false)
	private String email;

	@Column(length = 200,  nullable = false)
	private String password;

	@Builder.Default
	@Column(length = 50,  nullable = false)
	private String role="ROLE_USER";   //기본 권한
	
	@Column(length = 150,  nullable = false)
	private String provider="local";

	@Column(name="PROVIDER_ID" ,  length = 150 )
	private String providerId="local";   
	//  kakao_id, naver_id
	
	@Column(length = 255)
	private String ufile;

	@Column(length = 50,  nullable = false)
	private String nickname;
	
	@Column(length = 30)
	private String mobile;

	@Column(name="Mbti_TYPE_ID")
	private Integer mbtitype;
	
	@Column
	private Boolean deleted=false;

	@Column(name= "CREATED_AT",  nullable = false)
	private LocalDateTime createdAt;

	@Column(name= "UPDATED_AT",  nullable = false)
	private LocalDateTime updatedAt;
	
	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}
	
	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
	
	public AppUser(String email, String password, String provider, String nickname) {
		super();
		this.email = email;
		this.password = password;
		this.provider = provider;
		this.nickname = nickname;
		this.role     = "ROLE_USER";
	}
	
	// ★한 사람이 여러 글을 쓸수 있다.  (AppUser)
	// 1. mappedBy = "user"          Post 엔티티에 있는 user 필드와 연결 - 읽기만 가능 / 수정 x
	// 2. cascade = CascadeType.ALL  AppUser 변화 (생성, 수정, 삭제 등)와 연결된 Post 에 반영
	// 3. orphanRemoval = true       유저탈퇴시 글들이 깔끔하게 삭제
	@OneToMany( mappedBy = "user" ,cascade = CascadeType.ALL, orphanRemoval = true )
	private List<Post> posts = new ArrayList<>(); 
	
	@OneToMany( mappedBy = "user" ,cascade = CascadeType.ALL, orphanRemoval = true )
	private List<PostLike> likes = new ArrayList<>(); 
	
	@OneToMany( mappedBy = "user" ,cascade = CascadeType.ALL, orphanRemoval = true )
	private List<Comment> comments = new ArrayList<>(); 
	
	@OneToMany( mappedBy= "follower",   cascade = CascadeType.ALL , orphanRemoval = true)
	private List<Follow>  following = new ArrayList<>();  //내가 팔로우한 사람들
	
	@OneToMany( mappedBy= "followee",   cascade = CascadeType.ALL , orphanRemoval = true)
	private List<Follow>  followers = new ArrayList<>(); // 팔로우를 당했어요 
	
	///////////////////////////////////////////////////////////

    // ★ 한 유저가 여러 글(SBOARD2)을 쓸 수 있다  (일대다)
    // 1. mappedBy = "user"          Sboard2 엔티티의 user 필드와 연결 - 읽기 전용(FK는 Sboard2 쪽이 관리)
    // 2. cascade = CascadeType.ALL  유저 변경(등록/수정/삭제)이 연결된 글에 반영
    // 3. orphanRemoval = true       유저 탈퇴시 작성글도 함께 삭제
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sboard2> boards = new ArrayList<>();

    // ★ 한 유저가 여러 책을 찜(좋아요)할 수 있고, 한 책도 여러 유저가 찜할 수 있다 (다대다)
    // - 소유(owning) 쪽: 조인테이블 BOOK_LIKE(APP_USER_ID, BOOK_ID) 를 이 쪽에서 관리
    // - 반대편(inverse) 은 Book.likedByUsers (mappedBy = "likedBooks")
    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "BOOK_LIKE",
        joinColumns = @JoinColumn(name = "APP_USER_ID"),
        inverseJoinColumns = @JoinColumn(name = "BOOK_ID")
    )
    private List<Book> likedBooks = new ArrayList<>();
    
    ///////////////////////////////////////////////////////////
	
}



