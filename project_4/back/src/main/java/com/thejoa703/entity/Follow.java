package com.thejoa703.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 팔로우 (FOLLOWS) - flutter 수업(track011) 의 Follow 엔티티를 그대로 가져옴
 *
 *  follower : 구독하는 사람(나)      followee : 구독당하는 사람
 *  같은 사람을 두 번 팔로우할 수 없도록 (FOLLOWER_ID, FOLLOWEE_ID) 유니크 제약
 */
@Entity
@Table(name = "FOLLOWS",
	   uniqueConstraints = @UniqueConstraint(name = "UK_FOLLOW_PAIR", columnNames = {"FOLLOWER_ID", "FOLLOWEE_ID"}))
@Getter @Setter @NoArgsConstructor
public class Follow {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "follow_seq")
	@SequenceGenerator(name = "follow_seq", sequenceName = "FOLLOW_SEQ", allocationSize = 1)
	private Long id;

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "FOLLOWER_ID", nullable = false)
	private AppUser follower;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "FOLLOWEE_ID", nullable = false)
	private AppUser followee;

	@PrePersist
	void onCreate() { this.createdAt = LocalDateTime.now(); }

	public Follow(AppUser follower, AppUser followee) {
		this.follower = follower;
		this.followee = followee;
	}
}
