// __tests__/bookReducer.test.js
import bookReducer, {
    resetBookState,
    fetchBooksRequest, fetchBooksSuccess, fetchBooksFailure,
    fetchBookDetailRequest, fetchBookDetailSuccess, fetchBookDetailFailure,
    searchBooksRequest, searchBooksSuccess, searchBooksFailure,
    createBookRequest, createBookSuccess, createBookFailure,
    updateBookRequest, updateBookSuccess, updateBookFailure,
    deleteBookRequest, deleteBookSuccess, deleteBookFailure,
    kakaoInsertRequest, kakaoInsertSuccess, kakaoInsertFailure, resetKakaoInsertState,
    nlSearchRequest, nlSearchSuccess, nlSearchFailure,
    selectNlBook, clearNlSelectedBook,
    nlSaveRequest, nlSaveSuccess, nlSaveFailure, resetNlSaveState,
    fetchBestsellersRequest, fetchBestsellersSuccess, fetchBestsellersFailure,
} from '../bookReducer';

describe('book slice reducer', () => {
    const initialState = {
        books: [],
        currentBook: null,
        currentPage: 1,
        totalPages: 1,
        totalElements: 0,
        pageSize: 12,
        loading: false,
        error: null,
        success: false,
        kakaoLoading: false,
        kakaoError: null,
        kakaoInsertedCount: null,
        nlResults: [],
        nlLoading: false,
        nlError: null,
        nlSelectedBook: null,
        nlSaveLoading: false,
        nlSaveError: null,
        nlSaveSuccess: false,
        bestsellers: [],
        bestsellersLoading: false,
        bestsellersError: null,
    };

    // 더미SQL 데이터(스프링부트 완전정복 등)와 겹치지 않도록, 테스트 전용 도서명을 사용합니다.
    const bookA = { id: 301, title: '북리듀서테스트도서A', author: '테스트작가', price: 18000, category: 'IT' };
    const bookB = { id: 302, title: '북리듀서테스트도서B', author: '테스트작가2', price: 12000, category: '소설' };

    it('resetBookState', () => {
        const prev = { ...initialState, loading: true, error: '에러', success: true };
        const state = bookReducer(prev, resetBookState());
        expect(state.loading).toBe(false);
        expect(state.error).toBeNull();
        expect(state.success).toBe(false);
    });

    //////////////////////////////////////////// 전체(카테고리별) 조회 - 페이징
    it('fetchBooksRequest', () => {
        const state = bookReducer(initialState, fetchBooksRequest());
        expect(state.loading).toBe(true);
        expect(state.error).toBeNull();
    });

    it('fetchBooksSuccess - 페이징 필드가 전부 정확히 반영되는지', () => {
        const pageResponse = { content: [bookA, bookB], currentPage: 1, pageSize: 12, totalElements: 2, totalPages: 1 };
        const state = bookReducer(initialState, fetchBooksSuccess(pageResponse));
        expect(state.loading).toBe(false);
        expect(state.books).toEqual([bookA, bookB]);
        expect(state.currentPage).toBe(1);
        expect(state.pageSize).toBe(12);
        expect(state.totalElements).toBe(2);
        expect(state.totalPages).toBe(1);
    });

    it('fetchBooksFailure', () => {
        const state = bookReducer(initialState, fetchBooksFailure('도서 목록 조회 실패'));
        expect(state.loading).toBe(false);
        expect(state.error).toBe('도서 목록 조회 실패');
    });

    //////////////////////////////////////////// 단건 조회
    it('fetchBookDetailRequest', () => {
        const state = bookReducer(initialState, fetchBookDetailRequest());
        expect(state.loading).toBe(true);
    });

    it('fetchBookDetailSuccess', () => {
        const state = bookReducer(initialState, fetchBookDetailSuccess(bookA));
        expect(state.loading).toBe(false);
        expect(state.currentBook).toEqual(bookA);
    });

    it('fetchBookDetailFailure', () => {
        const state = bookReducer(initialState, fetchBookDetailFailure('존재하지 않는 도서입니다.'));
        expect(state.error).toBe('존재하지 않는 도서입니다.');
    });

    //////////////////////////////////////////// 제목검색
    it('searchBooksRequest', () => {
        const state = bookReducer(initialState, searchBooksRequest());
        expect(state.loading).toBe(true);
    });

    it('searchBooksSuccess - 검색결과는 페이징 없이 배열 그대로 books 에 저장되는지', () => {
        const state = bookReducer(initialState, searchBooksSuccess([bookA]));
        expect(state.loading).toBe(false);
        expect(state.books).toEqual([bookA]);
    });

    it('searchBooksFailure', () => {
        const state = bookReducer(initialState, searchBooksFailure('검색 실패'));
        expect(state.error).toBe('검색 실패');
    });

    //////////////////////////////////////////// 도서등록 ( 관리자 전용, 백엔드 @PreAuthorize )
    it('createBookRequest', () => {
        const state = bookReducer(initialState, createBookRequest());
        expect(state.loading).toBe(true);
        expect(state.success).toBe(false);
    });

    it('createBookSuccess - 목록에 바로 끼워넣지 않고 success 플래그만 세우는지', () => {
        const state = bookReducer(initialState, createBookSuccess(bookA));
        expect(state.loading).toBe(false);
        expect(state.success).toBe(true);
        expect(state.books).toEqual([]); // 페이징 도입 이후 로컬 끼워넣기 안 함 (첫 페이지 재조회 방식)
    });

    it('createBookFailure - 관리자가 아니면 거부되는 상황(403) 등', () => {
        const state = bookReducer(initialState, createBookFailure('접근이 거부되었습니다.'));
        expect(state.loading).toBe(false);
        expect(state.error).toBe('접근이 거부되었습니다.');
    });

    //////////////////////////////////////////// 도서수정 ( 관리자 전용, 목록 갱신 로직 꼼꼼히 검증 )
    it('updateBookRequest', () => {
        const state = bookReducer(initialState, updateBookRequest());
        expect(state.loading).toBe(true);
    });

    it('updateBookSuccess - 목록(books) 중 해당 id 항목만 정확히 교체되고 currentBook 도 갱신되는지', () => {
        const prev = { ...initialState, books: [bookA, bookB] };
        const updatedA = { ...bookA, title: '북리듀서테스트도서A(수정됨)', price: 20000 };
        const state = bookReducer(prev, updateBookSuccess(updatedA));
        expect(state.loading).toBe(false);
        expect(state.books).toHaveLength(2);
        expect(state.books.find((b) => b.id === bookA.id).title).toBe('북리듀서테스트도서A(수정됨)');
        expect(state.books.find((b) => b.id === bookB.id)).toEqual(bookB); // bookB 는 그대로여야 함
        expect(state.currentBook).toEqual(updatedA);
    });

    it('updateBookFailure', () => {
        const state = bookReducer(initialState, updateBookFailure('수정 권한이 없습니다.'));
        expect(state.error).toBe('수정 권한이 없습니다.');
    });

    //////////////////////////////////////////// 도서삭제 ( 관리자 전용, 목록에서 실제로 빠지는지 검증 )
    it('deleteBookRequest', () => {
        const state = bookReducer(initialState, deleteBookRequest());
        expect(state.loading).toBe(true);
    });

    it('deleteBookSuccess - 삭제한 도서만 목록에서 빠지는지', () => {
        const prev = { ...initialState, books: [bookA, bookB] };
        const state = bookReducer(prev, deleteBookSuccess(bookA.id));
        expect(state.loading).toBe(false);
        expect(state.books).toHaveLength(1);
        expect(state.books[0].id).toBe(bookB.id);
    });

    it('deleteBookFailure', () => {
        const state = bookReducer(initialState, deleteBookFailure('삭제 권한이 없습니다.'));
        expect(state.error).toBe('삭제 권한이 없습니다.');
    });

    //////////////////////////////////////////// 카카오 도서검색 자동등록 (관리자 전용)
    it('kakaoInsertRequest', () => {
        const prev = { ...initialState, kakaoInsertedCount: 3 };
        const state = bookReducer(prev, kakaoInsertRequest());
        expect(state.kakaoLoading).toBe(true);
        expect(state.kakaoError).toBeNull();
        expect(state.kakaoInsertedCount).toBeNull();
    });

    it('kakaoInsertSuccess - 등록건수가 정확히 반영되는지', () => {
        const payload = { search: '북리듀서테스트도서', insertedCount: 5 };
        const state = bookReducer(initialState, kakaoInsertSuccess(payload));
        expect(state.kakaoLoading).toBe(false);
        expect(state.kakaoInsertedCount).toBe(5);
    });

    it('kakaoInsertFailure', () => {
        const state = bookReducer(initialState, kakaoInsertFailure('카카오 API 호출 실패'));
        expect(state.kakaoError).toBe('카카오 API 호출 실패');
    });

    it('resetKakaoInsertState', () => {
        const prev = { ...initialState, kakaoLoading: true, kakaoError: '에러', kakaoInsertedCount: 5 };
        const state = bookReducer(prev, resetKakaoInsertState());
        expect(state.kakaoLoading).toBe(false);
        expect(state.kakaoError).toBeNull();
        expect(state.kakaoInsertedCount).toBeNull();
    });

    //////////////////////////////////////////// 국립중앙도서관 도서검색 (조회전용, 로그인 불필요)
    it('nlSearchRequest', () => {
        const state = bookReducer(initialState, nlSearchRequest());
        expect(state.nlLoading).toBe(true);
    });

    it('nlSearchSuccess', () => {
        const nlResults = [{ id: 'nl1', title_info: '북리듀서테스트도서NL', author_info: '국중도테스트작가' }];
        const state = bookReducer(initialState, nlSearchSuccess(nlResults));
        expect(state.nlLoading).toBe(false);
        expect(state.nlResults).toEqual(nlResults);
    });

    it('nlSearchFailure', () => {
        const state = bookReducer(initialState, nlSearchFailure('국립중앙도서관 검색 실패'));
        expect(state.nlError).toBe('국립중앙도서관 검색 실패');
    });

    it('selectNlBook - 목록에서 클릭한 도서가 상세화면용으로 저장되는지', () => {
        const nlBook = { id: 'nl1', title_info: '북리듀서테스트도서NL' };
        const state = bookReducer(initialState, selectNlBook(nlBook));
        expect(state.nlSelectedBook).toEqual(nlBook);
    });

    it('clearNlSelectedBook', () => {
        const prev = { ...initialState, nlSelectedBook: { id: 'nl1' } };
        const state = bookReducer(prev, clearNlSelectedBook());
        expect(state.nlSelectedBook).toBeNull();
    });

    //////////////////////////////////////////// 국립중앙도서관 검색결과 저장 (관리자 전용)
    it('nlSaveRequest', () => {
        const state = bookReducer(initialState, nlSaveRequest());
        expect(state.nlSaveLoading).toBe(true);
        expect(state.nlSaveSuccess).toBe(false);
    });

    it('nlSaveSuccess', () => {
        const state = bookReducer(initialState, nlSaveSuccess());
        expect(state.nlSaveLoading).toBe(false);
        expect(state.nlSaveSuccess).toBe(true);
    });

    it('nlSaveFailure - 이미 등록된 제목이면 거부되는지', () => {
        const state = bookReducer(initialState, nlSaveFailure('이미 등록된 도서입니다: 북리듀서테스트도서A'));
        expect(state.nlSaveError).toBe('이미 등록된 도서입니다: 북리듀서테스트도서A');
    });

    it('resetNlSaveState', () => {
        const prev = { ...initialState, nlSaveLoading: true, nlSaveError: '에러', nlSaveSuccess: true };
        const state = bookReducer(prev, resetNlSaveState());
        expect(state.nlSaveLoading).toBe(false);
        expect(state.nlSaveError).toBeNull();
        expect(state.nlSaveSuccess).toBe(false);
    });

    //////////////////////////////////////////// 베스트셀러(판매량 TOP 10)
    it('fetchBestsellersRequest', () => {
        const state = bookReducer(initialState, fetchBestsellersRequest());
        expect(state.bestsellersLoading).toBe(true);
        expect(state.bestsellersError).toBeNull();
    });

    it('fetchBestsellersSuccess - 순위/판매량이 포함된 목록이 그대로 저장되는지', () => {
        const bestsellers = [
            { rank: 1, soldQuantity: 15, book: { id: 301, title: '북리듀서테스트도서A' } },
            { rank: 2, soldQuantity: 9, book: { id: 302, title: '북리듀서테스트도서B' } },
        ];
        const state = bookReducer(initialState, fetchBestsellersSuccess(bestsellers));
        expect(state.bestsellersLoading).toBe(false);
        expect(state.bestsellers).toEqual(bestsellers);
        expect(state.bestsellers[0].rank).toBe(1);
        expect(state.bestsellers[0].soldQuantity).toBe(15);
    });

    it('fetchBestsellersFailure', () => {
        const state = bookReducer(initialState, fetchBestsellersFailure('베스트셀러 조회 실패'));
        expect(state.bestsellersLoading).toBe(false);
        expect(state.bestsellersError).toBe('베스트셀러 조회 실패');
    });
});
// npm test bookReducer
