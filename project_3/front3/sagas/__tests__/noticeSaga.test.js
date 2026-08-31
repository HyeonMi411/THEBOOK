// sagas/__tests__/noticeSaga.test.js
// call - 동기 - 제너레이터함수 function* 일시중단 후 결과물 받기
// put  - redux 액션처리
import { call, put } from 'redux-saga/effects';
import {
    fetchNoticesRequest, fetchNoticesSuccess, fetchNoticesFailure,
    fetchNoticeDetailRequest, fetchNoticeDetailSuccess, fetchNoticeDetailFailure,
    searchNoticesRequest, searchNoticesSuccess, searchNoticesFailure,
    createNoticeRequest, createNoticeSuccess, createNoticeFailure,
    updateNoticeRequest, updateNoticeSuccess, updateNoticeFailure,
    deleteNoticeRequest, deleteNoticeSuccess, deleteNoticeFailure,
} from '../../reducers/noticeReducer';
import {
    fetchNotices, fetchNoticeDetail, searchNotices,
    createNotice, updateNotice, deleteNotice,
} from '../noticeSaga';

// authSaga.test.js 와 동일한 이유로 jest.mock('axios') 를 쓰지 않습니다.
//  generator.next() 로 saga 를 한단계씩 직접 실행시키면서 CALL 이펙트에 가짜 응답을
//  수동으로 넣어주는 방식이라, 실제 axios 인스턴스가 네트워크를 타지 않습니다.

// 더미SQL 데이터와 겹치지 않도록, 테스트 전용 공지사항 제목을 사용합니다.
const noticeA = { id: 401, btitle: '공지사가테스트공지A', bcontent: '내용A', bhit: 5 };

describe('notice(SBOARD2) saga', () => {
    afterEach(() => { jest.clearAllMocks(); });

    // -- 전체조회 - 페이징 --
    it('fetchNotices success', () => {
        const action = fetchNoticesRequest({ page: 1, size: 12 });
        const generator = fetchNotices(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = { data: { content: [noticeA], currentPage: 1, pageSize: 12, totalElements: 1, totalPages: 1 } };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(fetchNoticesSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('fetchNotices failure', () => {
        const action = fetchNoticesRequest({ page: 1, size: 12 });
        const generator = fetchNotices(action);

        generator.next();

        const mockError = { response: { data: { message: '공지사항 목록 조회 실패' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(fetchNoticesFailure('공지사항 목록 조회 실패')));
        expect(generator.next().done).toBe(true);
    });

    // -- 단건조회 ( 서버가 BHIT(조회수)를 실제로 +1 해서 내려준 응답이 saga 를 거쳐 정확히 그대로 dispatch 되는지 검증 ) --
    it('fetchNoticeDetail success - 조회수가 +1 증가된 응답이 그대로 전달되는지', () => {
        const action = fetchNoticeDetailRequest(401);
        const generator = fetchNoticeDetail(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        // 백엔드가 상세조회 시점에 BHIT 를 실제로 DB에서 +1 처리한 뒤 그 결과를 응답합니다.
        // (5 -> 6 으로 증가된 상태로 응답이 온다고 가정)
        const noticeAfterView = { ...noticeA, bhit: noticeA.bhit + 1 };
        const mockResponse = { data: noticeAfterView };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(fetchNoticeDetailSuccess(mockResponse.data)));
        // saga 가 응답값을 누락/변형 없이 그대로 전달하는지, 증가된 조회수 값으로 재확인
        expect(mockResponse.data.bhit).toBe(6);
        expect(generator.next().done).toBe(true);
    });

    it('fetchNoticeDetail failure - 존재하지 않는 공지사항', () => {
        const action = fetchNoticeDetailRequest(999);
        const generator = fetchNoticeDetail(action);

        generator.next();

        const mockError = { response: { data: { message: '존재하지 않는 공지사항입니다. ID : 999' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(fetchNoticeDetailFailure('존재하지 않는 공지사항입니다. ID : 999')));
        expect(generator.next().done).toBe(true);
    });

    // -- 제목검색 --
    it('searchNotices success', () => {
        const action = searchNoticesRequest('공지사가테스트');
        const generator = searchNotices(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = { data: [noticeA] };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(searchNoticesSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('searchNotices failure', () => {
        const action = searchNoticesRequest('없는공지');
        const generator = searchNotices(action);

        generator.next();

        const mockError = { response: { data: { message: '검색 실패' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(searchNoticesFailure('검색 실패')));
        expect(generator.next().done).toBe(true);
    });

    // -- 공지사항 작성 ( 관리자 전용, 백엔드 @PreAuthorize("hasRole('ADMIN')") ) --
    it('createNotice success', () => {
        const payload = { dto: { btitle: '공지사가테스트공지A', bcontent: '내용A' }, file: null };
        const action = createNoticeRequest(payload);
        const generator = createNotice(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = { data: noticeA };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(createNoticeSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('createNotice failure - 관리자가 아니면 403 등으로 거부되는지', () => {
        const payload = { dto: { btitle: '공지사가테스트공지A' }, file: null };
        const action = createNoticeRequest(payload);
        const generator = createNotice(action);

        generator.next();

        const mockError = { response: { data: { message: '접근이 거부되었습니다.' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(createNoticeFailure('접근이 거부되었습니다.')));
        expect(generator.next().done).toBe(true);
    });

    // -- 공지사항 수정 ( 관리자 전용 ) --
    it('updateNotice success', () => {
        const payload = { noticeId: 401, dto: { btitle: '공지사가테스트공지A(수정)' }, file: null };
        const action = updateNoticeRequest(payload);
        const generator = updateNotice(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const updated = { ...noticeA, btitle: '공지사가테스트공지A(수정)' };
        const mockResponse = { data: updated };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(updateNoticeSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('updateNotice failure', () => {
        const payload = { noticeId: 401, dto: {}, file: null };
        const action = updateNoticeRequest(payload);
        const generator = updateNotice(action);

        generator.next();

        const mockError = { response: { data: { message: '수정 권한이 없습니다.' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(updateNoticeFailure('수정 권한이 없습니다.')));
        expect(generator.next().done).toBe(true);
    });

    // -- 공지사항 삭제 ( 관리자 전용 ) -- ( API 응답값 자체는 안 쓰고 action.payload(id)를 그대로 success에 담아 dispatch )
    it('deleteNotice success', () => {
        const noticeId = 401;
        const action = deleteNoticeRequest(noticeId);
        const generator = deleteNotice(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const putStep = generator.next(undefined).value;

        expect(putStep).toEqual(put(deleteNoticeSuccess(noticeId)));
        expect(generator.next().done).toBe(true);
    });

    it('deleteNotice failure', () => {
        const action = deleteNoticeRequest(401);
        const generator = deleteNotice(action);

        generator.next();

        const mockError = { response: { data: { message: '삭제 권한이 없습니다.' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(deleteNoticeFailure('삭제 권한이 없습니다.')));
        expect(generator.next().done).toBe(true);
    });
});

// npm test noticeSaga.test.js
