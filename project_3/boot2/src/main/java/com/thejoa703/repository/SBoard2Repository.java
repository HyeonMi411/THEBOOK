package com.thejoa703.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.entity.SBoard2;

@Repository										// Entity , PK
public interface SBoard2Repository extends JpaRepository<SBoard2, Long> {

	// 원글목록만 조회 (parent가 없는 글 = 원글)
	List<SBoard2> findByParentIsNullOrderByIdDesc();

	// 특정 원글의 답글목록 조회
	List<SBoard2> findByParent_IdOrderByIdAsc(Long parentId);

	// 제목으로 검색 (부분일치)
	List<SBoard2> findByBtitleContainingIgnoreCase(String btitle);

	// 특정 회원이 작성한 글목록
	List<SBoard2> findByUser_IdOrderByIdDesc(Long appUserId);

	// 해쉬태그이름으로 게시글 검색   (해쉬태그이름: List<Hashtag> hashtags 필드 name)
	List<SBoard2> findByHashtags_Name(String name);

	// 조회수 증가  (Insert/Update/Delete → @Modifying  @Transactional)
	@Modifying			// select 조회가 아니라 update/delete 용도
	@Transactional		// 안전장치
	@Query("UPDATE SBoard2 s SET s.bhit = s.bhit + 1 WHERE s.id = :id")
	void updateHitCount(@Param("id") Long id);

	// 원글 페이징 (Oracle ROWNUM)
	@Query(
			value = "SELECT * FROM ( " +
					"SELECT b.*, ROWNUM AS rnum " +
					"FROM (SELECT * FROM SBOARD2 WHERE PARENT_ID IS NULL ORDER BY ID DESC) b " +
					") " +
					"WHERE rnum BETWEEN :start AND :end",
			nativeQuery = true
	)
	List<SBoard2> findBoardsWithPaging(@Param("start") int start, @Param("end") int end);
}

/*
(1) 사용할수 있는 기본 SQL
	1. CREATE : save       - insert into  sboard2 ( 컬럼1, 컬럼2,,,)  values (?,?,,,,)
	2. READ   : findAll    - select * from sboard2
				findById   - select * from sboard2 where id=?
	3. UPDATE : save       - update  테이블명  set  컬럼1=?,,,, where id=?
	4. DELETE : deleteById - delete from sboard2 where id=?

(2) 검색       : findBy필드명
(3) 복잡한 sql : @Query
*/
