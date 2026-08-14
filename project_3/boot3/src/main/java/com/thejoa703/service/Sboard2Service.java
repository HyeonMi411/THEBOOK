package com.thejoa703.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
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
@Transactional(readOnly = true) // ##
public class Sboard2Service {

	private final Sboard2Repository  sboard2Repository;
	private final AppUserRepository  appUserRepository;
	private final FileStorageService fileStorageService; // 첨부파일 업로드처리

	// 1. 전체조회 (최신순)
	public List<Sboard2ResponseDto> getAllNotices() {
		return sboard2Repository.findAllByOrderByIdDesc().stream()
				.map(Sboard2ResponseDto::from)
				.collect(Collectors.toList());
	}

	// 2. 단건조회 ( 조회수 +1 )
	@Transactional
	public Sboard2ResponseDto getNotice(Long id) {
		Sboard2 board = sboard2Repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 공지사항입니다. ID : " + id));

		sboard2Repository.increaseHit(id); // DB 조회수 +1
		board.setBhit(board.getBhit() + 1); // 응답용 값도 +1 반영

		return Sboard2ResponseDto.from(board);
	}

	// 3. 제목검색
	public List<Sboard2ResponseDto> searchByTitle(String keyword) {
		return sboard2Repository.findByBtitleContainingOrderByIdDesc(keyword).stream()
				.map(Sboard2ResponseDto::from)
				.collect(Collectors.toList());
	}

	// 4. 공지사항 작성 ( ★관리자 전용 )
	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public Sboard2ResponseDto createNotice(Long userId, Sboard2RequestDto dto, MultipartFile file, String ip) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다. ID : " + userId));

		Sboard2 board = new Sboard2();
		board.setBtitle(dto.getBtitle());
		board.setBcontent(dto.getBcontent());
		board.setBip(ip != null ? ip : "0:0:0:0:0:0:0:1");
		board.setUser(user);

		if (file != null && !file.isEmpty()) {
			board.setBfile(fileStorageService.upload(file));
		}

		return Sboard2ResponseDto.from(sboard2Repository.save(board));
	}

	// 5. 공지사항 수정 ( ★관리자 전용 - 더티체킹으로 update 반영 )
	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public Sboard2ResponseDto updateNotice(Long id, Sboard2RequestDto dto, MultipartFile file) {
		Sboard2 board = sboard2Repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 공지사항입니다. ID : " + id));

		board.setBtitle(dto.getBtitle());
		board.setBcontent(dto.getBcontent());

		if (file != null && !file.isEmpty()) {
			board.setBfile(fileStorageService.upload(file));
		}

		return Sboard2ResponseDto.from(board); // 더티체킹(Dirty Checking)으로 자동 update
	}

	// 6. 공지사항 삭제 ( ★관리자 전용 )
	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public void deleteNotice(Long id) {
		if (!sboard2Repository.existsById(id)) {
			throw new ResourceNotFoundException("존재하지 않는 공지사항입니다. ID : " + id);
		}
		sboard2Repository.deleteById(id);
	}
}
