package com.thejoa703.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.thejoa703.entity.BookStock;

@Repository
public interface BookStockRepository extends JpaRepository<BookStock, Long> {

	// 결제승인(재고차감) 시점에 이 행을 잠급니다 (Oracle: SELECT ... FOR UPDATE)
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM BookStock s WHERE s.bookId = :bookId")
	Optional<BookStock> findByIdForUpdate(@Param("bookId") Long bookId);
}
