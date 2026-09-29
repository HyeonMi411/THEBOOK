package com.thejoa703.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.thejoa703.entity.Comment;
import com.thejoa703.entity.Hashtag;
import com.thejoa703.entity.Image;
import com.thejoa703.entity.Post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PostDto {

	// 작성/수정 요청 Dto (multipart/form-data 의 텍스트 필드)
	@Setter @Getter @AllArgsConstructor @NoArgsConstructor
	public static class PostRequestDto {
		@NotBlank(message = "내용을 입력해주세요.")
		@Size(max = 4000, message = "내용은 4000자를 넘을 수 없습니다.")
		private String content;

		@Size(max = 500, message = "해시태그 입력이 너무 깁니다.")
		private String hashtags;   // "#tag1, #tag2" 또는 "#tag1 #tag2" 형태의 문자열
	}

	// 게시글 응답 Dto
	@Getter @Setter @NoArgsConstructor
	public static class PostResponseDto {
		private Long id;
		private String content;
		private List<String> imageUrls;
		private List<String> hashtags;
		private LocalDateTime createdAt;
		private Long userId;            // 작성자 본인 여부 판별용 (모바일 상세화면의 수정/삭제 버튼)
		private String userNickname;
		private long likeCount;         // 좋아요 수
		private long commentCount;      // 삭제되지 않은 댓글 수
		private boolean likedByMe;      // 요청한 로그인 사용자가 이미 좋아요를 눌렀는지
		private String userProfileImage; // 작성자 프로필 이미지 (없으면 null)
		private long retweetCount;      // 리트윗 수
		private boolean retweetedByMe;  // 요청한 로그인 사용자가 리트윗했는지
		private String retweetedBy;     // 피드에서 "OOO님이 리트윗" 표시용 (리트윗으로 노출된 경우에만)

		public static PostResponseDto from(Post post) {
			return from(post, 0L, 0L, false);
		}

		public static PostResponseDto from(Post post, long likeCount, long commentCount, boolean likedByMe,
				long retweetCount, boolean retweetedByMe) {
			PostResponseDto dto = from(post, likeCount, commentCount, likedByMe);
			dto.setRetweetCount(retweetCount);
			dto.setRetweetedByMe(retweetedByMe);
			return dto;
		}

		public static PostResponseDto from(Post post, long likeCount, long commentCount, boolean likedByMe) {
			PostResponseDto dto = new PostResponseDto();
			dto.setId(post.getId());
			dto.setContent(post.getContent());
			if (post.getUser() != null) {
				dto.setUserId(post.getUser().getId());
				dto.setUserNickname(post.getUser().getNickname());
				dto.setUserProfileImage(post.getUser().getUfile());
			}
			dto.setImageUrls(post.getImages().stream().map(Image::getSrc).collect(Collectors.toList()));
			dto.setHashtags(post.getHashtags().stream().map(Hashtag::getName).collect(Collectors.toList()));
			dto.setCreatedAt(post.getCreatedAt());
			dto.setLikeCount(likeCount);
			dto.setCommentCount(commentCount);
			dto.setLikedByMe(likedByMe);
			return dto;
		}
	}

	// 리트윗 토글 결과
	@Getter @Setter @NoArgsConstructor @AllArgsConstructor
	public static class RetweetResponseDto {
		private boolean retweeted;
		private long retweetCount;
	}

	// 팔로우 토글 결과
	@Getter @Setter @NoArgsConstructor @AllArgsConstructor
	public static class FollowResponseDto {
		private boolean following;      // 토글 후 내가 이 사람을 팔로우 중인지
		private long followerCount;     // 토글 후 이 사람의 팔로워 수
	}

	// 팔로워/팔로잉 목록 한 줄
	@Getter @Setter @NoArgsConstructor @AllArgsConstructor
	public static class SimpleUserDto {
		private Long id;
		private String nickname;
		private String profileImage;

		public static SimpleUserDto from(com.thejoa703.entity.AppUser user) {
			return new SimpleUserDto(user.getId(), user.getNickname(), user.getUfile());
		}
	}

	// 사용자 프로필 (게시판에서 작성자 닉네임을 누르면 보이는 화면)
	@Getter @Setter @NoArgsConstructor
	public static class UserProfileDto {
		private Long id;
		private String nickname;
		private String profileImage;
		private long postCount;
		private long followerCount;
		private long followingCount;
		private boolean followedByMe;   // 요청한 로그인 사용자가 팔로우 중인지 (비로그인은 false)
		private boolean me;             // 내 프로필인지 (팔로우 버튼 숨김용)
	}

	// 좋아요 토글 결과
	@Getter @Setter @NoArgsConstructor @AllArgsConstructor
	public static class LikeResponseDto {
		private boolean liked;      // 토글 후 상태 (true = 좋아요 눌림)
		private long likeCount;     // 토글 후 전체 좋아요 수
	}

	// 댓글 작성 요청 Dto (JSON)
	@Setter @Getter @NoArgsConstructor @AllArgsConstructor
	public static class CommentRequestDto {
		@NotBlank(message = "댓글 내용을 입력해주세요.")
		@Size(max = 1000, message = "댓글은 1000자를 넘을 수 없습니다.")
		private String content;
	}

	// 댓글 응답 Dto
	@Getter @Setter @NoArgsConstructor
	public static class CommentResponseDto {
		private Long id;
		private Long postId;
		private Long userId;
		private String userNickname;
		private String content;
		private LocalDateTime createdAt;

		public static CommentResponseDto from(Comment comment) {
			CommentResponseDto dto = new CommentResponseDto();
			dto.setId(comment.getId());
			dto.setPostId(comment.getPost().getId());
			if (comment.getUser() != null) {
				dto.setUserId(comment.getUser().getId());
				dto.setUserNickname(comment.getUser().getNickname());
			}
			dto.setContent(comment.getContent());
			dto.setCreatedAt(comment.getCreatedAt());
			return dto;
		}
	}
}
