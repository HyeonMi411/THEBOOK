package com.thejoa703.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.dto.Sboard2Dto.Sboard2RequestDto;
import com.thejoa703.dto.Sboard2Dto.Sboard2ResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Sboard2;
import com.thejoa703.exception.ResourceNotFoundException;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.Sboard2Repository;
import com.thejoa703.util.FileStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 조회는 읽기전용 / 등록·수정·삭제·조회수증가는 메서드에 @Transactional 재선언
public class Sboard2Service {

	private final Sboard2Repository  sboard2Repository;
	private final AppUserRepository  appUserRepository;
	private final FileStorageService fileStorageService;   // 첨부파일 업로드처리

	// ------------------------------------------------------------
	// ★관리자 권한 검증 공통 메서드 - 공지글 작성/수정/삭제는 관리자만 가능
	// ------------------------------------------------------------
	private AppUser validateAdmin(Long adminUserId) {
		AppUser user = appUserRepository.findById(adminUserId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다. ID:" + adminUserId));

		if (!"ROLE_ADMIN".equals(user.getRole())) {
			throw new IllegalArgumentException("공지사항 작성/수정/삭제는 관리자만 가능합니다.");
		}
		return user;
	}

	// 1. 목록조회 (오라클 네이티브 페이징)
	public List<Sboard2ResponseDto> getNoticesPaged(int start, int end) {
		return sboard2Repository.findNoticesWithPaging(start, end).stream()
				.map(Sboard2ResponseDto::fromEntity)
				.collect(Collectors.toList());
	}

	// 2. 전체 갯수
	public long getNoticeCount() {
		return sboard2Repository.count();
	}

	// 3. 단건조회 (상세보기 - 조회수 +1 증가 포함)
	@Transactional
	public Sboard2ResponseDto getNoticeDetail(Long id) {
		sboard2Repository.increaseHit(id);   // 조회수 증가 (JPQL 벌크쿼리)

		Sboard2 board = sboard2Repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 공지글입니다. ID:" + id));
		return Sboard2ResponseDto.fromEntity(board);
	}

	// 4. 제목검색
	public List<Sboard2ResponseDto> searchByTitle(String keyword) {
		return sboard2Repository.findByBtitleContaining(keyword).stream()
				.map(Sboard2ResponseDto::fromEntity)
				.collect(Collectors.toList());
	}

	// 5. 제목검색 + 페이징
	public List<Sboard2ResponseDto> searchByTitlePaged(String keyword, int start, int end) {
		return sboard2Repository.searchNoticesWithPaging(keyword, start, end).stream()
				.map(Sboard2ResponseDto::fromEntity)
				.collect(Collectors.toList());
	}

	// 6. ★관리자가 작성한 공지글 목록
	public List<Sboard2ResponseDto> getNoticesByAdmin(Long adminUserId) {
		return sboard2Repository.findByUser_Id(adminUserId).stream()
				.map(Sboard2ResponseDto::fromEntity)
				.collect(Collectors.toList());
	}

	// ------------------------------------------------------------
	// 7. 작성 (관리자 전용)
	// ------------------------------------------------------------
	@Transactional
	public Sboard2ResponseDto createNotice(Long adminUserId, Sboard2RequestDto dto,
	                                        MultipartFile file, String clientIp) {
		AppUser admin = validateAdmin(adminUserId);

		Sboard2 board = new Sboard2();
		board.setUser(admin);   // ★작성한 관리자와 연결
		board.setBtitle(dto.getBtitle());
		board.setBcontent(dto.getBcontent());
		board.setBpass(dto.getBpass() != null ? dto.getBpass() : "");   // BPASS NOT NULL 컬럼 대응
		board.setBip(clientIp);

		if (file != null && !file.isEmpty()) {
			board.setBfile(fileStorageService.upload(file));
		}
		return Sboard2ResponseDto.fromEntity(sboard2Repository.save(board));
	}

	// ------------------------------------------------------------
	// 8. 수정 (관리자 전용, 더티체킹)
	// ------------------------------------------------------------
	@Transactional
	public Sboard2ResponseDto updateNotice(Long adminUserId, Long noticeId,
	                                        Sboard2RequestDto dto, MultipartFile file) {
		validateAdmin(adminUserId);

		Sboard2 board = sboard2Repository.findById(noticeId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 공지글입니다. ID:" + noticeId));

		board.setBtitle(dto.getBtitle());
		board.setBcontent(dto.getBcontent());
		if (dto.getBpass() != null && !dto.getBpass().isBlank()) {
			board.setBpass(dto.getBpass());
		}
		if (file != null && !file.isEmpty()) {
			board.setBfile(fileStorageService.upload(file));
		}
		return Sboard2ResponseDto.fromEntity(board);   // 저장메서드를 따로 호출하지 않아도 update 쿼리 반영(더티체킹)
	}

	// ------------------------------------------------------------
	// 9. 삭제 (관리자 전용)
	// ------------------------------------------------------------
	@Transactional
	public void deleteNotice(Long adminUserId, Long noticeId) {
		validateAdmin(adminUserId);

		if (!sboard2Repository.existsById(noticeId)) {
			throw new ResourceNotFoundException("존재하지 않는 공지글입니다. ID:" + noticeId);
		}
		sboard2Repository.deleteById(noticeId);
	}
}
