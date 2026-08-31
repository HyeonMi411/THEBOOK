// pages/books/national-library/index.js
// boot1(the703) templates/book/search-nl.html(KDC 분류 아코디언) +
// search-nl_list.html(검색결과 카드) 을 하나의 화면으로 재현했습니다.
// (DB에는 저장하지 않고 검색결과만 보여주는 화면이며, 상세보기 클릭시 상세페이지로 이동합니다)
//
// boot1 원본은 부트스트랩 아코디언(data-bs-toggle="collapse")으로 되어있는데,
//  부트스트랩 JS 번들이 제대로 로드/초기화되지 않으면 "대분류를 클릭해도 하위
//  키워드 목록이 안 펼쳐지는" 증상이 그대로 나타납니다. 그래서 React 자체 상태로
//  여닫는 진짜 아코디언을 직접 구현했습니다(외부 JS 라이브러리 의존 없이 항상 동작).
import React, { useState } from 'react';
import { useRouter } from 'next/router';
import { useSelector, useDispatch } from 'react-redux';
import { nlSearchRequest, selectNlBook } from '../../../reducers/bookReducer';
import BookCoverImage from '../../../components/BookCoverImage';

// boot1 search-nl.html 의 KDC(한국십진분류법) 전체 분류체계를 그대로 재현
const KDC_GROUPS = [
  { title: '000 총류', items: ['001 지식, 학문 일반', '003 사전', '004 컴퓨터과학', '005 프로그래밍, 소프트웨어', '006 특허, 표준', '007 정보학', '008 총서', '009 기타 총류'] },
  { title: '100 철학', items: ['110 형이상학', '120 인식론', '130 논리학', '140 윤리학', '150 심리학', '160 미학', '170 동양철학', '180 서양철학', '190 기타 철학'] },
  { title: '200 종교', items: ['210 비교종교', '220 불교', '230 기독교', '240 천주교', '250 도교', '260 이슬람교', '270 힌두교', '280 기타 종교'] },
  { title: '300 사회과학', items: ['310 통계학', '320 경제학', '330 경영학', '340 법학', '350 행정학', '360 사회학', '370 교육학', '380 풍속, 민속학', '390 정치학'] },
  { title: '400 자연과학', items: ['410 수학', '420 물리학', '430 화학', '440 천문학', '450 지학', '460 생물학', '470 식물학', '480 동물학', '490 기타 자연과학'] },
  { title: '500 기술과학', items: ['510 의학', '520 공학일반', '530 건축공학', '540 기계공학', '550 전기전자공학', '560 화학공학', '570 제조업', '580 생활과학', '590 농업, 축산업'] },
  { title: '600 예술', items: ['610 건축예술', '620 조각', '630 회화', '640 사진', '650 음악', '660 연극', '670 영화', '680 오락, 스포츠'] },
  { title: '700 언어', items: ['710 한국어', '720 중국어', '730 일본어', '740 영어', '750 독일어', '760 프랑스어', '770 스페인어', '780 기타 언어'] },
  { title: '800 문학', items: ['810 한국문학', '820 중국문학', '830 일본문학', '840 영어문학', '850 독일문학', '860 프랑스문학', '870 스페인문학', '880 기타 문학'] },
  { title: '900 역사', items: ['910 한국사', '920 아시아사', '930 유럽사', '940 아메리카사', '950 아프리카사', '960 오세아니아사', '970 고고학', '980 전기', '990 기타 역사'] },
];

