package com.thejoa703.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.Image;

@Mapper
public interface ImageMapper {

	List<Image> findByPostId(Long postId);

	Image findById(Long id);

	void insert(Image image);

	void deleteById(Long id);

	void deleteByPostId(Long postId);
}
