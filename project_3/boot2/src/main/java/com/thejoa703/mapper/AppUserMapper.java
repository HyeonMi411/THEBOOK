package com.thejoa703.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.AppUser;

@Mapper
public interface AppUserMapper {
	// 닉네임 키워드로 검색 (boot2 DeptUserMapper.findByNameKeyword 참고)
	List<AppUser> findByNicknameKeyword(String keyword);
}
