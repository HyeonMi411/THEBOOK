// sagas/noticeSaga.js  (SBOARD2 - 공지사항)
import { all, call, put, takeLatest } from 'redux-saga/effects';
import api from '../api/axios';
import {
    fetchNoticesRequest, fetchNoticesSuccess, fetchNoticesFailure,
    fetchNoticeDetailRequest, fetchNoticeDetailSuccess, fetchNoticeDetailFailure,
    searchNoticesRequest, searchNoticesSuccess, searchNoticesFailure,
    createNoticeRequest, createNoticeSuccess, createNoticeFailure,
    updateNoticeRequest, updateNoticeSuccess, updateNoticeFailure,
    deleteNoticeRequest, deleteNoticeSuccess, deleteNoticeFailure,
} from '../reducers/noticeReducer';

const NOTICE_API_BASE = '/api/notices';

//   watchFetchNotices      -  GET   /api/notices   전체조회
//   watchFetchNotices      -  GET   /api/notices?page=1&size=12   전체(페이징) 조회
//   action.payload : { page, size } (모두 선택, 기본 page=1/size=12)
export const fetchNoticesAPI = (params = {}) =>
    api.get(NOTICE_API_BASE, { params: { page: params.page || 1, size: params.size || 12 } });
export function* fetchNotices(action) {
    try {
        const result = yield call(fetchNoticesAPI, action.payload);
        yield put(fetchNoticesSuccess(result.data));
    } catch (err) {
        yield put(fetchNoticesFailure(err.response?.data?.message || err.message));
    }
}

//   watchFetchNoticeDetail -  GET   /api/notices/{id}   단건조회 (서버에서 BHIT 조회수 +1 처리됨)
export const fetchNoticeDetailAPI = (id) => api.get(`${NOTICE_API_BASE}/${id}`);
export function* fetchNoticeDetail(action) {
    try {
        const result = yield call(fetchNoticeDetailAPI, action.payload);
        yield put(fetchNoticeDetailSuccess(result.data));
    } catch (err) {
        yield put(fetchNoticeDetailFailure(err.response?.data?.message || err.message));
    }
}

//   watchSearchNotices     -  GET   /api/notices/search?keyword=xxx   제목검색
export const searchNoticesAPI = (keyword) =>
    api.get(`${NOTICE_API_BASE}/search`, { params: { keyword } });
export function* searchNotices(action) {
    try {
        const result = yield call(searchNoticesAPI, action.payload);
        yield put(searchNoticesSuccess(result.data));
    } catch (err) {
        yield put(searchNoticesFailure(err.response?.data?.message || err.message));
    }
}

//   watchCreateNotice      -  POST  /api/notices   공지사항작성 ( ★관리자 전용 )
export function createNoticeAPI(payload) {
    const { dto, file } = payload;   // dto: btitle/bcontent , file: 첨부파일(File)
    const formData = new FormData();
    Object.entries(dto || {}).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== "") {
            formData.append(k, v);
        }
    });
    if (file) {
        formData.append("bfile", file);
    }
    return api.post(NOTICE_API_BASE, formData, {
        headers: { "Content-Type": "multipart/form-data" },
    });
}
export function* createNotice(action) {
    try {
        const result = yield call(createNoticeAPI, action.payload);
        yield put(createNoticeSuccess(result.data));
    } catch (err) {
        yield put(createNoticeFailure(err.response?.data?.message || err.message));
    }
}

//   watchUpdateNotice      -  PATCH /api/notices/{id}   공지사항수정 ( ★관리자 전용 )
export function updateNoticeAPI(payload) {
    const { noticeId, dto, file } = payload;
    const formData = new FormData();
    Object.entries(dto || {}).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== "") {
            formData.append(k, v);
        }
    });
    if (file) {
        formData.append("bfile", file);
    }
    return api.patch(`${NOTICE_API_BASE}/${noticeId}`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
    });
}
export function* updateNotice(action) {
    try {
        const result = yield call(updateNoticeAPI, action.payload);
        yield put(updateNoticeSuccess(result.data));
    } catch (err) {
        yield put(updateNoticeFailure(err.response?.data?.message || err.message));
    }
}

//   watchDeleteNotice      -  DELETE /api/notices/{id}   공지사항삭제 ( ★관리자 전용 )
export const deleteNoticeAPI = (id) => api.delete(`${NOTICE_API_BASE}/${id}`);
export function* deleteNotice(action) {
    try {
        yield call(deleteNoticeAPI, action.payload);
        yield put(deleteNoticeSuccess(action.payload));
    } catch (err) {
        yield put(deleteNoticeFailure(err.response?.data?.message || err.message));
    }
}

//  --- watch saga들 ---
function* watchFetchNotices() {      yield takeLatest(fetchNoticesRequest.type,      fetchNotices); }
function* watchFetchNoticeDetail() { yield takeLatest(fetchNoticeDetailRequest.type, fetchNoticeDetail); }
function* watchSearchNotices() {     yield takeLatest(searchNoticesRequest.type,     searchNotices); }
function* watchCreateNotice() {      yield takeLatest(createNoticeRequest.type,      createNotice); }
function* watchUpdateNotice() {      yield takeLatest(updateNoticeRequest.type,      updateNotice); }
function* watchDeleteNotice() {      yield takeLatest(deleteNoticeRequest.type,      deleteNotice); }

export default function* noticeSaga() {
    yield all([
        call(watchFetchNotices),
        call(watchFetchNoticeDetail),
        call(watchSearchNotices),
        call(watchCreateNotice),
        call(watchUpdateNotice),
        call(watchDeleteNotice),
    ]);
}
