package com.thejoa703.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.SBoard2;

@Mapper
public interface SBoard2Mapper {

	// 원글목록 조회 (PARENT_ID 가 없는 글)
	List<SBoard2> findParents();

	// 특정 원글의 답글목록 조회
	List<SBoard2> findChildren(Long parentId);

	// 제목(부분일치) 검색
	List<SBoard2> findByTitleKeyword(String keyword);

	// 단건조회
	SBoard2 findById(Long id);

	// 원글목록 페이징 (Oracle ROWNUM)
	List<SBoard2> findBoardsWithPaging(Map<String, Object> params);	// params: start, end

	// 전체 원글수
	long countParents();

	// 등록 (원글/답글 공통)
	int insertBoard(SBoard2 board);

	// 수정
	int updateBoard(SBoard2 board);

	// 삭제
	int deleteBoard(Long id);

	// 조회수 +1
	int increaseHit(Long id);

	// 비밀번호 확인 (수정/삭제시)
	int checkPassword(Map<String, Object> params);	// params: id, bpass
}
