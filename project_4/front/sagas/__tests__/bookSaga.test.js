// sagas/__tests__/bookSaga.test.js
// call - 동기 - 제너레이터함수 function* 일시중단 후 결과물 받기
// put  - redux 액션처리
import { call, put } from 'redux-saga/effects';
import {
    fetchBooksRequest, fetchBooksSuccess, fetchBooksFailure,
    fetchBookDetailRequest, fetchBookDetailSuccess, fetchBookDetailFailure,
    searchBooksRequest, searchBooksSuccess, searchBooksFailure,
    createBookRequest, createBookSuccess, createBookFailure,
    updateBookRequest, updateBookSuccess, updateBookFailure,
    deleteBookRequest, deleteBookSuccess, deleteBookFailure,
    kakaoInsertRequest, kakaoInsertSuccess, kakaoInsertFailure,
    nlSearchRequest, nlSearchSuccess, nlSearchFailure,
    nlSaveRequest, nlSaveSuccess, nlSaveFailure,
    fetchBestsellersRequest, fetchBestsellersSuccess, fetchBestsellersFailure,
} from '../../reducers/bookReducer';
import {
    fetchBooks, fetchBookDetail, searchBooks,
    createBook, updateBook, deleteBook,
    kakaoInsert, nlSearch, nlSave,
    fetchBestsellers,
} from '../bookSaga';

// authSaga.test.js 와 동일한 이유로 jest.mock('axios') 를 쓰지 않음.
// generator.next() 로 saga 를 한단계씩 직접 실행시키면서 CALL 이펙트에 가짜 응답을
// 수동으로 넣어주는 방식이라, 실제 axios 인스턴스가 네트워크를 타지 않음.

// 더미SQL 데이터(스프링부트 완전정복 등)와 겹치지 않도록, 테스트 전용 도서명을 사용.
const bookA = { id: 301, title: '북사가테스트도서A', author: '테스트작가', price: 18000, category: 'IT' };

