package com.thejoa703.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.dto.PostDto.PostRequestDto;
import com.thejoa703.dto.PostDto.PostResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Hashtag;
import com.thejoa703.entity.Image;
import com.thejoa703.entity.Post;
import com.thejoa703.mapper.AppUserMapper;
import com.thejoa703.mapper.HashtagMapper;
import com.thejoa703.mapper.ImageMapper;
import com.thejoa703.mapper.PostMapper;
import com.thejoa703.util.FileStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

	private final PostMapper         postMapper;
	private final ImageMapper        imageMapper;
	private final HashtagMapper      hashtagMapper;
	private final AppUserMapper      appUserMapper;
	private final FileStorageService fileStorageService;

	// MyBatis 는 JPA 의 @OneToMany/@ManyToMany 자동 로딩이 없으므로, Post 를 조회할 때마다
	// images/hashtags 를 직접 채워서 완전한 객체로 만들어줍니다.
	private Post loadPostWithDetails(Post post) {
		if (post == null) { return null; }
		post.setImages(imageMapper.findByPostId(post.getId()));
		post.setHashtags(hashtagMapper.findByPostId(post.getId()));
		return post;
	}

	public List<PostResponseDto> getAllPosts() {
		return postMapper.findByDeletedFalse().stream()
				.map(this::loadPostWithDetails)
				.map(PostResponseDto::from)
				.collect(Collectors.toList());
	}

	public Post getPostById(Long id) {
		Post post = postMapper.findById(id);
		if (post == null) {
			throw new IllegalArgumentException("존재하지 않는 게시글입니다 ID:" + id);
		}
		if (post.isDeleted()) {
			throw new IllegalArgumentException("삭제된 게시글 입니다.");
		}
		return loadPostWithDetails(post);
	}

	@Transactional
	public PostResponseDto createPost(Long userId, PostRequestDto dto, List<MultipartFile> files) {
		AppUser user = appUserMapper.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. ID : " + userId));

		Post post = new Post();
		post.setContent(dto.getContent());
		post.setUser(user);
		postMapper.insert(post);

		if (files != null && !files.isEmpty()) {
			for (MultipartFile file : files) {
				String url = fileStorageService.upload(file);
				Image image = new Image();
				image.setSrc(url);
				image.setPost(post);
				imageMapper.insert(image);
			}
		}

		linkHashtags(post, dto.getHashtags());

		return PostResponseDto.from(loadPostWithDetails(post));
	}

	@Transactional
	public PostResponseDto updatePost(Long userId, Long postId, PostRequestDto dto, List<MultipartFile> files) {
		Post post = postMapper.findById(postId);
		if (post == null) {
			throw new IllegalArgumentException("존재하지 않는 게시글입니다 ID:" + postId);
		}
		if (!post.getUser().getId().equals(userId)) {
			throw new IllegalArgumentException("본인 글만 수정할수 있습니다.");
		}

		post.setContent(dto.getContent());
		postMapper.update(post);

		if (files != null && !files.isEmpty()) {
			imageMapper.deleteByPostId(postId);
			for (MultipartFile file : files) {
				String url = fileStorageService.upload(file);
				Image image = new Image();
				image.setSrc(url);
				image.setPost(post);
				imageMapper.insert(image);
			}
		}

		if (dto.getHashtags() != null && !dto.getHashtags().isEmpty()) {
			hashtagMapper.deleteLinksByPostId(postId);
			linkHashtags(post, dto.getHashtags());
		}

		return PostResponseDto.from(loadPostWithDetails(post));
	}

	// "#해쉬,#first,태그" 형태의 문자열을 쉼표로 분리해서, 중복 제거 후 각 태그를
	// (이미 있으면 재사용, 없으면 새로 등록) POST_HASHTAG 조인테이블에 연결합니다.
	private void linkHashtags(Post post, String hashtagsCsv) {
		if (hashtagsCsv == null || hashtagsCsv.isBlank()) { return; }

		Set<String> distinctTags = Arrays.stream(hashtagsCsv.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toSet());

		distinctTags.forEach(tagStr -> {
			String normalized = tagStr.startsWith("#") ? tagStr.substring(1) : tagStr;
			Hashtag tag = hashtagMapper.findByName(normalized);
			if (tag == null) {
				tag = new Hashtag();
				tag.setName(normalized);
				hashtagMapper.insert(tag);
			}
			hashtagMapper.linkPostHashtag(post.getId(), tag.getId());
		});
	}

	@Transactional
	public void deletePost(Long userId, Long postId) {
		Post post = postMapper.findById(postId);
		if (post == null) {
			throw new IllegalArgumentException("존재하지 않는 게시글입니다 ID:" + postId);
		}
		if (!post.getUser().getId().equals(userId)) {
			throw new SecurityException("본인 글만 삭제 할수 있습니다.");
		}
		postMapper.updateDeleted(postId, true); // 소프트삭제
	}
}
