package com.thejoa703.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.thejoa703.entity.Hashtag;
import com.thejoa703.entity.Post;

@Mapper
public interface HashtagMapper {

	Hashtag findByName(String name);

	// 해시태그 이름으로 조회하면서, 그 해시태그가 달린 게시글 목록까지 함께 반환
	// (JPA 의 "JOIN FETCH h.posts" 를 MyBatis 로 재현 - Service 에서 posts 를 채워 반환)
	List<Post> findPostsByHashtagName(String name);

	void insert(Hashtag hashtag);

	// 특정 게시글에 연결된 해시태그 목록 (POST_HASHTAG 조인테이블 경유)
	List<Hashtag> findByPostId(Long postId);

	void linkPostHashtag(@Param("postId") Long postId, @Param("hashtagId") Long hashtagId);

	void deleteLinksByPostId(Long postId);
}