describe('book saga', () => {
    afterEach(() => { jest.clearAllMocks(); });

    // -- 전체(카테고리별) 조회 - 페이징 --
    it('fetchBooks success', () => {
        const action = fetchBooksRequest({ page: 1, size: 12 });
        const generator = fetchBooks(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = { data: { content: [bookA], currentPage: 1, pageSize: 12, totalElements: 1, totalPages: 1 } };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(fetchBooksSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('fetchBooks failure', () => {
        const action = fetchBooksRequest({ page: 1, size: 12 });
        const generator = fetchBooks(action);

        generator.next();

        const mockError = { response: { data: { message: '도서 목록 조회 실패' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(fetchBooksFailure('도서 목록 조회 실패')));
        expect(generator.next().done).toBe(true);
    });

    // -- 단건 조회 --
    it('fetchBookDetail success', () => {
        const action = fetchBookDetailRequest(301);
        const generator = fetchBookDetail(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = { data: bookA };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(fetchBookDetailSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('fetchBookDetail failure - 존재하지 않는 도서', () => {
        const action = fetchBookDetailRequest(999);
        const generator = fetchBookDetail(action);

        generator.next();

        const mockError = { response: { data: { message: '존재하지 않는 도서입니다. ID : 999' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(fetchBookDetailFailure('존재하지 않는 도서입니다. ID : 999')));
        expect(generator.next().done).toBe(true);
    });

    // -- 제목검색 --
    it('searchBooks success', () => {
        const action = searchBooksRequest('북사가테스트');
        const generator = searchBooks(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = { data: [bookA] };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(searchBooksSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('searchBooks failure', () => {
        const action = searchBooksRequest('없는도서');
        const generator = searchBooks(action);

        generator.next();

        const mockError = { response: { data: { message: '검색 실패' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(searchBooksFailure('검색 실패')));
        expect(generator.next().done).toBe(true);
    });

    // -- 도서등록 ( 관리자 전용, 백엔드 @PreAuthorize("hasRole('ADMIN')") ) --
    it('createBook success', () => {
        const payload = { dto: { title: '북사가테스트도서A', author: '테스트작가', price: 18000 }, cover: null };
        const action = createBookRequest(payload);
        const generator = createBook(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = { data: bookA };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(createBookSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('createBook failure - 관리자가 아니면 403 등으로 거부되는지', () => {
        const payload = { dto: { title: '북사가테스트도서A' }, cover: null };
        const action = createBookRequest(payload);
        const generator = createBook(action);

        generator.next();

        const mockError = { response: { data: { message: '접근이 거부되었습니다.' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(createBookFailure('접근이 거부되었습니다.')));
        expect(generator.next().done).toBe(true);
    });

    // -- 도서수정 ( 관리자 전용 ) --
    it('updateBook success', () => {
        const payload = { bookId: 301, dto: { title: '북사가테스트도서A(수정)' }, cover: null };
        const action = updateBookRequest(payload);
        const generator = updateBook(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const updated = { ...bookA, title: '북사가테스트도서A(수정)' };
        const mockResponse = { data: updated };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(updateBookSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('updateBook failure', () => {
        const payload = { bookId: 301, dto: {}, cover: null };
        const action = updateBookRequest(payload);
        const generator = updateBook(action);

        generator.next();

        const mockError = { response: { data: { message: '수정 권한이 없습니다.' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(updateBookFailure('수정 권한이 없습니다.')));
        expect(generator.next().done).toBe(true);
    });

    // -- 도서삭제 ( 관리자 전용 ) -- ( API 응답값 자체는 안 쓰고 action.payload(id)를 그대로 success에 담아 dispatch )
    it('deleteBook success', () => {
        const bookId = 301;
        const action = deleteBookRequest(bookId);
        const generator = deleteBook(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const putStep = generator.next(undefined).value;

        expect(putStep).toEqual(put(deleteBookSuccess(bookId)));
        expect(generator.next().done).toBe(true);
    });

    it('deleteBook failure', () => {
        const action = deleteBookRequest(301);
        const generator = deleteBook(action);

        generator.next();

        const mockError = { response: { data: { message: '삭제 권한이 없습니다.' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(deleteBookFailure('삭제 권한이 없습니다.')));
        expect(generator.next().done).toBe(true);
    });

    // -- 카카오 도서검색 자동등록 ( 관리자 전용 ) --
    it('kakaoInsert success - 등록건수가 정확히 전달되는지', () => {
        const action = kakaoInsertRequest('북사가테스트');
        const generator = kakaoInsert(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = { data: { search: '북사가테스트', insertedCount: 3 } };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(kakaoInsertSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('kakaoInsert failure', () => {
        const action = kakaoInsertRequest('북사가테스트');
        const generator = kakaoInsert(action);

        generator.next();

        const mockError = { response: { data: { message: '카카오 API 호출 실패' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(kakaoInsertFailure('카카오 API 호출 실패')));
        expect(generator.next().done).toBe(true);
    });

    // -- 국립중앙도서관 도서검색 (조회전용, 로그인 불필요) --
    it('nlSearch success', () => {
        const action = nlSearchRequest({ keyword: '북사가테스트', page: 1 });
        const generator = nlSearch(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = { data: [{ id: 'nl1', title_info: '북사가테스트도서NL' }] };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(nlSearchSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('nlSearch failure', () => {
        const action = nlSearchRequest({ keyword: '북사가테스트', page: 1 });
        const generator = nlSearch(action);

        generator.next();

        const mockError = { response: { data: { message: '국립중앙도서관 검색 실패' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(nlSearchFailure('국립중앙도서관 검색 실패')));
        expect(generator.next().done).toBe(true);
    });

    // -- 국립중앙도서관 검색결과 저장 ( 관리자 전용 ) -- ( API 응답값을 쓰지 않고 성공여부만 dispatch )
    it('nlSave success', () => {
        const nlBook = { id: 'nl1', title_info: '북사가테스트도서NL', author_info: '국중도테스트작가' };
        const action = nlSaveRequest(nlBook);
        const generator = nlSave(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const putStep = generator.next().value;

        expect(putStep).toEqual(put(nlSaveSuccess()));
        expect(generator.next().done).toBe(true);
    });

    it('nlSave failure - 이미 등록된 제목이면 거부되는지', () => {
        const nlBook = { id: 'nl1', title_info: '북사가테스트도서NL' };
        const action = nlSaveRequest(nlBook);
        const generator = nlSave(action);

        generator.next();

        const mockError = { response: { data: { message: '이미 등록된 도서입니다: 북사가테스트도서NL' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(nlSaveFailure('이미 등록된 도서입니다: 북사가테스트도서NL')));
        expect(generator.next().done).toBe(true);
    });

    // -- 베스트셀러(판매량 TOP 10) -- ( 파라미터 없는 saga - action 인자를 안 받음 )
    it('fetchBestsellers success', () => {
        const generator = fetchBestsellers();

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = {
            data: [
                { rank: 1, soldQuantity: 15, book: { id: 301, title: '북사가테스트도서A' } },
            ],
        };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(fetchBestsellersSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('fetchBestsellers failure', () => {
        const generator = fetchBestsellers();

        generator.next();

        const mockError = { response: { data: { message: '베스트셀러 조회 실패' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(fetchBestsellersFailure('베스트셀러 조회 실패')));
        expect(generator.next().done).toBe(true);
    });
});

// npm test bookSaga.test.js
