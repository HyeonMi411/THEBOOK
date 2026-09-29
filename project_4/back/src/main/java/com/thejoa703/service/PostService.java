package com.thejoa703.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.dto.PostDto.CommentRequestDto;
import com.thejoa703.dto.PostDto.FollowResponseDto;
import com.thejoa703.dto.PostDto.RetweetResponseDto;
import com.thejoa703.dto.PostDto.SimpleUserDto;
import com.thejoa703.dto.PostDto.UserProfileDto;
import com.thejoa703.dto.PostDto.CommentResponseDto;
import com.thejoa703.dto.PostDto.LikeResponseDto;
import com.thejoa703.dto.PostDto.PostRequestDto;
import com.thejoa703.dto.PostDto.PostResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Comment;
import com.thejoa703.entity.Follow;
import com.thejoa703.entity.Hashtag;
import com.thejoa703.entity.Image;
import com.thejoa703.entity.Post;
import com.thejoa703.entity.PostLike;
import com.thejoa703.entity.Retweet;
import com.thejoa703.exception.ResourceNotFoundException;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.CommentRepository;
import com.thejoa703.repository.FollowRepository;
import com.thejoa703.repository.RetweetRepository;
import com.thejoa703.repository.HashtagRepository;
import com.thejoa703.repository.PostLikeRepository;
import com.thejoa703.repository.PostRepository;
import com.thejoa703.util.FileStorageService;

import lombok.RequiredArgsConstructor;

