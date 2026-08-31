package com.thejoa703.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.AppUser;

@Mapper
public interface AppUserMapper {

	Optional<AppUser> findById(Long id);

	Optional<AppUser> findByEmail(String email);

	Optional<AppUser> findByEmailAndProvider(String email, String provider);

	boolean existsByNickname(String nickname);

	boolean existsByEmail(String email);

	boolean existsById(Long id);

	long count();

	String findRoleById(Long id);

	void insert(AppUser user);

	void update(AppUser user);

	void deleteById(Long id);
}
