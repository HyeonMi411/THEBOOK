// reducers/bookReducer.js
import { createSlice } from "@reduxjs/toolkit";

const initialState = {
    books: [],        // 전체 도서목록
    currentBook: null, // 단건 조회된 상세 도서
    loading: false,
    error: null,
    success: false,
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

        // --- 전체(카테고리별) 조회 ---
        fetchBooksRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        fetchBooksSuccess: (state, action) => {
            state.loading = false;
            state.books = action.payload;
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
            state.books.unshift(action.payload); // 새 도서 맨앞에 추가
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
} = bookReducer.actions;

export default bookReducer.reducer;
