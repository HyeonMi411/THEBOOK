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

    // 카카오페이 결제 관련 상태
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
            // currentOrder 도 함께 초기화합니다. 이걸 안 지우면, 결제완료(paymentApproveSuccess)
            // 시점에 채워진 currentOrder 가 그대로 남아있다가, 나중에 다른 화면(도서상세 등)의
            // "주문 생성 성공시 이동" 같은 로직이 이 남아있는 값을 새 주문으로 착각해서
            // 아무것도 안 눌렀는데도 자동으로 결제 흐름이 다시 실행되는 문제가 있었습니다.
            state.currentOrder = null;
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
            // action.payload.content 가 없는 경우(응답 구조가 예상과 다른 경우 등)에도
            // orders 가 undefined 가 되지 않도록 방어합니다.
            state.orders = action.payload.content || [];
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

        // --- 주문 삭제 (결제전(PENDING) 주문만 가능) ---
        deleteOrderRequest: (state) => {
            state.loading = true;
            state.error = null;
        },
        deleteOrderSuccess: (state, action) => {
            state.loading = false;
            // 삭제한 주문만 목록에서 실제로 빠지는지 - 화면 새로고침 없이 즉시 반영
            state.orders = state.orders.filter((order) => order.id !== action.payload);
        },
        deleteOrderFailure: (state, action) => {
            state.loading = false;
            state.error = action.payload;
        },

        // --- 카카오페이 결제준비 ---
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

        // --- 카카오페이 결제승인 ---
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

        // --- 카카오페이 결제취소 ---
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

        // --- 카카오페이 결제실패 ---
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
    deleteOrderRequest, deleteOrderSuccess, deleteOrderFailure,
    paymentReadyRequest, paymentReadySuccess, paymentReadyFailure,
    paymentApproveRequest, paymentApproveSuccess, paymentApproveFailure,
    paymentCancelRequest, paymentCancelSuccess, paymentCancelFailure,
    paymentFailRequest, paymentFailSuccess, paymentFailFailure,
} = orderReducer.actions;

export default orderReducer.reducer;
