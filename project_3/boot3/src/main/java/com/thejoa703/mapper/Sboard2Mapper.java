package com.thejoa703.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.Sboard2;

/**
 * Sboard2(공지사항) MyBatis 매퍼
 * ------------------------------------------------------------------
 * boot1(the703) 의 Sboard2Dao / sboard2-mapper.xml 을 boot3 구조(JPA 엔티티 Sboard2,
 * 시퀀스 SBOARD2_SEQ, APP_USER_ID 외래키)에 맞춰 재구성한 버전입니다.
 * 비회원 비밀번호(BPASS) 기반 수정/삭제 대신, 관리자(AppUser) 연동 방식으로 변경되었습니다.
 *
 *  실제 서비스에서 작성/수정/삭제는 Sboard2Service(JPA, @PreAuthorize("hasRole('ADMIN')"))를
 *   통해서만 호출하세요. 이 매퍼의 insert/update/delete 는 컨트롤러에서 직접 노출하지 않는
 *   것을 권장합니다(권한체크 없음).
 * ------------------------------------------------------------------
 */
@Mapper
public interface Sboard2Mapper {

	// ------------------------------------
	// 조회
	// ------------------------------------

	// 전체조회 (최신순)
	List<Sboard2> selectAll();

	// 페이징 조회 - map : { start, end }
	List<Sboard2> selectPaging(Map<String, Object> map);

	// 전체 게시글수
	int selectCnt();

	// 단건조회
	Sboard2 selectById(Long id);

	// 제목검색(부분일치)
	List<Sboard2> searchByTitle(String keyword);

	// 특정 관리자가 작성한 공지사항목록
	List<Sboard2> findByAppUserId(Long appUserId);

	// ------------------------------------
	// 등록 / 수정 / 삭제 / 조회수
	// ------------------------------------

	// 등록 - map : { btitle, bcontent, bfile, bip, appUserId }
	int insert(Map<String, Object> map);

	// 조회수 +1
	int updateHit(Long id);

	// 수정 - map : { id, btitle, bcontent, bfile }
	int update(Map<String, Object> map);

	// 삭제
	int delete(Long id);
}
