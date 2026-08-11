package com.thejoa703.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.dto.Sboard2Dto.Sboard2RequestDto;
import com.thejoa703.dto.Sboard2Dto.Sboard2ResponseDto;
import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Sboard2;
import com.thejoa703.repository.AppUserRepository;
import com.thejoa703.repository.Sboard2Repository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class Sboard2Service {

	private final Sboard2Repository sboard2Repository;
	private final AppUserRepository appUserRepository;

	// 1. 오라클 네이티브 페이징 - 전체글목록
	public List<Sboard2ResponseDto> getBoardsPaged(int start, int end) {
		return sboard2Repository.findBoardsWithPaging(start, end).stream()
				.map(Sboard2ResponseDto::from)
				.collect(Collectors.toList());
	}

	// 2. 특정유저(AppUser)가 작성한 글목록 - ManyToOne user 로 조회
	public List<Sboard2ResponseDto> getBoardsByUser(Long userId) {
		return sboard2Repository.findByUser_Id(userId).stream()
				.map(Sboard2ResponseDto::from)
				.collect(Collectors.toList());
	}

	// 3. 단건조회 + 조회수 증가
	@Transactional
	public Sboard2ResponseDto getBoardById(Long boardId) {
		Sboard2 board = sboard2Repository.findById(boardId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. ID:" + boardId));
		sboard2Repository.increaseHit(boardId);   // 조회수 +1
		return Sboard2ResponseDto.from(board);
	}

	// 4. 게시글작성 ( AppUser - ManyToOne 연결 )
	@Transactional
	public Sboard2ResponseDto createBoard(Long userId, Sboard2RequestDto dto, String clientIp) {
		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. ID:" + userId));

		Sboard2 board = new Sboard2();
		board.setUser(user);
		board.setBtitle(dto.getBtitle());
		board.setBcontent(dto.getBcontent());
		board.setBpass(dto.getBpass());
		board.setBfile(dto.getBfile());
		board.setBip(clientIp);

		return Sboard2ResponseDto.from(sboard2Repository.save(board));
	}

	// 5. 게시글수정 ( 본인글 + 비밀번호 확인, 더티체킹 )
	@Transactional
	public Sboard2ResponseDto updateBoard(Long userId, Long boardId, Sboard2RequestDto dto) {
		Sboard2 board = sboard2Repository.findById(boardId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. ID:" + boardId));

		if (!board.getUser().getId().equals(userId)) {
			throw new IllegalArgumentException("본인 글만 수정할 수 있습니다.");
		}
		if (!board.getBpass().equals(dto.getBpass())) {
			throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
		}

		board.setBtitle(dto.getBtitle());
		board.setBcontent(dto.getBcontent());
		if (dto.getBfile() != null) {
			board.setBfile(dto.getBfile());
		}
		return Sboard2ResponseDto.from(board);  // save() 없이 update 쿼리 반영
	}

	// 6. 게시글삭제 ( 본인글 + 비밀번호 확인 )
	@Transactional
	public void deleteBoard(Long userId, Long boardId, String bpass) {
		Sboard2 board = sboard2Repository.findById(boardId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. ID:" + boardId));

		if (!board.getUser().getId().equals(userId)) {
			throw new IllegalArgumentException("본인 글만 삭제할 수 있습니다.");
		}
		if (!board.getBpass().equals(bpass)) {
			throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
		}
		sboard2Repository.delete(board);
	}
}
