package com.thejoa703.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.Sboard2;

@Mapper
public interface Sboard2Mapper {
	// 제목 키워드로 검색 (boot2 DeptUserMapper.findByNameKeyword 참고)
	List<Sboard2> findByKeyword(String keyword);

	// 특정 유저의 글 수 집계
	int countByAppUserId(Long appUserId);
}
