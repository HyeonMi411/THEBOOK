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
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.util.FileStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final AppUserRepository  appUserRepository;
	private final FileStorageService fileStorageService;
	private final PasswordEncoder    passwordEncoder;

	@Transactional
	public UserResponseDto createUser(UserRequestDto request, MultipartFile profileImage) {
		String provider = request.getProvider() != null ? request.getProvider() : "local";

		if (appUserRepository.findByEmailAndProvider(request.getEmail(), provider).isPresent()) {
			throw new IllegalArgumentException("이미 존재하는 사용자입니다.");
		}
		if (appUserRepository.existsByNickname(request.getNickname())) {
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

		appUserRepository.save(user);
		return UserResponseDto.fromEntity(user);
	}

	public boolean existsByEmail(String email) {
		return appUserRepository.existsByEmail(email);
	}

	public boolean existsByNickname(String nickname) {
		return appUserRepository.existsByNickname(nickname);
	}

	public UserResponseDto login(LoginRequest request) {
		AppUser user = appUserRepository
				.findByEmailAndProvider(request.getEmail(), request.getProvider() != null ? request.getProvider() : "local")
				.orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을수 없습니다."));

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new IllegalArgumentException("비밀번호 불일치");
		}
		return UserResponseDto.fromEntity(user);
	}

	public Optional<AppUser> findByEmailAndProvider(String email, String provider) {
		return appUserRepository.findByEmailAndProvider(email, provider);
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
		return appUserRepository.save(user);
	}

	public String findRoleByUserId(Long userId) {
		return appUserRepository.findById(userId)
				.map(AppUser::getRole)
				.orElse("ROLE_USER");
	}

	public UserResponseDto getUser(Long userId) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다.id : " + userId));
		return UserResponseDto.fromEntity(user);
	}

	public long countUsers() {
		return appUserRepository.count();
	}

	@Transactional
	public UserResponseDto updateNickname(Long userId, String newNickanme) {
		if (appUserRepository.existsByNickname(newNickanme)) {
			throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
		}
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

		user.setNickname(newNickanme); // 더티체킹으로 트랜잭션 커밋시 자동 UPDATE
		return UserResponseDto.fromEntity(user);
	}

	@Transactional
	public UserResponseDto updateProfileImage(Long userId, MultipartFile profileImage) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

		user.setUfile(
				profileImage != null && !profileImage.isEmpty()
						? fileStorageService.upload(profileImage)
						: "uploads/thejoa703.png"
		); // 더티체킹으로 트랜잭션 커밋시 자동 UPDATE
		return UserResponseDto.fromEntity(user);
	}

	@Transactional
	public void deleteById(Long userId) {
		if (!appUserRepository.existsById(userId)) {
			throw new IllegalArgumentException("삭제할 사용자가 존재하지 않습니다. ID: " + userId);
		}
		appUserRepository.deleteById(userId);
	}
}
