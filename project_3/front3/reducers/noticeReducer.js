// reducers/noticeReducer.js  (SBOARD2 - 공지사항)
import { createSlice } from "@reduxjs/toolkit";

const initialState = {
    notices: [],           // 현재 페이지의 공지사항목록
    currentNotice: null,   // 단건 조회된 상세 공지사항 (조회시 BHIT 증가)
    // ★페이징 정보 (백엔드 PageResponseDto 와 대응, 12개씩)
    currentPage: 1,
    totalPages: 1,
    totalElements: 0,
    pageSize: 12,
    loading: false,
    error: null,
    success: false,
};

const noticeReducer = createSlice({
    name: "notice",
    initialState,
    reducers: {
        // --- 상태 초기화 ---
        resetNoticeState: (state) => {
            state.loading = false;
            state.error = null;
            state.success = false;
        },

        // --- 전체조회 - 페이징(page/size) ---
        fetchNoticesRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        // ★payload : 백엔드 PageResponseDto { content, currentPage, pageSize, totalElements, totalPages }
        fetchNoticesSuccess: (state, action) => {
            state.loading = false;
            state.notices = action.payload.content;
            state.currentPage = action.payload.currentPage;
            state.pageSize = action.payload.pageSize;
            state.totalElements = action.payload.totalElements;
            state.totalPages = action.payload.totalPages;
        },
        fetchNoticesFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 단건조회 (서버에서 BHIT 조회수 +1 반영됨) ---
        fetchNoticeDetailRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        fetchNoticeDetailSuccess: (state, action) => {
            state.loading = false;
            state.currentNotice = action.payload;
        },
        fetchNoticeDetailFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 제목검색 ---
        searchNoticesRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        searchNoticesSuccess: (state, action) => {
            state.loading = false;
            state.notices = action.payload;
        },
        searchNoticesFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 공지사항 작성 (관리자 전용, 백엔드 @PreAuthorize("hasRole('ADMIN')")) ---
        createNoticeRequest: (state) => {
            state.loading = true;
            state.error = null;
            state.success = false;
        },
        createNoticeSuccess: (state, action) => {
            state.loading = false;
            // ★페이징 도입 이후: 로컬에서 목록에 끼워넣지 않고, 작성 후 첫 페이지를 다시
            //   불러오는 방식(pages/notices/new.js 에서 router.push('/notices'))으로 처리합니다.
            state.success = true;
        },
        createNoticeFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 공지사항 수정 (관리자 전용) ---
        updateNoticeRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        updateNoticeSuccess: (state, action) => {
            state.loading = false;
            state.notices = state.notices.map((n) =>
                n.id === action.payload.id ? action.payload : n
            );
            state.currentNotice = action.payload;
        },
        updateNoticeFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 공지사항 삭제 (관리자 전용) ---
        deleteNoticeRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        deleteNoticeSuccess: (state, action) => {
            state.loading = false;
            state.notices = state.notices.filter((n) => n.id !== action.payload);
        },
        deleteNoticeFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },
    },
});

export const {
    resetNoticeState,
    fetchNoticesRequest, fetchNoticesSuccess, fetchNoticesFailure,
    fetchNoticeDetailRequest, fetchNoticeDetailSuccess, fetchNoticeDetailFailure,
    searchNoticesRequest, searchNoticesSuccess, searchNoticesFailure,
    createNoticeRequest, createNoticeSuccess, createNoticeFailure,
    updateNoticeRequest, updateNoticeSuccess, updateNoticeFailure,
    deleteNoticeRequest, deleteNoticeSuccess, deleteNoticeFailure,
} = noticeReducer.actions;

export default noticeReducer.reducer;
