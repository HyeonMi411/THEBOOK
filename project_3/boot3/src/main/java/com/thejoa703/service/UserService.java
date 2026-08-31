package com.thejoa703.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.dto.LoginRequest;
import com.thejoa703.dto.UserDto.UserRequestDto;
import com.thejoa703.dto.UserDto.UserResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.exception.ResourceNotFoundException;
import com.thejoa703.mapper.AppUserMapper;
import com.thejoa703.util.FileStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final AppUserMapper      appUserMapper;
	private final FileStorageService fileStorageService;
	private final PasswordEncoder    passwordEncoder;

	@Transactional
	public UserResponseDto createUser(UserRequestDto request, MultipartFile profileImage) {
		String provider = request.getProvider() != null ? request.getProvider() : "local";

		if (appUserMapper.findByEmailAndProvider(request.getEmail(), provider).isPresent()) {
			throw new IllegalArgumentException("이미 존재하는 사용자입니다.");
		}
		if (appUserMapper.existsByNickname(request.getNickname())) {
			throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
		}

		AppUser user = new AppUser();
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setNickname(request.getNickname());
		user.setProvider(provider);
		user.setProviderId(provider);
		user.setRole("ROLE_USER");
		user.setDeleted(false);
		user.setUfile(
				profileImage != null && !profileImage.isEmpty()
						? fileStorageService.upload(profileImage)
						: "uploads/thejoa703.png"
		);

		appUserMapper.insert(user);
		return UserResponseDto.fromEntity(user);
	}

	public boolean existsByEmail(String email) {
		return appUserMapper.existsByEmail(email);
	}

	public boolean existsByNickname(String nickname) {
		return appUserMapper.existsByNickname(nickname);
	}

	public UserResponseDto login(LoginRequest request) {
		AppUser user = appUserMapper
				.findByEmailAndProvider(request.getEmail(), request.getProvider() != null ? request.getProvider() : "local")
				.orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을수 없습니다."));

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new IllegalArgumentException("비밀번호 불일치");
		}
		return UserResponseDto.fromEntity(user);
	}

	public Optional<AppUser> findByEmailAndProvider(String email, String provider) {
		return appUserMapper.findByEmailAndProvider(email, provider);
	}

	@Transactional
	public AppUser saveSocialUser(String email, String provider, String providerId, String nickname, String image) {
		AppUser user = AppUser.builder()
				.email(email)
				.provider(provider)
				.providerId(providerId)
				.nickname(nickname)
				.ufile(image)
				.password(passwordEncoder.encode("thejoa703"))
				.role("ROLE_USER")
				.deleted(false)
				.build();
		appUserMapper.insert(user);
		return user;
	}

	public String findRoleByUserId(Long userId) {
		String role = appUserMapper.findRoleById(userId);
		return role != null ? role : "ROLE_USER";
	}

	public UserResponseDto getUser(Long userId) {
		AppUser user = appUserMapper.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다.id : " + userId));
		return UserResponseDto.fromEntity(user);
	}

	public long countUsers() {
		return appUserMapper.count();
	}

	@Transactional
	public UserResponseDto updateNickname(Long userId, String newNickanme) {
		if (appUserMapper.existsByNickname(newNickanme)) {
			throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
		}
		AppUser user = appUserMapper.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

		user.setNickname(newNickanme);
		appUserMapper.update(user);
		return UserResponseDto.fromEntity(user);
	}

	@Transactional
	public UserResponseDto updateProfileImage(Long userId, MultipartFile profileImage) {
		AppUser user = appUserMapper.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

		user.setUfile(
				profileImage != null && !profileImage.isEmpty()
						? fileStorageService.upload(profileImage)
						: "uploads/thejoa703.png"
		);
		appUserMapper.update(user);
		return UserResponseDto.fromEntity(user);
	}

	@Transactional
	public void deleteById(Long userId) {
		if (!appUserMapper.existsById(userId)) {
			throw new IllegalArgumentException("삭제할 사용자가 존재하지 않습니다. ID: " + userId);
		}
		appUserMapper.deleteById(userId);
	}
}
