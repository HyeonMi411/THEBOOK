package com.thejoa703.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.Sboard2;

@Mapper
public interface Sboard2Mapper {

	// ------------------------------------------------------------
	// 📢 기본 CRUD
	// ------------------------------------------------------------
	// 전체 목록조회 (페이징) - map: start, end
	List<Sboard2> findAll(Map<String, Object> map);

	// 전체 갯수
	int findAllCnt();

	// 단건조회
	Sboard2 findById(Long id);

	// 등록 - 관리자(ROLE_ADMIN)만 호출, board.user.id 필수
	int insert(Sboard2 board);

	// 수정
	int update(Sboard2 board);

	// 삭제
	int delete(Long id);


	// ------------------------------------------------------------
	// 👁 조회수 증가 (상세보기시 호출)
	// ------------------------------------------------------------
	int updateHit(Long id);


	// ------------------------------------------------------------
	// 🔍 제목검색 (페이징) - map: keyword, start, end
	// ------------------------------------------------------------
	List<Sboard2> searchByTitle(Map<String, Object> map);

	// 검색결과 갯수
	int searchByTitleCnt(String keyword);


	// ------------------------------------------------------------
	// ⭐ 비밀번호 확인 (수정/삭제시 검증) - map: id, bpass
	// ------------------------------------------------------------
	int checkPassword(Map<String, Object> map);


	// ------------------------------------------------------------
	// ★특정 관리자(AppUser)가 작성한 공지글 목록 - APP_USER_ID 참조
	// ------------------------------------------------------------
	List<Sboard2> findByUserId(Long userId);

}
