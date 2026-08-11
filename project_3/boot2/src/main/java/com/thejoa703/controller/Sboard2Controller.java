package com.thejoa703.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thejoa703.dto.Sboard2Dto.Sboard2RequestDto;
import com.thejoa703.dto.Sboard2Dto.Sboard2ResponseDto;
import com.thejoa703.service.Sboard2Service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/jpa/boards")
@RequiredArgsConstructor
public class Sboard2Controller {

	private final Sboard2Service sboard2Service;

	// GET  /api/jpa/boards?start=1&end=10                     전체글목록 (오라클 페이징)
	@GetMapping
	public ResponseEntity<List<Sboard2ResponseDto>> getBoards(
			@RequestParam(name = "start", defaultValue = "1") int start,
			@RequestParam(name = "end", defaultValue = "10") int end) {
		return ResponseEntity.ok(sboard2Service.getBoardsPaged(start, end));
	}

	// GET  /api/jpa/boards/user/{userId}                      특정유저 작성글목록 (ManyToOne)
	@GetMapping("/user/{userId}")
	public ResponseEntity<List<Sboard2ResponseDto>> getBoardsByUser(@PathVariable("userId") Long userId) {
		return ResponseEntity.ok(sboard2Service.getBoardsByUser(userId));
	}

	// GET  /api/jpa/boards/{id}                                단건조회 (조회수 +1)
	@GetMapping("/{id}")
	public ResponseEntity<Sboard2ResponseDto> getBoard(@PathVariable("id") Long id) {
		return ResponseEntity.ok(sboard2Service.getBoardById(id));
	}

	// POST /api/jpa/boards?userId=1                            게시글작성
	@PostMapping
	public ResponseEntity<Sboard2ResponseDto> createBoard(
			@RequestParam("userId") Long userId,
			@RequestBody Sboard2RequestDto dto,
			HttpServletRequest request) {
		String clientIp = request.getRemoteAddr();
		return ResponseEntity.ok(sboard2Service.createBoard(userId, dto, clientIp));
	}

	// PATCH /api/jpa/boards/{id}?userId=1                      게시글수정 (본인+비밀번호 확인)
	@PatchMapping("/{id}")
	public ResponseEntity<Sboard2ResponseDto> updateBoard(
			@PathVariable("id") Long id,
			@RequestParam("userId") Long userId,
			@RequestBody Sboard2RequestDto dto) {
		return ResponseEntity.ok(sboard2Service.updateBoard(userId, id, dto));
	}

	// DELETE /api/jpa/boards/{id}?userId=1&bpass=1234           게시글삭제 (본인+비밀번호 확인)
	@DeleteMapping("/{id}")
	public ResponseEntity<Long> deleteBoard(
			@PathVariable("id") Long id,
			@RequestParam("userId") Long userId,
			@RequestParam("bpass") String bpass) {
		sboard2Service.deleteBoard(userId, id, bpass);
		return ResponseEntity.ok(id);
	}
}
// - GET    /api/jpa/boards                 전체글목록(오라클 페이징)  ※기능: sboard2Service.getBoardsPaged
// - GET    /api/jpa/boards/user/{userId}   특정유저 작성글목록        ※기능: sboard2Service.getBoardsByUser
// - GET    /api/jpa/boards/{id}            게시글 단건조회(조회수+1)   ※기능: sboard2Service.getBoardById
// - POST   /api/jpa/boards                 게시글작성                ※기능: sboard2Service.createBoard
// - PATCH  /api/jpa/boards/{id}            게시글수정                ※기능: sboard2Service.updateBoard
// - DELETE /api/jpa/boards/{id}            게시글삭제                ※기능: sboard2Service.deleteBoard
