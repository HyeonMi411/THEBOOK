package com.thejoa703.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thejoa703.dto.PostDto.FollowResponseDto;
import com.thejoa703.dto.PostDto.PostResponseDto;
import com.thejoa703.dto.PostDto.SimpleUserDto;
import com.thejoa703.dto.PostDto.UserProfileDto;
import com.thejoa703.oauth2.CustomOAuth2User;
import com.thejoa703.service.AuthUserJwtService;
import com.thejoa703.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 커뮤니티 사용자 API - 프로필 / 팔로우 / 팔로워·팔로잉 / 좋아요한 글
 *
 * GET  /api/users/{id}/profile     프로필 (글 수, 팔로워/팔로잉 수, 내가 팔로우 중인지)  - 비로그인 가능
 * GET  /api/users/{id}/posts       이 사용자가 쓴 글 + 리트윗한 글                        - 비로그인 가능
 * GET  /api/users/{id}/followers   팔로워 목록                                           - 비로그인 가능
 * GET  /api/users/{id}/followings  팔로잉 목록                                           - 비로그인 가능
 * POST /api/users/{id}/follow      팔로우 토글                                           - 로그인 필요
 * GET  /api/users/me/liked-posts   내가 좋아요한 글                                      - 로그인 필요
 */
@Tag(name = "Social Api", description = "커뮤니티 프로필 / 팔로우 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class SocialController {

	private final PostService postService;
	private final AuthUserJwtService authUserJwtService;

	private Long currentUserIdOrNull(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
			return null;
		}
		return authUserJwtService.getCurrentUserId(authentication);
	}

	@Operation(summary = "내가 좋아요한 글")
	@GetMapping("/me/liked-posts")
	public ResponseEntity<List<PostResponseDto>> likedPosts(Authentication authentication) {
		return ResponseEntity.ok(postService.getLikedPosts(authUserJwtService.getCurrentUserId(authentication)));
	}

	@Operation(summary = "사용자 프로필")
	@GetMapping("/{userId}/profile")
	public ResponseEntity<UserProfileDto> profile(Authentication authentication, @PathVariable("userId") Long userId) {
		return ResponseEntity.ok(postService.getProfile(userId, currentUserIdOrNull(authentication)));
	}

	@Operation(summary = "사용자가 쓴 글 + 리트윗한 글")
	@GetMapping("/{userId}/posts")
	public ResponseEntity<List<PostResponseDto>> posts(Authentication authentication, @PathVariable("userId") Long userId) {
		return ResponseEntity.ok(postService.getUserPosts(userId, currentUserIdOrNull(authentication)));
	}

	@Operation(summary = "팔로워 목록")
	@GetMapping("/{userId}/followers")
	public ResponseEntity<List<SimpleUserDto>> followers(@PathVariable("userId") Long userId) {
		return ResponseEntity.ok(postService.getFollowers(userId));
	}

	@Operation(summary = "팔로잉 목록")
	@GetMapping("/{userId}/followings")
	public ResponseEntity<List<SimpleUserDto>> followings(@PathVariable("userId") Long userId) {
		return ResponseEntity.ok(postService.getFollowings(userId));
	}

	@Operation(summary = "팔로우 토글", description = "이미 팔로우 중이면 취소, 아니면 팔로우합니다.")
	@PostMapping("/{userId}/follow")
	public ResponseEntity<FollowResponseDto> follow(Authentication authentication, @PathVariable("userId") Long userId) {
		return ResponseEntity.ok(postService.toggleFollow(authUserJwtService.getCurrentUserId(authentication), userId));
	}
}
