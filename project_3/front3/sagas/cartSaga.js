// sagas/cartSaga.js
import { all, call, put, takeLatest } from 'redux-saga/effects';
import api from '../api/axios';
import {
    fetchCartRequest, fetchCartSuccess, fetchCartFailure,
    addToCartRequest, addToCartSuccess, addToCartFailure,
    updateCartItemRequest, updateCartItemSuccess, updateCartItemFailure,
    removeCartItemRequest, removeCartItemSuccess, removeCartItemFailure,
    clearCartRequest, clearCartSuccess, clearCartFailure,
} from '../reducers/cartReducer';

const CART_API_BASE = '/api/cart';

// GET /api/cart - 장바구니 조회
export const fetchCartAPI = () => api.get(CART_API_BASE);
export function* fetchCart() {
    try {
        const result = yield call(fetchCartAPI);
        yield put(fetchCartSuccess(result.data));
    } catch (err) {
        yield put(fetchCartFailure(err.response?.data?.error || err.message));
    }
}

// POST /api/cart - 담기 (action.payload : { bookId, quantity })
export const addToCartAPI = (payload) => api.post(CART_API_BASE, payload);
export function* addToCart(action) {
    try {
        const result = yield call(addToCartAPI, action.payload);
        yield put(addToCartSuccess(result.data));
    } catch (err) {
        yield put(addToCartFailure(err.response?.data?.error || err.message));
    }
}

// PATCH /api/cart/{itemId} - 수량수정 (action.payload : { itemId, quantity })
export const updateCartItemAPI = ({ itemId, quantity }) =>
    api.patch(`${CART_API_BASE}/${itemId}`, { quantity });
export function* updateCartItem(action) {
    try {
        const result = yield call(updateCartItemAPI, action.payload);
        yield put(updateCartItemSuccess(result.data));
    } catch (err) {
        yield put(updateCartItemFailure(err.response?.data?.error || err.message));
    }
}

// DELETE /api/cart/{itemId} - 항목삭제 (action.payload : itemId)
export const removeCartItemAPI = (itemId) => api.delete(`${CART_API_BASE}/${itemId}`);
export function* removeCartItem(action) {
    try {
        yield call(removeCartItemAPI, action.payload);
        yield put(removeCartItemSuccess(action.payload));
    } catch (err) {
        yield put(removeCartItemFailure(err.response?.data?.error || err.message));
    }
}

// DELETE /api/cart - 전체비우기
export const clearCartAPI = () => api.delete(CART_API_BASE);
export function* clearCart() {
    try {
        yield call(clearCartAPI);
        yield put(clearCartSuccess());
    } catch (err) {
        yield put(clearCartFailure(err.response?.data?.error || err.message));
    }
}

function* watchFetchCart() {       yield takeLatest(fetchCartRequest.type,       fetchCart); }
function* watchAddToCart() {       yield takeLatest(addToCartRequest.type,       addToCart); }
function* watchUpdateCartItem() {  yield takeLatest(updateCartItemRequest.type,  updateCartItem); }
function* watchRemoveCartItem() {  yield takeLatest(removeCartItemRequest.type,  removeCartItem); }
function* watchClearCart() {       yield takeLatest(clearCartRequest.type,       clearCart); }

export default function* cartSaga() {
    yield all([
        call(watchFetchCart),
        call(watchAddToCart),
        call(watchUpdateCartItem),
        call(watchRemoveCartItem),
        call(watchClearCart),
    ]);
}
