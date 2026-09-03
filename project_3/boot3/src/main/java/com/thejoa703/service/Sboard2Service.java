package com.thejoa703.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.dto.PageResponseDto;
import com.thejoa703.dto.Sboard2Dto.Sboard2RequestDto;
import com.thejoa703.dto.Sboard2Dto.Sboard2ResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Sboard2;
import com.thejoa703.exception.ResourceNotFoundException;
import com.thejoa703.mapper.Sboard2Mapper;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.util.FileStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class Sboard2Service {

	private final Sboard2Mapper      sboard2Mapper;
	private final AppUserRepository  appUserRepository;
	private final FileStorageService fileStorageService;

	private static final int DEFAULT_PAGE_SIZE = 12;

	public List<Sboard2ResponseDto> getAllNotices() {
		return sboard2Mapper.selectAll().stream()
				.map(Sboard2ResponseDto::from)
				.collect(Collectors.toList());
	}

	public PageResponseDto<Sboard2ResponseDto> getAllNoticesPaged(int page, int size) {
		int currentPage = Math.max(page, 1);
		int pageSize = size > 0 ? size : DEFAULT_PAGE_SIZE;

		Map<String, Object> params = new HashMap<>();
		params.put("start", (currentPage - 1) * pageSize);
		params.put("end", pageSize);

		List<Sboard2> boards = sboard2Mapper.selectPaging(params);
		int totalElements = sboard2Mapper.selectCnt();
		int totalPages = (int) Math.ceil((double) totalElements / pageSize);

		List<Sboard2ResponseDto> content = boards.stream()
				.map(Sboard2ResponseDto::from)
				.collect(Collectors.toList());

		return new PageResponseDto<>(content, currentPage, pageSize, totalElements, totalPages);
	}

	@Transactional
	public Sboard2ResponseDto getNotice(Long id) {
		Sboard2 board = sboard2Mapper.selectById(id);
		if (board == null) {
			throw new ResourceNotFoundException("존재하지 않는 공지사항입니다. ID : " + id);
		}

		sboard2Mapper.updateHit(id);
		board.setBhit(board.getBhit() + 1);

		return Sboard2ResponseDto.from(board);
	}

	public List<Sboard2ResponseDto> searchByTitle(String keyword) {
		String cleaned = cleanKeyword(keyword);
		return sboard2Mapper.searchByTitle(cleaned).stream()
				.map(Sboard2ResponseDto::from)
				.collect(Collectors.toList());
	}

	private String cleanKeyword(String keyword) {
		if (keyword == null) { return ""; }
		return keyword.trim().replaceAll("\\s+", " ");
	}

	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public Sboard2ResponseDto createNotice(Long userId, Sboard2RequestDto dto, MultipartFile file, String ip) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다. ID : " + userId));

		Map<String, Object> params = new HashMap<>();
		params.put("btitle", dto.getBtitle());
		params.put("bcontent", dto.getBcontent());
		params.put("bip", ip != null ? ip : "0:0:0:0:0:0:0:1");
		params.put("appUserId", user.getId());
		if (file != null && !file.isEmpty()) {
			params.put("bfile", fileStorageService.uploadDocument(file));
		}

		sboard2Mapper.insert(params);
		Long newId = (Long) params.get("id");
		return getNoticeWithoutHitIncrease(newId);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public Sboard2ResponseDto updateNotice(Long id, Sboard2RequestDto dto, MultipartFile file) {
		if (sboard2Mapper.selectById(id) == null) {
			throw new ResourceNotFoundException("존재하지 않는 공지사항입니다. ID : " + id);
		}

		Map<String, Object> params = new HashMap<>();
		params.put("id", id);
		params.put("btitle", dto.getBtitle());
		params.put("bcontent", dto.getBcontent());
		if (file != null && !file.isEmpty()) {
			params.put("bfile", fileStorageService.uploadDocument(file));
		}

		sboard2Mapper.update(params);
		return getNoticeWithoutHitIncrease(id);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public void deleteNotice(Long id) {
		if (sboard2Mapper.selectById(id) == null) {
			throw new ResourceNotFoundException("존재하지 않는 공지사항입니다. ID : " + id);
		}
		sboard2Mapper.delete(id);
	}

	// 등록/수정 직후 응답용 조회 - 조회수를 증가시키지 않습니다 (getNotice()는 상세조회 전용)
	private Sboard2ResponseDto getNoticeWithoutHitIncrease(Long id) {
		Sboard2 board = sboard2Mapper.selectById(id);
		return Sboard2ResponseDto.from(board);
	}
}
