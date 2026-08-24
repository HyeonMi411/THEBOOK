// reducers/orderReducer.js
import { createSlice } from "@reduxjs/toolkit";

const initialState = {
    orders: [],           // 내 주문내역 목록
    currentOrder: null,   // 단건 주문 상세(결제확인/완료 화면에서 사용)
    currentPage: 1,
    totalPages: 1,
    totalElements: 0,
    pageSize: 12,
    loading: false,
    error: null,

    // ★카카오페이 결제 관련 상태
    paymentLoading: false,
    paymentError: null,
    redirectUrl: null,      // 결제준비 성공시 이동할 카카오페이 결제창 URL
};

const orderReducer = createSlice({
    name: "order",
    initialState,
    reducers: {
        resetOrderState: (state) => {
            state.loading = false;
            state.error = null;
        },
        resetPaymentState: (state) => {
            state.paymentLoading = false;
            state.paymentError = null;
            state.redirectUrl = null;
        },

        // --- 주문 생성 ---
        createOrderRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        createOrderSuccess: (state, action) => {
            state.loading = false;
            state.currentOrder = action.payload;
        },
        createOrderFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 내 주문내역 조회 (페이징) ---
        fetchMyOrdersRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        fetchMyOrdersSuccess: (state, action) => {
            state.loading = false;
            state.orders = action.payload.content;
            state.currentPage = action.payload.currentPage;
            state.pageSize = action.payload.pageSize;
            state.totalElements = action.payload.totalElements;
            state.totalPages = action.payload.totalPages;
        },
        fetchMyOrdersFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 주문 상세 조회 ---
        fetchOrderDetailRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        fetchOrderDetailSuccess: (state, action) => {
            state.loading = false;
            state.currentOrder = action.payload;
        },
        fetchOrderDetailFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- ★카카오페이 결제준비 ---
        paymentReadyRequest: (state) => {
            state.paymentLoading = true;
            state.paymentError = null;
            state.redirectUrl = null;
        },
        paymentReadySuccess: (state, action) => {
            state.paymentLoading = false;
            state.redirectUrl = action.payload.redirectUrl;
        },
        paymentReadyFailure: (state, action) => {
            state.paymentLoading = false;
            state.paymentError = action.payload;
        },

        // --- ★카카오페이 결제승인 ---
        paymentApproveRequest: (state) => {
            state.paymentLoading = true;
            state.paymentError = null;
        },
        paymentApproveSuccess: (state, action) => {
            state.paymentLoading = false;
            state.currentOrder = action.payload;
        },
        paymentApproveFailure: (state, action) => {
            state.paymentLoading = false;
            state.paymentError = action.payload;
        },

        // --- ★카카오페이 결제취소 ---
        paymentCancelRequest: (state) => {
            state.paymentLoading = true;
            state.paymentError = null;
        },
        paymentCancelSuccess: (state) => {
            state.paymentLoading = false;
            if (state.currentOrder) state.currentOrder.orderStatus = 'CANCELLED';
        },
        paymentCancelFailure: (state, action) => {
            state.paymentLoading = false;
            state.paymentError = action.payload;
        },

        // --- ★카카오페이 결제실패 ---
        paymentFailRequest: (state) => {
            state.paymentLoading = true;
            state.paymentError = null;
        },
        paymentFailSuccess: (state) => {
            state.paymentLoading = false;
            if (state.currentOrder) state.currentOrder.orderStatus = 'FAILED';
        },
        paymentFailFailure: (state, action) => {
            state.paymentLoading = false;
            state.paymentError = action.payload;
        },
    },
});

export const {
    resetOrderState, resetPaymentState,
    createOrderRequest, createOrderSuccess, createOrderFailure,
    fetchMyOrdersRequest, fetchMyOrdersSuccess, fetchMyOrdersFailure,
    fetchOrderDetailRequest, fetchOrderDetailSuccess, fetchOrderDetailFailure,
    paymentReadyRequest, paymentReadySuccess, paymentReadyFailure,
    paymentApproveRequest, paymentApproveSuccess, paymentApproveFailure,
    paymentCancelRequest, paymentCancelSuccess, paymentCancelFailure,
    paymentFailRequest, paymentFailSuccess, paymentFailFailure,
} = orderReducer.actions;

export default orderReducer.reducer;
