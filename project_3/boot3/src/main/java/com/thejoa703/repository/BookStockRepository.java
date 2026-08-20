package com.thejoa703.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.BookStock;

import jakarta.persistence.LockModeType;

@Repository
public interface BookStockRepository extends JpaRepository<BookStock, Long> { // Entity , PK(=BOOK_ID)

	// BookStock 의 PK 는 Book 의 PK 와 동일(@MapsId)하므로, findById(bookId) 로 바로 조회 가능합니다.

	// ★결제승인(재고차감) 시점에 비관적 락으로 재고행을 잠급니다.
	//   BookStock 에 이미 @Version(낙관적 락)이 있지만, 결제처럼 "실패시 즉시 재시도"보다
	//   "동시에 들어온 요청을 줄세워서 순서대로 처리"가 안전한 트랜잭션에서는
	//   비관적 락(SELECT ... FOR UPDATE)을 함께 쓸 수 있도록 별도 메서드를 준비했습니다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT bs FROM BookStock bs WHERE bs.bookId = :bookId")
	Optional<BookStock> findByIdForUpdate(@Param("bookId") Long bookId);
}
