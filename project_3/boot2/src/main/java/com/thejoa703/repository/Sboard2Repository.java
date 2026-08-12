package com.thejoa703.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.entity.Sboard2;

@Repository
public interface Sboard2Repository extends JpaRepository<Sboard2, Long> { //Entity , PK

	// ------------------------------------------------------------
	// 📢 기본 CRUD ( JpaRepository 기본 제공 )
	// ------------------------------------------------------------
	// create - save        : insert into  sboard2 (컬럼1, 컬럼2,,,)  values (?,?,,,)
	// read   - findAll()   : select * from sboard2
	//          findById(id): select * from sboard2  where id=?
	// update - save        : update  sboard2  set  컬럼1=?,,,,  where  id=?
	// delete - deleteById  : delete from sboard2  where id=?


	// ------------------------------------------------------------
	// 🔎 단일조건 조회 / 집계
	// ------------------------------------------------------------
	// ★특정 관리자(AppUser)가 작성한 공지글 목록 - Sboard2.user 필드 참조 (글쓰기는 관리자만 가능)
	List<Sboard2> findByUser_Id(Long userId);

	// ★특정 관리자가 작성한 공지글 갯수
	long countByUser_Id(Long userId);

	// 제목검색 (LIKE)
	List<Sboard2> findByBtitleContaining(String btitle);

	// 비밀번호 확인 (수정/삭제시 검증용) - findBy   ※ Optional<Sboard2>
	Optional<Sboard2> findByIdAndBpass(Long id, String bpass);

	// 비밀번호 일치여부 집계 (existsBy)
	boolean existsByIdAndBpass(Long id, String bpass);


	// ------------------------------------------------------------
	// 📄 공지글 목록 페이징 (Oracle ROWNUM)
	// ------------------------------------------------------------
	@Query(
			value = "SELECT * FROM ( " +
	                "SELECT s.*, ROWNUM AS rnum " +
	                "FROM (SELECT * FROM SBOARD2 ORDER BY ID DESC) s " +
	               ") " +
	               "WHERE rnum BETWEEN :start AND :end",
			nativeQuery = true
	)
	List<Sboard2> findNoticesWithPaging(@Param("start") int start, @Param("end") int end);

	// 📄 제목검색 + 페이징
	@Query(
			value = "SELECT * FROM ( " +
	                "SELECT s.*, ROWNUM AS rnum " +
	                "FROM (SELECT * FROM SBOARD2 WHERE BTITLE LIKE '%' || :keyword || '%' ORDER BY ID DESC) s " +
	               ") " +
	               "WHERE rnum BETWEEN :start AND :end",
			nativeQuery = true
	)
	List<Sboard2> searchNoticesWithPaging(@Param("keyword") String keyword,
	                                       @Param("start") int start,
	                                       @Param("end") int end);

	// 제목검색 결과 갯수
	long countByBtitleContaining(String keyword);


	// ------------------------------------------------------------
	// 👁 조회수 증가 (상세보기시 호출) - select 조회가 아니라 update용도
	// ------------------------------------------------------------
	@Modifying      // update/delete 용도
	@Transactional  // 안전장치
	@Query("UPDATE Sboard2 s SET s.bhit = s.bhit + 1 WHERE s.id = :id")
	void increaseHit(@Param("id") Long id);

}
/*
(1) 사용할수 있는 기본 SQL
	1. CREATE : save       - insert into  sboard2 ( 컬럼1, 컬럼2,,,)  values (?,?,,,,)
	2. READ   : findAll    - select * from sboard2
	            findById   - select * from sboard2  where id=?
	3. UPDATE : save       - update  sboard2  set  컬럼1=?,,,,  where  id=?
	4. DELETE : deleteById - delete from sboard2  where id=?

(2) 공지글 작성(관리자only)은 Service 단에서 user.getRole()=="ROLE_ADMIN" 체크 후
    sboard2.setUser(관리자AppUser) 세팅하여 save()
*/
