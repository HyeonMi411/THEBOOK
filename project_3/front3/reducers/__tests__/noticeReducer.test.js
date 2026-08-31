// __tests__/noticeReducer.test.js
import noticeReducer, {
    resetNoticeState,
    fetchNoticesRequest, fetchNoticesSuccess, fetchNoticesFailure,
    fetchNoticeDetailRequest, fetchNoticeDetailSuccess, fetchNoticeDetailFailure,
    searchNoticesRequest, searchNoticesSuccess, searchNoticesFailure,
    createNoticeRequest, createNoticeSuccess, createNoticeFailure,
    updateNoticeRequest, updateNoticeSuccess, updateNoticeFailure,
    deleteNoticeRequest, deleteNoticeSuccess, deleteNoticeFailure,
} from '../noticeReducer';

describe('notice(SBOARD2) slice reducer', () => {
    const initialState = {
        notices: [],
        currentNotice: null,
        currentPage: 1,
        totalPages: 1,
        totalElements: 0,
        pageSize: 12,
        loading: false,
        error: null,
        success: false,
    };

    // 더미SQL 데이터와 겹치지 않도록, 테스트 전용 공지사항 제목을 사용합니다.
    const noticeA = { id: 401, btitle: '공지리듀서테스트공지A', bcontent: '내용A', bhit: 3 };
    const noticeB = { id: 402, btitle: '공지리듀서테스트공지B', bcontent: '내용B', bhit: 0 };

    it('resetNoticeState', () => {
        const prev = { ...initialState, loading: true, error: '에러', success: true };
        const state = noticeReducer(prev, resetNoticeState());
        expect(state.loading).toBe(false);
        expect(state.error).toBeNull();
        expect(state.success).toBe(false);
    });

    //////////////////////////////////////////// 전체조회 - 페이징
    it('fetchNoticesRequest', () => {
        const state = noticeReducer(initialState, fetchNoticesRequest());
        expect(state.loading).toBe(true);
        expect(state.error).toBeNull();
    });

    it('fetchNoticesSuccess - 페이징 필드가 전부 정확히 반영되는지', () => {
        const pageResponse = { content: [noticeA, noticeB], currentPage: 1, pageSize: 12, totalElements: 2, totalPages: 1 };
        const state = noticeReducer(initialState, fetchNoticesSuccess(pageResponse));
        expect(state.loading).toBe(false);
        expect(state.notices).toEqual([noticeA, noticeB]);
        expect(state.currentPage).toBe(1);
        expect(state.pageSize).toBe(12);
        expect(state.totalElements).toBe(2);
        expect(state.totalPages).toBe(1);
    });

    it('fetchNoticesFailure', () => {
        const state = noticeReducer(initialState, fetchNoticesFailure('공지사항 목록 조회 실패'));
        expect(state.loading).toBe(false);
        expect(state.error).toBe('공지사항 목록 조회 실패');
    });

    //////////////////////////////////////////// 단건조회 ( 서버에서 BHIT(조회수) +1 된 값이 정확히 반영되는지 검증 )
    it('fetchNoticeDetailRequest', () => {
        const state = noticeReducer(initialState, fetchNoticeDetailRequest());
        expect(state.loading).toBe(true);
    });

    it('fetchNoticeDetailSuccess - 서버가 조회수 +1 해서 내려준 값이 그대로 currentNotice 에 반영되는지', () => {
        // 백엔드(Sboard2Service)는 상세조회 시점에 BHIT 를 실제로 +1 하고 그 결과를 응답합니다.
        // 프론트는 그 응답값을 있는 그대로 신뢰해서 currentNotice 에 저장하기만 하면 됩니다.
        const noticeAfterView = { ...noticeA, bhit: noticeA.bhit + 1 }; // 3 -> 4 로 증가된 상태로 응답 온 것을 가정
        const state = noticeReducer(initialState, fetchNoticeDetailSuccess(noticeAfterView));
        expect(state.loading).toBe(false);
        expect(state.currentNotice).toEqual(noticeAfterView);
        expect(state.currentNotice.bhit).toBe(4); // 조회수가 실제로 증가된 값으로 반영됐는지 확인
    });

    it('fetchNoticeDetailFailure', () => {
        const state = noticeReducer(initialState, fetchNoticeDetailFailure('존재하지 않는 공지사항입니다.'));
        expect(state.error).toBe('존재하지 않는 공지사항입니다.');
    });

    //////////////////////////////////////////// 제목검색
    it('searchNoticesRequest', () => {
        const state = noticeReducer(initialState, searchNoticesRequest());
        expect(state.loading).toBe(true);
    });

    it('searchNoticesSuccess - 검색결과는 페이징 없이 배열 그대로 notices 에 저장되는지', () => {
        const state = noticeReducer(initialState, searchNoticesSuccess([noticeA]));
        expect(state.loading).toBe(false);
        expect(state.notices).toEqual([noticeA]);
    });

    it('searchNoticesFailure', () => {
        const state = noticeReducer(initialState, searchNoticesFailure('검색 실패'));
        expect(state.error).toBe('검색 실패');
    });

    //////////////////////////////////////////// 공지사항 작성 ( 관리자 전용, 백엔드 @PreAuthorize )
    it('createNoticeRequest', () => {
        const state = noticeReducer(initialState, createNoticeRequest());
        expect(state.loading).toBe(true);
        expect(state.success).toBe(false);
    });

    it('createNoticeSuccess - 목록에 바로 끼워넣지 않고 success 플래그만 세우는지', () => {
        const state = noticeReducer(initialState, createNoticeSuccess(noticeA));
        expect(state.loading).toBe(false);
        expect(state.success).toBe(true);
        expect(state.notices).toEqual([]); // 페이징 도입 이후 로컬 끼워넣기 안 함 (첫 페이지 재조회 방식)
    });

    it('createNoticeFailure - 관리자가 아니면 거부되는 상황(403) 등', () => {
        const state = noticeReducer(initialState, createNoticeFailure('접근이 거부되었습니다.'));
        expect(state.loading).toBe(false);
        expect(state.error).toBe('접근이 거부되었습니다.');
    });

    //////////////////////////////////////////// 공지사항 수정 ( 관리자 전용, 목록 갱신 로직 꼼꼼히 검증 )
    it('updateNoticeRequest', () => {
        const state = noticeReducer(initialState, updateNoticeRequest());
        expect(state.loading).toBe(true);
    });

    it('updateNoticeSuccess - 목록(notices) 중 해당 id 항목만 정확히 교체되고 currentNotice 도 갱신되는지', () => {
        const prev = { ...initialState, notices: [noticeA, noticeB] };
        const updatedA = { ...noticeA, btitle: '공지리듀서테스트공지A(수정됨)' };
        const state = noticeReducer(prev, updateNoticeSuccess(updatedA));
        expect(state.loading).toBe(false);
        expect(state.notices).toHaveLength(2);
        expect(state.notices.find((n) => n.id === noticeA.id).btitle).toBe('공지리듀서테스트공지A(수정됨)');
        expect(state.notices.find((n) => n.id === noticeB.id)).toEqual(noticeB); // noticeB 는 그대로여야 함
        expect(state.currentNotice).toEqual(updatedA);
    });

    it('updateNoticeFailure', () => {
        const state = noticeReducer(initialState, updateNoticeFailure('수정 권한이 없습니다.'));
        expect(state.error).toBe('수정 권한이 없습니다.');
    });

    //////////////////////////////////////////// 공지사항 삭제 ( 관리자 전용, 목록에서 실제로 빠지는지 검증 )
    it('deleteNoticeRequest', () => {
        const state = noticeReducer(initialState, deleteNoticeRequest());
        expect(state.loading).toBe(true);
    });

    it('deleteNoticeSuccess - 삭제한 공지사항만 목록에서 빠지는지', () => {
        const prev = { ...initialState, notices: [noticeA, noticeB] };
        const state = noticeReducer(prev, deleteNoticeSuccess(noticeA.id));
        expect(state.loading).toBe(false);
        expect(state.notices).toHaveLength(1);
        expect(state.notices[0].id).toBe(noticeB.id);
    });

    it('deleteNoticeFailure', () => {
        const state = noticeReducer(initialState, deleteNoticeFailure('삭제 권한이 없습니다.'));
        expect(state.error).toBe('삭제 권한이 없습니다.');
    });
});
// npm test noticeReducer
