package com.thejoa703.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import com.thejoa703.service.Sboard2Service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "Sboard2 Api", description = "공지사항 게시판 관련 API (작성/수정/삭제는 관리자 전용)")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class Sboard2Controller {

	private final Sboard2Service sboard2Service;

	@Operation(summary = "공지글 작성", description = "관리자(adminUserId, ROLE_ADMIN)만 공지글을 작성할 수 있습니다.")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Sboard2ResponseDto> createNotice(
			@Parameter(description = "작성하는 관리자 사용자 ID") @RequestParam("adminUserId") Long adminUserId,
			@ModelAttribute Sboard2RequestDto dto,   // multipart/form-data
			@Parameter(description = "첨부파일")
			@RequestPart(name = "file", required = false) MultipartFile file,
			HttpServletRequest request
	) {
		String clientIp = request.getRemoteAddr();   // 작성자 IP(BIP, NOT NULL) 자동수집
		return ResponseEntity.ok(sboard2Service.createNotice(adminUserId, dto, file, clientIp));
	}

	@Operation(summary = "공지글 목록조회(페이징)", description = "Oracle ROWNUM 기반 페이징 조회입니다. (start, end 는 1부터 시작하는 순번)")
	@GetMapping
	public ResponseEntity<List<Sboard2ResponseDto>> getNotices(
			@Parameter(description = "시작 순번(1부터)") @RequestParam(value = "start", defaultValue = "1") int start,
			@Parameter(description = "끝 순번") @RequestParam(value = "end", defaultValue = "10") int end
	) {
		return ResponseEntity.ok(sboard2Service.getNoticesPaged(start, end));
	}

	@Operation(summary = "공지글 전체 갯수", description = "공지글 총 갯수를 조회합니다.")
	@GetMapping("/count")
	public ResponseEntity<Long> getNoticeCount() {
		return ResponseEntity.ok(sboard2Service.getNoticeCount());
	}

	@Operation(summary = "공지글 단건조회", description = "상세조회시 조회수(BHIT)가 1 증가합니다.")
	@GetMapping("/{id}")
	public ResponseEntity<Sboard2ResponseDto> getNotice(@PathVariable("id") Long id) {
		return ResponseEntity.ok(sboard2Service.getNoticeDetail(id));
	}

	@Operation(summary = "공지글 제목검색", description = "제목(btitle) 기준 LIKE 검색입니다.")
	@GetMapping("/search")
	public ResponseEntity<List<Sboard2ResponseDto>> searchNotices(
			@Parameter(description = "검색 키워드") @RequestParam("keyword") String keyword
	) {
		return ResponseEntity.ok(sboard2Service.searchByTitle(keyword));
	}

	@Operation(summary = "공지글 제목검색 + 페이징", description = "제목검색 + Oracle ROWNUM 페이징 조회입니다.")
	@GetMapping("/search/paging")
	public ResponseEntity<List<Sboard2ResponseDto>> searchNoticesPaged(
			@RequestParam("keyword") String keyword,
			@RequestParam("start") int start,
			@RequestParam("end") int end
	) {
		return ResponseEntity.ok(sboard2Service.searchByTitlePaged(keyword, start, end));
	}

	@Operation(summary = "관리자별 작성 공지글 조회", description = "특정 관리자가 작성한 공지글 목록을 조회합니다.")
	@GetMapping("/admin/{adminUserId}")
	public ResponseEntity<List<Sboard2ResponseDto>> getNoticesByAdmin(
			@PathVariable("adminUserId") Long adminUserId
	) {
		return ResponseEntity.ok(sboard2Service.getNoticesByAdmin(adminUserId));
	}

	@Operation(summary = "공지글 수정", description = "관리자(adminUserId, ROLE_ADMIN)만 공지글을 수정할 수 있습니다.")
	@PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Sboard2ResponseDto> updateNotice(
			@Parameter(description = "수정하는 관리자 사용자 ID") @RequestParam("adminUserId") Long adminUserId,
			@Parameter(description = "수정할 공지글 ID") @PathVariable("id") Long id,
			@ModelAttribute Sboard2RequestDto dto,
			@Parameter(description = "변경할 첨부파일")
			@RequestPart(name = "file", required = false) MultipartFile file
	) {
		return ResponseEntity.ok(sboard2Service.updateNotice(adminUserId, id, dto, file));
	}

	@Operation(summary = "공지글 삭제", description = "관리자(adminUserId, ROLE_ADMIN)만 공지글을 삭제할 수 있습니다.")
	@DeleteMapping("/{id}")
	public ResponseEntity<Long> deleteNotice(
			@Parameter(description = "삭제하는 관리자 사용자 ID") @RequestParam("adminUserId") Long adminUserId,
			@PathVariable("id") Long id
	) {
		sboard2Service.deleteNotice(adminUserId, id);
		return ResponseEntity.ok(id);
	}
}
// 요청 : Sboard2RequestDto ,  응답: Sboard2ResponseDto
// - POST   /api/notices                     공지글 작성(관리자전용)      ※기능: sboard2Service.createNotice
// - GET    /api/notices                     목록조회(페이징)             ※기능: sboard2Service.getNoticesPaged
// - GET    /api/notices/count               전체 갯수                    ※기능: sboard2Service.getNoticeCount
// - GET    /api/notices/{id}                단건조회(조회수+1)           ※기능: sboard2Service.getNoticeDetail
// - GET    /api/notices/search              제목검색                     ※기능: sboard2Service.searchByTitle
// - GET    /api/notices/admin/{adminUserId} 관리자별 작성글 조회         ※기능: sboard2Service.getNoticesByAdmin
// - PATCH  /api/notices/{id}                공지글 수정(관리자전용)      ※기능: sboard2Service.updateNotice
// - DELETE /api/notices/{id}                공지글 삭제(관리자전용)      ※기능: sboard2Service.deleteNotice