export default function NationalLibrarySearchPage() {
  const router = useRouter();
  const dispatch = useDispatch();
  const { nlResults, nlLoading, nlError } = useSelector((state) => state.book);

  const [keyword, setKeyword] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  // boot1 원본처럼 "000 총류"만 기본으로 펼쳐진 상태로 시작 (accordion-collapse show)
  const [expandedGroup, setExpandedGroup] = useState('000 총류');

  const toggleGroup = (title) => {
    setExpandedGroup((prev) => (prev === title ? null : title)); // 다시 누르면 접힘
  };

  const runSearch = (kw) => {
    setSelectedCategory('');
    dispatch(nlSearchRequest({ keyword: kw, page: 1 }));
  };

  const onSubmit = (e) => {
    e.preventDefault();
    if (!keyword.trim()) return;
    runSearch(keyword.trim());
  };

  // 키워드(하위 분류) 클릭 → 국립중앙도서관 검색 실행 → 결과목록 노출
  const onKeywordClick = (keywordText) => {
    setKeyword('');
    setSelectedCategory(keywordText);
    dispatch(nlSearchRequest({ keyword: keywordText, page: 1 }));
  };

  // 검색결과 카드 클릭 → 상세페이지로 이동
  const goDetail = (book) => {
    dispatch(selectNlBook(book));
    router.push(`/books/national-library/${encodeURIComponent(book.id || book.isbn || book.title_info)}`);
  };

  return (
    <div className="nl-search-wrap">
      <h2 style={{ marginBottom: 6 }}>🏛 국립중앙도서관 도서 검색</h2>
      <p style={{ color: '#777', marginBottom: 20 }}>
        국립중앙도서관 오픈API로 도서를 검색하고, 관리자는 마음에 드는 도서를 골라 BookStore에 저장할 수 있습니다.
      </p>

      {/* 키워드 직접 검색 */}
      <form className="nl-search-box" onSubmit={onSubmit}>
        <input
          type="text"
          className="bs-form-control"
          placeholder="예: 인공지능, 역사, 철학"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <button type="submit" className="btn btn-primary-bs">검색</button>
      </form>

      {/* KDC 분류 아코디언 - 대분류 클릭시 하위 키워드 목록이 펼쳐짐 */}
      <div className="nl-kdc-title">📖 KDC 전체 분류 체계</div>
      <div className="nl-accordion">
        {KDC_GROUPS.map((group) => {
          const isOpen = expandedGroup === group.title;
          return (
            <div key={group.title} className="nl-accordion-item">
              <button
                type="button"
                className={`nl-accordion-header${isOpen ? ' open' : ''}`}
                onClick={() => toggleGroup(group.title)}
              >
                {group.title}
                <span className="nl-accordion-arrow">{isOpen ? '▲' : '▼'}</span>
              </button>

              {isOpen && (
                <div className="nl-accordion-body">
                  {group.items.map((item) => (
                    <a
                      key={item}
                      className="nl-keyword-link"
                      onClick={() => onKeywordClick(item)}
                    >
                      {item}
                    </a>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* 검색결과 */}
      {(nlLoading || nlError || nlResults.length > 0 || selectedCategory || keyword) && (
        <>
          <div className="nl-result-header">
            <h3>
              검색결과 {selectedCategory ? `(분류: ${selectedCategory})` : keyword ? `("${keyword}")` : ''}
            </h3>
          </div>

          {nlLoading && <p>검색중...</p>}
          {nlError && <p style={{ color: 'red' }}>{nlError}</p>}

          {!nlLoading && nlResults.length === 0 && (
            <div className="notice-empty">검색 결과가 없습니다.</div>
          )}

          {!nlLoading && nlResults.length > 0 && (
            <div className="nl-grid">
              {nlResults.map((book, idx) => (
                <div key={book.id || idx} className="nl-card" onClick={() => goDetail(book)}>
                  <div className="nl-card-cover">
                    {/* 표지 없음/링크깨짐 → 자동으로 기본 아이콘 표시 */}
                    <BookCoverImage src={book.bookCover} alt={book.title_info} iconSize={36} />
                  </div>
                  <div className="nl-card-body">
                    <div className="nl-card-title">{book.title_info}</div>
                    <div className="nl-card-author">{book.author_info}</div>
                    {book.kdc_name_1s && <div className="nl-card-kdc">{book.kdc_name_1s}</div>}
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
