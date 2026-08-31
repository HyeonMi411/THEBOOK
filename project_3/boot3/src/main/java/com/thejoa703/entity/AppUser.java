package com.thejoa703.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@AllArgsConstructor @Builder
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "appuser_seq")
	@SequenceGenerator(name = "appuser_seq", sequenceName = "APPUSER_SEQ", allocationSize = 1)
	@Column(name = "APP_USER_ID")
	private Long id;

	@Column(length = 120, nullable = false)
	private String email;

	@Column(length = 200, nullable = false)
	private String password;

	@Builder.Default
	@Column(length = 50, nullable = false)
	private String role = "ROLE_USER"; // 기본 권한

	@Column(length = 150, nullable = false)
	private String provider = "local";

	@Column(name = "PROVIDER_ID", length = 150)
	private String providerId = "local"; // kakao_id, naver_id 등

	@Column(length = 255)
	private String ufile;

	@Column(length = 50, nullable = false)
	private String nickname;

	@Column(length = 30)
	private String mobile;

	@Column(name = "Mbti_TYPE_ID")
	private Integer mbtitype;

	@Column
	private Boolean deleted = false;

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "UPDATED_AT", nullable = false)
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
		this.role = "ROLE_USER";
	}
}
