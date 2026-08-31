// sagas/orderSaga.js
import { all, call, put, takeLatest } from 'redux-saga/effects';
import api from '../api/axios';
import {
    createOrderRequest, createOrderSuccess, createOrderFailure,
    fetchMyOrdersRequest, fetchMyOrdersSuccess, fetchMyOrdersFailure,
    fetchOrderDetailRequest, fetchOrderDetailSuccess, fetchOrderDetailFailure,
    deleteOrderRequest, deleteOrderSuccess, deleteOrderFailure,
    paymentReadyRequest, paymentReadySuccess, paymentReadyFailure,
    paymentApproveRequest, paymentApproveSuccess, paymentApproveFailure,
    paymentCancelRequest, paymentCancelSuccess, paymentCancelFailure,
    paymentFailRequest, paymentFailSuccess, paymentFailFailure,
} from '../reducers/orderReducer';

const ORDER_API_BASE = '/api/orders';
const PAYMENT_API_BASE = '/api/payments/kakao';

// POST /api/orders - 주문생성 (action.payload : { cartItemIds } 또는 { bookId, quantity })
export const createOrderAPI = (payload) => api.post(ORDER_API_BASE, payload);
export function* createOrder(action) {
    try {
        const result = yield call(createOrderAPI, action.payload);
        yield put(createOrderSuccess(result.data));
    } catch (err) {
        yield put(createOrderFailure(err.response?.data?.error || err.message));
    }
}

// GET /api/orders?page=&size= - 내 주문내역 (12개씩 페이징)
export const fetchMyOrdersAPI = (params = {}) =>
    api.get(ORDER_API_BASE, { params: { page: params.page || 1, size: params.size || 12 } });
export function* fetchMyOrders(action) {
    try {
        const result = yield call(fetchMyOrdersAPI, action.payload);
        yield put(fetchMyOrdersSuccess(result.data));
    } catch (err) {
        yield put(fetchMyOrdersFailure(err.response?.data?.error || err.message));
    }
}

// GET /api/orders/{id} - 주문 상세
export const fetchOrderDetailAPI = (id) => api.get(`${ORDER_API_BASE}/${id}`);
export function* fetchOrderDetail(action) {
    try {
        const result = yield call(fetchOrderDetailAPI, action.payload);
        yield put(fetchOrderDetailSuccess(result.data));
    } catch (err) {
        yield put(fetchOrderDetailFailure(err.response?.data?.error || err.message));
    }
}

// DELETE /api/orders/{id} - 주문 삭제 (action.payload : orderId, 결제전(PENDING) 주문만 가능)
export const deleteOrderAPI = (id) => api.delete(`${ORDER_API_BASE}/${id}`);
export function* deleteOrder(action) {
    try {
        yield call(deleteOrderAPI, action.payload);
        yield put(deleteOrderSuccess(action.payload)); // API 응답 body 는 없으므로(204), orderId 를 그대로 담아 dispatch
    } catch (err) {
        yield put(deleteOrderFailure(err.response?.data?.error || err.message));
    }
}

// POST /api/payments/kakao/ready - 결제준비 (action.payload : orderId)
export const paymentReadyAPI = (orderId) => api.post(`${PAYMENT_API_BASE}/ready`, { orderId });
export function* paymentReady(action) {
    try {
        const result = yield call(paymentReadyAPI, action.payload);
        yield put(paymentReadySuccess(result.data));
    } catch (err) {
        yield put(paymentReadyFailure(err.response?.data?.error || err.message));
    }
}

// POST /api/payments/kakao/approve - 결제승인 (action.payload : { orderId, pgToken })
export const paymentApproveAPI = ({ orderId, pgToken }) =>
    api.post(`${PAYMENT_API_BASE}/approve`, { orderId, pgToken });
export function* paymentApprove(action) {
    try {
        const result = yield call(paymentApproveAPI, action.payload);
        yield put(paymentApproveSuccess(result.data));
    } catch (err) {
        yield put(paymentApproveFailure(err.response?.data?.error || err.message));
    }
}

// POST /api/payments/kakao/cancel/{orderId} - 결제취소 (action.payload : orderId)
export const paymentCancelAPI = (orderId) => api.post(`${PAYMENT_API_BASE}/cancel/${orderId}`);
export function* paymentCancel(action) {
    try {
        yield call(paymentCancelAPI, action.payload);
        yield put(paymentCancelSuccess());
    } catch (err) {
        yield put(paymentCancelFailure(err.response?.data?.error || err.message));
    }
}

// POST /api/payments/kakao/fail/{orderId} - 결제실패 (action.payload : orderId)
export const paymentFailAPI = (orderId) => api.post(`${PAYMENT_API_BASE}/fail/${orderId}`);
export function* paymentFail(action) {
    try {
        yield call(paymentFailAPI, action.payload);
        yield put(paymentFailSuccess());
    } catch (err) {
        yield put(paymentFailFailure(err.response?.data?.error || err.message));
    }
}

function* watchCreateOrder() {       yield takeLatest(createOrderRequest.type,       createOrder); }
function* watchFetchMyOrders() {     yield takeLatest(fetchMyOrdersRequest.type,     fetchMyOrders); }
function* watchFetchOrderDetail() {  yield takeLatest(fetchOrderDetailRequest.type,  fetchOrderDetail); }
function* watchDeleteOrder() {       yield takeLatest(deleteOrderRequest.type,       deleteOrder); }
function* watchPaymentReady() {      yield takeLatest(paymentReadyRequest.type,      paymentReady); }
function* watchPaymentApprove() {    yield takeLatest(paymentApproveRequest.type,    paymentApprove); }
function* watchPaymentCancel() {     yield takeLatest(paymentCancelRequest.type,     paymentCancel); }
function* watchPaymentFail() {       yield takeLatest(paymentFailRequest.type,       paymentFail); }

export default function* orderSaga() {
    yield all([
        call(watchCreateOrder),
        call(watchFetchMyOrders),
        call(watchFetchOrderDetail),
        call(watchDeleteOrder),
        call(watchPaymentReady),
        call(watchPaymentApprove),
        call(watchPaymentCancel),
        call(watchPaymentFail),
    ]);
}
