package com.thejoa703.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.dto.Sboard2Dto.Sboard2RequestDto;
import com.thejoa703.dto.Sboard2Dto.Sboard2ResponseDto;
import com.thejoa703.service.AuthUserJwtService;
import com.thejoa703.service.Sboard2Service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Notice(Sboard2) Api", description = "공지사항 관련 API (글쓰기/수정/삭제는 ROLE_ADMIN 전용)")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class Sboard2Controller {

	private final Sboard2Service     sboard2Service;
	private final AuthUserJwtService authUserJwtService; // ###

	@Operation(summary = "공지사항 전체조회", description = "최신순 전체 공지사항")
	@GetMapping
	public ResponseEntity<List<Sboard2ResponseDto>> getNotices() {
		return ResponseEntity.ok(sboard2Service.getAllNotices());
	}

	@Operation(summary = "공지사항 단건조회", description = "조회시 조회수(BHIT)가 1 증가합니다.")
	@GetMapping("/{id}")
	public ResponseEntity<Sboard2ResponseDto> getNotice(
			@Parameter(description = "공지사항 ID") @PathVariable("id") Long id
	) {
		return ResponseEntity.ok(sboard2Service.getNotice(id));
	}

	@Operation(summary = "공지사항 제목검색")
	@GetMapping("/search")
	public ResponseEntity<List<Sboard2ResponseDto>> search(
			@Parameter(description = "검색 키워드") @RequestParam("keyword") String keyword
	) {
		return ResponseEntity.ok(sboard2Service.searchByTitle(keyword));
	}

	@Operation(summary = "공지사항 작성 (ROLE_ADMIN 전용)", description = "로그인한 관리자 계정으로 공지사항을 작성합니다.")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Sboard2ResponseDto> createNotice(
			Authentication authentication,
			@Valid @ModelAttribute Sboard2RequestDto dto, // multipart/form-data
			@Parameter(description = "첨부파일") @RequestPart(name = "bfile", required = false) MultipartFile file,
			HttpServletRequest request
	) {
		Long userId = authUserJwtService.getCurrentUserId(authentication);
		return ResponseEntity.ok(sboard2Service.createNotice(userId, dto, file, request.getRemoteAddr()));
	}

	@Operation(summary = "공지사항 수정 (ROLE_ADMIN 전용)")
	@PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Sboard2ResponseDto> updateNotice(
			@Parameter(description = "수정할 공지사항 ID") @PathVariable("id") Long id,
			@Valid @ModelAttribute Sboard2RequestDto dto,
			@Parameter(description = "교체할 첨부파일") @RequestPart(name = "bfile", required = false) MultipartFile file
	) {
		return ResponseEntity.ok(sboard2Service.updateNotice(id, dto, file));
	}

	@Operation(summary = "공지사항 삭제 (ROLE_ADMIN 전용)")
	@DeleteMapping("/{id}")
	public ResponseEntity<Long> deleteNotice(
			@Parameter(description = "삭제할 공지사항 ID") @PathVariable("id") Long id
	) {
		sboard2Service.deleteNotice(id);
		return ResponseEntity.ok(id);
	}
}
