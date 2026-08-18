// reducers/bookReducer.js
import { createSlice } from "@reduxjs/toolkit";

const initialState = {
    books: [],        // 현재 페이지의 도서목록
    currentBook: null, // 단건 조회된 상세 도서
    // ★페이징 정보 (백엔드 PageResponseDto 와 대응, 12개씩)
    currentPage: 1,
    totalPages: 1,
    totalElements: 0,
    pageSize: 12,
    loading: false,
    error: null,
    success: false,
    // ★카카오 도서검색 자동등록 관련 상태
    kakaoLoading: false,
    kakaoError: null,
    kakaoInsertedCount: null, // 마지막 카카오검색 등록 결과(등록된 건수)
};

const bookReducer = createSlice({
    name: "book",
    initialState,
    reducers: {
        // --- 상태 초기화 ---
        resetBookState: (state) => {
            state.loading = false;
            state.error = null;
            state.success = false;
        },

        // --- 전체(카테고리별) 조회 - 페이징(page/size/category) ---
        fetchBooksRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        // ★payload : 백엔드 PageResponseDto { content, currentPage, pageSize, totalElements, totalPages }
        fetchBooksSuccess: (state, action) => {
            state.loading = false;
            state.books = action.payload.content;
            state.currentPage = action.payload.currentPage;
            state.pageSize = action.payload.pageSize;
            state.totalElements = action.payload.totalElements;
            state.totalPages = action.payload.totalPages;
        },
        fetchBooksFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 단건 조회 ---
        fetchBookDetailRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        fetchBookDetailSuccess: (state, action) => {
            state.loading = false;
            state.currentBook = action.payload;
        },
        fetchBookDetailFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 제목검색 ---
        searchBooksRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        searchBooksSuccess: (state, action) => {
            state.loading = false;
            state.books = action.payload;
        },
        searchBooksFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 도서등록 (관리자 전용, 백엔드 @PreAuthorize("hasRole('ADMIN')")) ---
        createBookRequest: (state) => {
            state.loading = true;
            state.error = null;
            state.success = false;
        },
        createBookSuccess: (state, action) => {
            state.loading = false;
            // ★페이징 도입 이후: 로컬에서 목록에 끼워넣지 않고, 등록 후 첫 페이지를 다시
            //   불러오는 방식(pages/books/new.js 에서 router.push('/books'))으로 처리합니다.
            state.success = true;
        },
        createBookFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 도서수정 (관리자 전용) ---
        updateBookRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        updateBookSuccess: (state, action) => {
            state.loading = false;
            state.books = state.books.map((b) =>
                b.id === action.payload.id ? action.payload : b
            );
            state.currentBook = action.payload;
        },
        updateBookFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 도서삭제 (관리자 전용) ---
        deleteBookRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        deleteBookSuccess: (state, action) => {
            state.loading = false;
            state.books = state.books.filter((b) => b.id !== action.payload);
        },
        deleteBookFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- ★카카오 도서검색 자동등록 (관리자 전용) ---
        //    검색버튼을 누르면 카카오 API에서 도서를 가져와 자동으로 DB에 저장한 후,
        //    성공시 도서 목록 페이지로 이동합니다. (pages/books/new.js 에서 처리)
        kakaoInsertRequest: (state) => {
            state.kakaoLoading = true;
            state.kakaoError = null;
            state.kakaoInsertedCount = null;
        },
        kakaoInsertSuccess: (state, action) => {
            state.kakaoLoading = false;
            state.kakaoInsertedCount = action.payload.insertedCount;
        },
        kakaoInsertFailure: (state, action) => {
            state.kakaoLoading = false;
            state.kakaoError = action.payload;
        },
        resetKakaoInsertState: (state) => {
            state.kakaoLoading = false;
            state.kakaoError = null;
            state.kakaoInsertedCount = null;
        },
    },
});

export const {
    resetBookState,
    fetchBooksRequest, fetchBooksSuccess, fetchBooksFailure,
    fetchBookDetailRequest, fetchBookDetailSuccess, fetchBookDetailFailure,
    searchBooksRequest, searchBooksSuccess, searchBooksFailure,
    createBookRequest, createBookSuccess, createBookFailure,
    updateBookRequest, updateBookSuccess, updateBookFailure,
    deleteBookRequest, deleteBookSuccess, deleteBookFailure,
    kakaoInsertRequest, kakaoInsertSuccess, kakaoInsertFailure, resetKakaoInsertState,
} = bookReducer.actions;

export default bookReducer.reducer;
