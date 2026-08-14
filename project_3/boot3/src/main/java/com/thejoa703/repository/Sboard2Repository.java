package com.thejoa703.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.entity.Sboard2;

@Repository
public interface Sboard2Repository extends JpaRepository<Sboard2, Long> { // Entity , PK

	// ------------------------------------
	// 조회 (JPA 쿼리메서드 - findBy필드명)
	// ------------------------------------

	// 최신순 전체조회
	List<Sboard2> findAllByOrderByIdDesc();

	// 제목검색(부분일치)
	List<Sboard2> findByBtitleContainingOrderByIdDesc(String keyword);

	// 제목중복확인(선택)
	boolean existsByBtitle(String btitle);

	// 작성한 관리자기준 조회 (마이페이지 - 내가 쓴 공지사항)
	List<Sboard2> findByUser_IdOrderByIdDesc(Long userId);

	// ------------------------------------
	// 수정 (Insert/Update/Delete → @Modifying + @Transactional)
	// ------------------------------------

	// 조회수 증가
	@Modifying
	@Transactional
	@Query("UPDATE Sboard2 s SET s.bhit = s.bhit + 1 WHERE s.id = :id")
	void increaseHit(@Param("id") Long id);

	// ------------------------------------
	// 오라클 네이티브 페이징 (ROWNUM)
	// ------------------------------------

	@Query(
			value = "SELECT * FROM ( " +
					"SELECT s.*, ROWNUM AS rnum " +
					"FROM (SELECT * FROM SBOARD2 ORDER BY ID DESC) s " +
					") " +
					"WHERE rnum BETWEEN :start AND :end",
			nativeQuery = true
	)
	List<Sboard2> findNoticesWithPaging(@Param("start") int start, @Param("end") int end);
}
