package com.thejoa703.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.BookStock;

@Mapper
public interface BookStockMapper {

	BookStock findByBookId(Long bookId);

	// 결제승인(재고차감) 시점에 이 행을 잠급니다 (Oracle: SELECT ... FOR UPDATE)
	BookStock findByBookIdForUpdate(Long bookId);

	void insert(BookStock stock);

	// 버전(version) 값이 일치하는 행만 갱신됩니다. 영향받은 행이 0이면 다른 트랜잭션이
	// 먼저 수정한 것이므로, 호출부(Service)에서 낙관적 락 충돌로 처리합니다.
	int updateWithVersionCheck(BookStock stock);

	// 도서 삭제 시, BOOK_STOCK 이 BOOK 을 FK 로 참조하고 있어서 먼저 지워야 합니다.
	void deleteByBookId(Long bookId);
}
