package com.thejoa703.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Follow;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

	Optional<Follow> findByFollower_IdAndFollowee_Id(Long followerId, Long followeeId);

	boolean existsByFollower_IdAndFollowee_Id(Long followerId, Long followeeId);

	long countByFollowee_Id(Long followeeId);   // 나를 팔로우하는 사람 수 (팔로워)

	long countByFollower_Id(Long followerId);   // 내가 팔로우하는 사람 수 (팔로잉)

	@Query("SELECT f.followee.id FROM Follow f WHERE f.follower.id = :userId")
	List<Long> findFolloweeIds(@Param("userId") Long userId);

	@Query("SELECT f.follower FROM Follow f WHERE f.followee.id = :userId ORDER BY f.createdAt DESC")
	List<AppUser> findFollowers(@Param("userId") Long userId);

	@Query("SELECT f.followee FROM Follow f WHERE f.follower.id = :userId ORDER BY f.createdAt DESC")
	List<AppUser> findFollowings(@Param("userId") Long userId);
}
