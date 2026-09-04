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
import com.thejoa703.security.EmailVerificationStore;
import com.thejoa703.util.FileStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final AppUserRepository  appUserRepository;
	private final FileStorageService fileStorageService;
	private final PasswordEncoder    passwordEncoder;
	private final EmailVerificationStore emailVerificationStore;

	@Transactional
	public UserResponseDto createUser(UserRequestDto request, MultipartFile profileImage) {
		String provider = request.getProvider() != null ? request.getProvider() : "local";

		if (appUserRepository.findByEmailAndProvider(request.getEmail(), provider).isPresent()) {
			throw new IllegalArgumentException("이미 존재하는 사용자입니다.");
		}
		if (appUserRepository.existsByNickname(request.getNickname())) {
			throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
		}
		// 일반(local) 회원가입은 소셜로그인처럼 제3자가 이메일 소유를 확인해주지 않으므로,
		// 반드시 POST /auth/email/send-code → /auth/email/verify-code 로 직접 인증을
		// 완료한 이메일이어야만 가입을 허용. 소셜 회원가입(saveSocialUser)은 이 메서드를
		// 거치지 않으므로 영향 없음.
		if ("local".equals(provider) && !emailVerificationStore.isVerified(request.getEmail())) {
			throw new IllegalArgumentException("이메일 인증을 먼저 완료해주세요.");
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
						? fileStorageService.uploadImage(profileImage)
						: "uploads/thejoa703.png"
		);

		appUserRepository.save(user);
		if ("local".equals(provider)) {
			emailVerificationStore.clearVerified(request.getEmail()); // 재사용 방지 - 가입 완료 후 정리
		}
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

		if (Boolean.TRUE.equals(user.getDeleted())) {
			throw new IllegalArgumentException("탈퇴한 계정입니다.");
		}
		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new IllegalArgumentException("비밀번호 불일치");
		}
		return UserResponseDto.fromEntity(user);
	}

	public Optional<AppUser> findByEmailAndProvider(String email, String provider) {
		return appUserRepository.findByEmailAndProvider(email, provider);
	}

	// provider 무관하게 이메일만으로 조회 - 소셜로그인 시 같은 이메일이 다른 provider로
	// 이미 가입되어 있는지 확인하는 용도(OAuth2SuccessHandler)
	public Optional<AppUser> findByEmail(String email) {
		return appUserRepository.findByEmail(email);
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
						? fileStorageService.uploadImage(profileImage)
						: "uploads/thejoa703.png"
		); // 더티체킹으로 트랜잭션 커밋시 자동 UPDATE
		return UserResponseDto.fromEntity(user);
	}

	@Transactional
	public void deleteById(Long userId) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("삭제할 사용자가 존재하지 않습니다. ID: " + userId));
		// 실제 행을 지우지 않고 DELETED 플래그만 갱신(소프트 삭제). AppUser 는 Book/Sboard2
		// 의 작성자(APP_USER_ID), Cart/Orders 의 소유자 등 여러 테이블이 FK 로 참조하는
		// 부모 행이라, 하드 삭제하면 Book 소프트삭제 도입 계기가 됐던 것과 동일한 FK 제약
		// 위반(ORA-02292)이 도서를 하나라도 등록했거나 주문 이력이 있는 계정에서 그대로 발생.
		user.setDeleted(true);
	}
}