/**
 * SNS 게시판(게시글 / 이미지 / 해시태그 / 좋아요 / 댓글) 서비스
 *
 * 보안 원칙
 * - 작성자(userId)는 클라이언트가 보낸 값이 아니라, 컨트롤러가 JWT 에서 꺼낸 값만 신뢰한다.
 * - 수정/삭제는 항상 "본인 글/댓글인지" 서버에서 다시 확인한다. (IDOR 방지)
 * - 이미지는 project_3 의 FileStorageService.uploadImage() 로 확장자/Content-Type/실제 디코딩까지 검증한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

	private static final int MAX_IMAGES_PER_POST = 5;
	private static final int MAX_TAGS_PER_POST = 10;
	private static final int MAX_TAG_LENGTH = 50;

	private final PostRepository postRepository;
	private final AppUserRepository appUserRepository;
	private final HashtagRepository hashtagRepository;
	private final PostLikeRepository postLikeRepository;
	private final CommentRepository commentRepository;
	private final FileStorageService fileStorageService;
	private final RetweetRepository retweetRepository;
	private final FollowRepository followRepository;

	// ───────────────────────── 조회 ─────────────────────────

	/** 전체 게시글(최신순). tag 가 있으면 해당 해시태그가 붙은 글만. currentUserId 는 비로그인이면 null. */
	public List<PostResponseDto> getPosts(String tag, Long currentUserId) {
		return getPosts(tag, null, currentUserId);
	}

	/**
	 * feed = "following" 이면 내가 팔로우한 사람의 글 + 그 사람들이 리트윗한 글만 (로그인 필요).
	 * 그 외에는 전체 글. tag 필터는 두 경우 모두 적용된다.
	 */
	public List<PostResponseDto> getPosts(String tag, String feed, Long currentUserId) {
		if ("following".equalsIgnoreCase(feed)) {
			if (currentUserId == null) {
				throw new SecurityException("팔로잉 피드는 로그인 후 볼 수 있습니다.");
			}
			return getFollowingFeed(tag, currentUserId);
		}
		String normalized = normalizeTag(tag);
		List<Post> posts;
		if (!normalized.isEmpty()) {
			posts = postRepository.findByHashtags_NameAndDeletedFalse(normalized);
		} else {
			posts = postRepository.findByDeletedFalse();
		}
		return posts.stream()
				.sorted(Comparator.comparing(Post::getCreatedAt, Comparator.reverseOrder()))
				.map(p -> toDto(p, currentUserId))
				.collect(Collectors.toList());
	}

	/** 단건 조회 - 목록과 같은 형태(이미지/해시태그/좋아요 포함)로 내려준다. */
	public PostResponseDto getPost(Long id, Long currentUserId) {
		return toDto(findActivePost(id), currentUserId);
	}

	/** 등록된 해시태그 이름 목록(가나다순) - 모바일 해시태그 필터 칩용 */
	public List<String> getHashtagNames() {
		return hashtagRepository.findAll().stream()
				.map(Hashtag::getName)
				.sorted()
				.collect(Collectors.toList());
	}

	// ───────────────────────── 작성/수정/삭제 ─────────────────────────

	@Transactional
	public PostResponseDto createPost(Long userId, PostRequestDto dto, List<MultipartFile> files) {
		AppUser user = findUser(userId);
		Post post = new Post();
		post.setContent(dto.getContent().trim());
		post.setUser(user);
		attachImages(post, files);
		applyHashtags(post, dto.getHashtags());
		return toDto(postRepository.save(post), userId);
	}

	@Transactional
	public PostResponseDto updatePost(Long userId, Long postId, PostRequestDto dto, List<MultipartFile> files) {
		Post post = findActivePost(postId);
		assertOwner(post.getUser().getId(), userId, "본인 글만 수정할 수 있습니다.");
		post.setContent(dto.getContent().trim());
		attachImages(post, files);                       // 새 이미지가 있을 때만 기존 이미지를 교체
		if (dto.getHashtags() != null) {                 // null = 변경 없음, ""(빈 문자열) = 해시태그 전부 제거
			applyHashtags(post, dto.getHashtags());
		}
		return toDto(postRepository.save(post), userId);
	}

	@Transactional
	public void deletePost(Long userId, Long postId) {
		Post post = findActivePost(postId);
		assertOwner(post.getUser().getId(), userId, "본인 글만 삭제할 수 있습니다.");
		post.setDeleted(true);                           // 소프트 삭제 (더티체킹으로 UPDATE)
	}

	// ───────────────────────── 좋아요 ─────────────────────────

	/** 이미 눌렀으면 취소, 아니면 추가 (토글) */
	@Transactional
	public LikeResponseDto toggleLike(Long userId, Long postId) {
		Post post = findActivePost(postId);
		AppUser user = findUser(userId);
		Optional<PostLike> existing = postLikeRepository.findByUser_IdAndPost_Id(userId, postId);
		boolean liked;
		if (existing.isPresent()) {
			postLikeRepository.delete(existing.get());
			liked = false;
		} else {
			postLikeRepository.save(new PostLike(user, post));
			liked = true;
		}
		long likeCount = postLikeRepository.countByPostId(postId);
		return new LikeResponseDto(liked, likeCount);
	}

	// ───────────────────────── 리트윗 ─────────────────────────

	/** 이미 리트윗했으면 취소, 아니면 리트윗 (토글). 본인 글은 리트윗할 수 없다. */
	@Transactional
	public RetweetResponseDto toggleRetweet(Long userId, Long postId) {
		Post post = findActivePost(postId);
		if (post.getUser().getId().equals(userId)) {
			throw new IllegalArgumentException("본인 글은 리트윗할 수 없습니다.");
		}
		AppUser user = findUser(userId);
		Optional<Retweet> existing = retweetRepository.findByUser_IdAndOriginalPost_Id(userId, postId);
		boolean retweeted;
		if (existing.isPresent()) {
			retweetRepository.delete(existing.get());
			retweeted = false;
		} else {
			retweetRepository.save(new Retweet(user, post));
			retweeted = true;
		}
		return new RetweetResponseDto(retweeted, retweetRepository.countByOriginalPost_Id(postId));
	}

	// ───────────────────────── 팔로우 / 프로필 ─────────────────────────

	@Transactional
	public FollowResponseDto toggleFollow(Long followerId, Long followeeId) {
		if (followerId.equals(followeeId)) {
			throw new IllegalArgumentException("자기 자신은 팔로우할 수 없습니다.");
		}
		AppUser follower = findUser(followerId);
		AppUser followee = findActiveUser(followeeId);
		Optional<Follow> existing = followRepository.findByFollower_IdAndFollowee_Id(followerId, followeeId);
		boolean following;
		if (existing.isPresent()) {
			followRepository.delete(existing.get());
			following = false;
		} else {
			followRepository.save(new Follow(follower, followee));
			following = true;
		}
		return new FollowResponseDto(following, followRepository.countByFollowee_Id(followeeId));
	}

	public UserProfileDto getProfile(Long targetUserId, Long currentUserId) {
		AppUser user = findActiveUser(targetUserId);
		UserProfileDto dto = new UserProfileDto();
		dto.setId(user.getId());
		dto.setNickname(user.getNickname());
		dto.setProfileImage(user.getUfile());
		dto.setPostCount(postRepository.countByUser_IdAndDeletedFalse(targetUserId));
		dto.setFollowerCount(followRepository.countByFollowee_Id(targetUserId));
		dto.setFollowingCount(followRepository.countByFollower_Id(targetUserId));
		dto.setMe(currentUserId != null && currentUserId.equals(targetUserId));
		dto.setFollowedByMe(currentUserId != null && !dto.isMe()
				&& followRepository.existsByFollower_IdAndFollowee_Id(currentUserId, targetUserId));
		return dto;
	}

	public List<SimpleUserDto> getFollowers(Long userId) {
		findActiveUser(userId);
		return followRepository.findFollowers(userId).stream()
				.filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
				.map(SimpleUserDto::from).collect(Collectors.toList());
	}

	public List<SimpleUserDto> getFollowings(Long userId) {
		findActiveUser(userId);
		return followRepository.findFollowings(userId).stream()
				.filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
				.map(SimpleUserDto::from).collect(Collectors.toList());
	}

	/** 프로필 화면의 글 목록 - 이 사용자가 쓴 글 + 리트윗한 글 (최신순) */
	public List<PostResponseDto> getUserPosts(Long targetUserId, Long currentUserId) {
		AppUser target = findActiveUser(targetUserId);
		List<PostResponseDto> result = new ArrayList<>();
		for (Post p : postRepository.findByUser_IdAndDeletedFalse(targetUserId)) {
			result.add(toDto(p, currentUserId));
		}
		List<Long> retweetedIds = retweetRepository.findOriginalPostIdByUserId(targetUserId);
		if (!retweetedIds.isEmpty()) {
			for (Post p : postRepository.findByIdInAndDeletedFalse(retweetedIds)) {
				PostResponseDto dto = toDto(p, currentUserId);
				dto.setRetweetedBy(target.getNickname());
				result.add(dto);
			}
		}
		result.sort(Comparator.comparing(PostResponseDto::getCreatedAt, Comparator.reverseOrder()));
		return result;
	}

	/** 마이페이지 - 내가 좋아요한 글 (좋아요 누른 순서, 최신 먼저) */
	public List<PostResponseDto> getLikedPosts(Long userId) {
		List<Long> ids = postLikeRepository.findPostIdsByUserId(userId);
		if (ids.isEmpty()) {
			return new ArrayList<>();
		}
		Map<Long, Post> byId = new HashMap<>();
		for (Post p : postRepository.findByIdInAndDeletedFalse(ids)) {
			byId.put(p.getId(), p);
		}
		List<PostResponseDto> result = new ArrayList<>();
		for (Long id : ids) {
			Post p = byId.get(id);
			if (p != null) {
				result.add(toDto(p, userId));
			}
		}
		return result;
	}

	private List<PostResponseDto> getFollowingFeed(String tag, Long currentUserId) {
		List<Long> followeeIds = followRepository.findFolloweeIds(currentUserId);
		if (followeeIds.isEmpty()) {
			return new ArrayList<>();
		}
		String normalized = normalizeTag(tag);
		Map<Long, PostResponseDto> byId = new HashMap<>();
		for (Post p : postRepository.findByUser_IdInAndDeletedFalse(followeeIds)) {
			byId.put(p.getId(), toDto(p, currentUserId));
		}
		// 팔로우한 사람이 리트윗한 글도 피드에 포함 ("OOO님이 리트윗" 표시)
		for (Long followeeId : followeeIds) {
			List<Long> retweetedIds = retweetRepository.findOriginalPostIdByUserId(followeeId);
			if (retweetedIds.isEmpty()) {
				continue;
			}
			AppUser followee = appUserRepository.findById(followeeId).orElse(null);
			for (Post p : postRepository.findByIdInAndDeletedFalse(retweetedIds)) {
				if (byId.containsKey(p.getId())) {
					continue;
				}
				PostResponseDto dto = toDto(p, currentUserId);
				dto.setRetweetedBy(followee != null ? followee.getNickname() : null);
				byId.put(p.getId(), dto);
			}
		}
		return byId.values().stream()
				.filter(d -> normalized.isEmpty() || (d.getHashtags() != null && d.getHashtags().contains(normalized)))
				.sorted(Comparator.comparing(PostResponseDto::getCreatedAt, Comparator.reverseOrder()))
				.collect(Collectors.toList());
	}

	// ───────────────────────── 댓글 ─────────────────────────

	public List<CommentResponseDto> getComments(Long postId) {
		findActivePost(postId);
		return commentRepository.findByPostIdAndDeletedFalse(postId).stream()
				.sorted(Comparator.comparing(Comment::getCreatedAt))
				.map(CommentResponseDto::from)
				.collect(Collectors.toList());
	}

	@Transactional
	public CommentResponseDto addComment(Long userId, Long postId, CommentRequestDto dto) {
		Post post = findActivePost(postId);
		AppUser user = findUser(userId);
		Comment comment = new Comment();
		comment.setContent(dto.getContent().trim());
		comment.setUser(user);
		comment.setPost(post);
		return CommentResponseDto.from(commentRepository.save(comment));
	}

	@Transactional
	public void deleteComment(Long userId, Long postId, Long commentId) {
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 댓글입니다. ID:" + commentId));
		if (comment.isDeleted() || !comment.getPost().getId().equals(postId)) {
			throw new ResourceNotFoundException("존재하지 않는 댓글입니다. ID:" + commentId);
		}
		assertOwner(comment.getUser().getId(), userId, "본인 댓글만 삭제할 수 있습니다.");
		comment.setDeleted(true);
	}

	// ───────────────────────── 내부 도우미 ─────────────────────────

	private PostResponseDto toDto(Post post, Long currentUserId) {
		long likeCount = postLikeRepository.countByPostId(post.getId());
		long commentCount = commentRepository.countByPostIdAndDeletedFalse(post.getId());
		boolean likedByMe = currentUserId != null
				&& postLikeRepository.countByUser_IdAndPost_Id(currentUserId, post.getId()) > 0;
		long retweetCount = retweetRepository.countByOriginalPost_Id(post.getId());
		boolean retweetedByMe = currentUserId != null
				&& retweetRepository.countByUser_IdAndOriginalPost_Id(currentUserId, post.getId()) > 0;
		return PostResponseDto.from(post, likeCount, commentCount, likedByMe, retweetCount, retweetedByMe);
	}

	private Post findActivePost(Long id) {
		Post post = postRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 게시글입니다. ID:" + id));
		if (post.isDeleted()) {
			throw new ResourceNotFoundException("삭제된 게시글입니다.");
		}
		return post;
	}

	private AppUser findUser(Long userId) {
		return appUserRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다. ID:" + userId));
	}

	/** 탈퇴하지 않은 사용자만 (프로필/팔로우 대상) */
	private AppUser findActiveUser(Long userId) {
		AppUser user = findUser(userId);
		if (Boolean.TRUE.equals(user.getDeleted())) {
			throw new ResourceNotFoundException("탈퇴한 사용자입니다.");
		}
		return user;
	}

	private void assertOwner(Long ownerId, Long userId, String message) {
		if (ownerId == null || !ownerId.equals(userId)) {
			throw new SecurityException(message);   // GlobalExceptionHandler 에서 403 으로 변환
		}
	}

	/** 비어있지 않은 파일만 골라 검증·저장한 뒤 Image 로 연결. 유효한 파일이 없으면 아무것도 바꾸지 않는다. */
	private void attachImages(Post post, List<MultipartFile> files) {
		List<MultipartFile> valid = new ArrayList<>();
		if (files != null) {
			for (MultipartFile f : files) {
				if (f != null && !f.isEmpty()) {
					valid.add(f);
				}
			}
		}
		if (valid.isEmpty()) {
			return;
		}
		if (valid.size() > MAX_IMAGES_PER_POST) {
			throw new IllegalArgumentException("이미지는 최대 " + MAX_IMAGES_PER_POST + "장까지 첨부할 수 있습니다.");
		}
		post.getImages().clear();
		for (MultipartFile f : valid) {
			String url = fileStorageService.uploadImage(f);
			Image image = new Image();
			image.setSrc(url);
			image.setPost(post);
			post.getImages().add(image);
		}
	}

	/** "#a, #b #c" 처럼 쉼표/공백으로 구분된 문자열을 중복 없이 Hashtag 로 연결 */
	private void applyHashtags(Post post, String raw) {
		post.getHashtags().clear();
		for (String name : parseHashtags(raw)) {
			Hashtag tag = hashtagRepository.findByName(name).orElseGet(() -> {
				Hashtag created = new Hashtag();
				created.setName(name);
				return hashtagRepository.save(created);
			});
			post.getHashtags().add(tag);
		}
	}

	private Set<String> parseHashtags(String raw) {
		Set<String> result = new LinkedHashSet<>();
		if (raw == null) {
			return result;
		}
		for (String token : raw.split("[,\\s]+")) {
			String name = normalizeTag(token);
			if (name.isEmpty()) {
				continue;
			}
			if (name.length() > MAX_TAG_LENGTH) {
				throw new IllegalArgumentException("해시태그는 " + MAX_TAG_LENGTH + "자를 넘을 수 없습니다.");
			}
			result.add(name);
		}
		if (result.size() > MAX_TAGS_PER_POST) {
			throw new IllegalArgumentException("해시태그는 최대 " + MAX_TAGS_PER_POST + "개까지 등록할 수 있습니다.");
		}
		return result;
	}

	/** 앞의 # 제거 + 소문자 통일 (#Java 와 #java 가 서로 다른 태그로 갈라지지 않도록) */
	private String normalizeTag(String tag) {
		if (tag == null) {
			return "";
		}
		String t = tag.trim();
		while (t.startsWith("#")) {
			t = t.substring(1);
		}
		return t.trim().toLowerCase(Locale.ROOT);
	}
}
