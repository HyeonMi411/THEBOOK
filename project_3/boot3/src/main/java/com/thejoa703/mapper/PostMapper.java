package com.thejoa703.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.thejoa703.entity.Post;

@Mapper
public interface PostMapper {

	List<Post> findByDeletedFalse();

	Post findById(Long id);

	void insert(Post post);

	void update(Post post);

	void updateDeleted(@Param("id") Long id, @Param("deleted") boolean deleted);
}
