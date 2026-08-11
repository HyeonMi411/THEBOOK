package com.thejoa703.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.Sboard2;

@Repository
public interface Sboard2Repository extends JpaRepository<Sboard2, Long> {  // Entity , PK(★기본키)

	// 특정 유저(AppUser)가 작성한 글 목록 - ManyToOne user 필드의 id 로 조회
	// @EntityGraph 로 user 를 한번에 조회 (N+1 방지, boot2 FollowRepository 참고)
	@EntityGraph(attributePaths = {"user"})
	List<Sboard2> findByUser_Id(Long userId);

	List<Sboard2> findByBtitleContaining(String keyword);

	// 전체 글목록 - Oracle 페이징 (boot2 PostRepository.findPostsWithPaging 참고)
	@Query(
		value = "SELECT * FROM ( " +
				"SELECT s.*, ROWNUM AS rnum " +
				"FROM (SELECT * FROM SBOARD2 ORDER BY CREATED_AT DESC) s " +
				") " +
				"WHERE rnum BETWEEN :start AND :end",
		nativeQuery = true
	)
	List<Sboard2> findBoardsWithPaging(@Param("start") int start, @Param("end") int end);

	// 조회수 증가 (직접 update)
	@Modifying
	@Query("UPDATE Sboard2 s SET s.bhit = s.bhit + 1 WHERE s.id = :id")
	int increaseHit(@Param("id") Long id);
}
/*
create  - save      : insert
read    - findAll   : select * from 테이블명
          findById  : select * from 테이블명  where id=?
update  - save      : update 테이블명  set 컬럼1=? ,,,   where id=?
delete  - delete    : delete from 테이블명  where id=?
*/
