package com.thejoa703.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.dto.PostDto.CommentRequestDto;
import com.thejoa703.dto.PostDto.CommentResponseDto;
import com.thejoa703.dto.PostDto.LikeResponseDto;
import com.thejoa703.dto.PostDto.PostRequestDto;
import com.thejoa703.dto.PostDto.PostResponseDto;
import com.thejoa703.dto.PostDto.RetweetResponseDto;
import com.thejoa703.oauth2.CustomOAuth2User;
import com.thejoa703.service.AuthUserJwtService;
import com.thejoa703.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * SNS 게시판 API
 *
 * GET  /api/posts               게시글 목록 (?tag=해시태그, ?feed=following 팔로잉 피드)   - 비로그인 가능(팔로잉 피드는 로그인)
 * POST /api/posts/{id}/retweet  리트윗 토글                            - 로그인 필요
 * GET  /api/posts/hashtags      등록된 해시태그 목록                   - 비로그인 가능
 * GET  /api/posts/{id}          게시글 단건                            - 비로그인 가능
 * GET  /api/posts/{id}/comments 댓글 목록                              - 비로그인 가능
 * POST /api/posts               게시글 작성 (multipart)                - 로그인 필요
 * PATCH/DELETE /api/posts/{id}  게시글 수정/삭제 (본인만)               - 로그인 필요
 * POST /api/posts/{id}/like     좋아요 토글                            - 로그인 필요
 * POST /api/posts/{id}/comments 댓글 작성                              - 로그인 필요
 * DELETE /api/posts/{id}/comments/{commentId} 댓글 삭제 (본인만)       - 로그인 필요
 *
 * 작성자 ID 는 요청 파라미터가 아니라 JWT(Authentication) 에서만 가져온다.
 */
@Tag(name = "Post Api", description = "게시판 관련 API")   // swagger
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;
	private final AuthUserJwtService authUserJwtService;

	/** 로그인 필수 API 용 - 반드시 로그인 사용자 ID 를 돌려준다. */
	private Long currentUserId(Authentication authentication) {
		return authUserJwtService.getCurrentUserId(authentication);
	}

	/** 비로그인도 허용하는 조회 API 용 - 토큰이 없거나 익명이면 null (좋아요 표시만 생략) */
	private Long currentUserIdOrNull(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
			return null;
		}
		return authUserJwtService.getCurrentUserId(authentication);
	}

	@Operation(summary = "게시글 작성", description = "로그인한 사용자의 게시글을 작성합니다. 작성자는 JWT 에서 결정됩니다.")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<PostResponseDto> createPost(
			Authentication authentication,
			@Valid @ModelAttribute PostRequestDto dto,   // multipart/form-data
			@Parameter(description = "업로드할 이미지 파일 리스트(최대 5장)")
			@RequestPart(name = "files", required = false) List<MultipartFile> files
	) {
		return ResponseEntity.ok(postService.createPost(currentUserId(authentication), dto, files));
	}

	@Operation(summary = "전체 게시글", description = "삭제되지 않은 전체 게시글(최신순). tag 를 주면 해당 해시태그가 붙은 글만 조회합니다.")
	@GetMapping
	public ResponseEntity<List<PostResponseDto>> getPosts(
			Authentication authentication,
			@Parameter(description = "해시태그 필터 (예: java 또는 #java)")
			@RequestParam(name = "tag", required = false) String tag,
			@Parameter(description = "all(기본) 또는 following(내가 팔로우한 사람의 글 + 그들이 리트윗한 글)")
			@RequestParam(name = "feed", required = false) String feed
	) {
		return ResponseEntity.ok(postService.getPosts(tag, feed, currentUserIdOrNull(authentication)));
	}

	@Operation(summary = "해시태그 목록", description = "등록된 해시태그 이름 목록")
	@GetMapping("/hashtags")
	public ResponseEntity<List<String>> getHashtags() {
		return ResponseEntity.ok(postService.getHashtagNames());
	}

	@Operation(summary = "단건 게시글", description = "게시글 한 건 (이미지·해시태그·좋아요 수 포함)")
	@GetMapping("/{id}")
	public ResponseEntity<PostResponseDto> getPost(
			Authentication authentication,
			@PathVariable("id") Long id
	) {
		return ResponseEntity.ok(postService.getPost(id, currentUserIdOrNull(authentication)));
	}

	@Operation(summary = "게시글 수정", description = "본인 글만 수정할 수 있습니다.")
	@PatchMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<PostResponseDto> updatePost(
			Authentication authentication,
			@Parameter(description = "수정할 게시글 ID") @PathVariable("postId") Long postId,
			@Valid @ModelAttribute PostRequestDto dto,
			@Parameter(description = "새 이미지 파일 리스트(있으면 기존 이미지를 교체)")
			@RequestPart(name = "files", required = false) List<MultipartFile> files
	) {
		return ResponseEntity.ok(postService.updatePost(currentUserId(authentication), postId, dto, files));
	}

	@Operation(summary = "게시글 삭제", description = "본인 글만 삭제할 수 있습니다. (소프트 삭제)")
	@DeleteMapping("/{id}")
	public ResponseEntity<Long> deletePost(
			Authentication authentication,
			@PathVariable("id") Long id
	) {
		postService.deletePost(currentUserId(authentication), id);
		return ResponseEntity.ok(id);
	}

	@Operation(summary = "좋아요 토글", description = "이미 눌렀다면 취소, 아니면 추가합니다.")
	@PostMapping("/{id}/like")
	public ResponseEntity<LikeResponseDto> toggleLike(
			Authentication authentication,
			@PathVariable("id") Long id
	) {
		return ResponseEntity.ok(postService.toggleLike(currentUserId(authentication), id));
	}

	@Operation(summary = "리트윗 토글", description = "이미 리트윗했다면 취소, 아니면 리트윗합니다. 본인 글은 리트윗할 수 없습니다.")
	@PostMapping("/{id}/retweet")
	public ResponseEntity<RetweetResponseDto> toggleRetweet(
			Authentication authentication,
			@PathVariable("id") Long id
	) {
		return ResponseEntity.ok(postService.toggleRetweet(currentUserId(authentication), id));
	}

	@Operation(summary = "댓글 목록", description = "삭제되지 않은 댓글(작성순)")
	@GetMapping("/{id}/comments")
	public ResponseEntity<List<CommentResponseDto>> getComments(@PathVariable("id") Long id) {
		return ResponseEntity.ok(postService.getComments(id));
	}

	@Operation(summary = "댓글 작성", description = "로그인한 사용자의 댓글을 작성합니다.")
	@PostMapping("/{id}/comments")
	public ResponseEntity<CommentResponseDto> addComment(
			Authentication authentication,
			@PathVariable("id") Long id,
			@Valid @RequestBody CommentRequestDto dto
	) {
		return ResponseEntity.ok(postService.addComment(currentUserId(authentication), id, dto));
	}

	@Operation(summary = "댓글 삭제", description = "본인 댓글만 삭제할 수 있습니다. (소프트 삭제)")
	@DeleteMapping("/{id}/comments/{commentId}")
	public ResponseEntity<Long> deleteComment(
			Authentication authentication,
			@PathVariable("id") Long id,
			@PathVariable("commentId") Long commentId
	) {
		postService.deleteComment(currentUserId(authentication), id, commentId);
		return ResponseEntity.ok(commentId);
	}
}
