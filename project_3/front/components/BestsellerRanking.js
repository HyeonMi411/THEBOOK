// components/BestsellerRanking.js
// 판매량(결제완료 주문 기준) TOP 10 을 보여주는 랭킹 위젯임.
// 데이터는 백엔드가 Redis 에 10분간 캐싱해서 내려주므로, 이 컴포넌트는 그냥
// fetchBestsellersRequest 를 한 번 호출하고 결과를 그대로 렌더링만 .
import React, { useEffect } from 'react';
import Link from 'next/link';
import { useSelector, useDispatch } from 'react-redux';
import { fetchBestsellersRequest } from '../reducers/bookReducer';
import BookCoverImage from './BookCoverImage';

export default function BestsellerRanking() {
  const dispatch = useDispatch();
  const { bestsellers, bestsellersLoading } = useSelector((state) => state.book);

  useEffect(() => {
    dispatch(fetchBestsellersRequest());
  }, [dispatch]);

  if (bestsellersLoading && bestsellers.length === 0) {
    return null; // 첫 로딩 중에는 자리를 차지하지 않고 조용히 넘어감
  }
  if (bestsellers.length === 0) {
    return null; // 아직 결제완료 주문이 하나도 없으면(신규 서비스 등) 섹션 자체를 숨김
  }

  return (
    <div className="bestseller-ranking">
      <h3 className="bestseller-ranking-title">🏆 판매량 TOP 10</h3>
      <div className="bestseller-ranking-list">
        {bestsellers.map((item) => (
          <Link key={item.book.id} href={`/books/${item.book.id}`}>
            <a className="bestseller-ranking-item">
              <span className="bestseller-rank-num">{item.rank}</span>
              <div className="bestseller-cover">
                <BookCoverImage
                  src={item.book.bookCover
                    ? (item.book.bookCover.startsWith('http') ? item.book.bookCover : `http://localhost:8080/${item.book.bookCover}`)
                    : null}
                  alt={item.book.title}
                  iconSize={22}
                />
              </div>
              <div className="bestseller-info">
                <div className="bestseller-title">{item.book.title}</div>
                <div className="bestseller-sold">누적 {item.soldQuantity.toLocaleString()}권 판매</div>
              </div>
            </a>
          </Link>
        ))}
      </div>
    </div>
  );
}
