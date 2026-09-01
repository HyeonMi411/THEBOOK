package com.thejoa703.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.Image;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

	List<Image> findByPost_Id(Long postId);

	void deleteByPost_Id(Long postId);
}
