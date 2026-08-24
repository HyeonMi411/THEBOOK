// reducers/cartReducer.js
import { createSlice } from "@reduxjs/toolkit";

const initialState = {
    items: [],          // 장바구니 항목 목록
    totalAmount: 0,      // 총 금액
    loading: false,
    error: null,
};

const cartReducer = createSlice({
    name: "cart",
    initialState,
    reducers: {
        resetCartError: (state) => {
            state.error = null;
        },

        // --- 장바구니 조회 ---
        fetchCartRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        fetchCartSuccess: (state, action) => {
            state.loading = false;
            state.items = action.payload.items || [];
            state.totalAmount = action.payload.totalAmount || 0;
        },
        fetchCartFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 담기 ---
        addToCartRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        addToCartSuccess: (state, action) => {
            state.loading = false;
            state.items = action.payload.items || [];
            state.totalAmount = action.payload.totalAmount || 0;
        },
        addToCartFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 수량수정 ---
        updateCartItemRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        updateCartItemSuccess: (state, action) => {
            state.loading = false;
            state.items = action.payload.items || [];
            state.totalAmount = action.payload.totalAmount || 0;
        },
        updateCartItemFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 항목삭제 ---
        removeCartItemRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        removeCartItemSuccess: (state, action) => {
            state.loading = false;
            state.items = state.items.filter((item) => item.id !== action.payload);
            state.totalAmount = state.items.reduce((sum, item) => sum + item.subtotal, 0);
        },
        removeCartItemFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 전체비우기 ---
        clearCartRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        clearCartSuccess: (state) => {
            state.loading = false;
            state.items = [];
            state.totalAmount = 0;
        },
        clearCartFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 결제할 항목 선택(체크박스) - 프론트 전용 상태 ---
        toggleSelectItem: (state, action) => {
            const id = action.payload;
            if (!state.selectedIds) state.selectedIds = [];
            state.selectedIds = state.selectedIds.includes(id)
                ? state.selectedIds.filter((sid) => sid !== id)
                : [...state.selectedIds, id];
        },
        selectAllItems: (state) => {
            state.selectedIds = state.items.map((item) => item.id);
        },
        clearSelection: (state) => {
            state.selectedIds = [];
        },
    },
});

export const {
    resetCartError,
    fetchCartRequest, fetchCartSuccess, fetchCartFailure,
    addToCartRequest, addToCartSuccess, addToCartFailure,
    updateCartItemRequest, updateCartItemSuccess, updateCartItemFailure,
    removeCartItemRequest, removeCartItemSuccess, removeCartItemFailure,
    clearCartRequest, clearCartSuccess, clearCartFailure,
    toggleSelectItem, selectAllItems, clearSelection,
} = cartReducer.actions;

export default cartReducer.reducer;
