// sagas/bookSaga.js
import { all, call, put, takeLatest } from 'redux-saga/effects';
import api from '../api/axios';
import {
    fetchBooksRequest, fetchBooksSuccess, fetchBooksFailure,
    fetchBookDetailRequest, fetchBookDetailSuccess, fetchBookDetailFailure,
    searchBooksRequest, searchBooksSuccess, searchBooksFailure,
    createBookRequest, createBookSuccess, createBookFailure,
    updateBookRequest, updateBookSuccess, updateBookFailure,
    deleteBookRequest, deleteBookSuccess, deleteBookFailure,
    kakaoInsertRequest, kakaoInsertSuccess, kakaoInsertFailure,
} from '../reducers/bookReducer';

const BOOK_API_BASE = '/api/books';

//   watchFetchBooks       -  GET   /api/books?category=xxx   전체(카테고리별) 조회
//   action.payload : category 문자열 (없으면 전체조회)
//   watchFetchBooks       -  GET   /api/books?page=1&size=12&category=xxx   전체(페이징+카테고리) 조회
//   action.payload : { page, size, category } (모두 선택, 기본 page=1/size=12)
export const fetchBooksAPI = (params = {}) =>
    api.get(BOOK_API_BASE, { params: {
        page: params.page || 1,
        size: params.size || 12,
        ...(params.category ? { category: params.category } : {}),
    } });
export function* fetchBooks(action) {
    try {
        const result = yield call(fetchBooksAPI, action.payload);
        yield put(fetchBooksSuccess(result.data));
    } catch (err) {
        yield put(fetchBooksFailure(err.response?.data?.message || err.message));
    }
}

//   watchFetchBookDetail  -  GET   /api/books/{id}   단건조회
export const fetchBookDetailAPI = (id) => api.get(`${BOOK_API_BASE}/${id}`);
export function* fetchBookDetail(action) {
    try {
        const result = yield call(fetchBookDetailAPI, action.payload);
        yield put(fetchBookDetailSuccess(result.data));
    } catch (err) {
        yield put(fetchBookDetailFailure(err.response?.data?.message || err.message));
    }
}

//   watchSearchBooks      -  GET   /api/books/search?keyword=xxx  제목검색
export const searchBooksAPI = (keyword) =>
    api.get(`${BOOK_API_BASE}/search`, { params: { keyword } });
export function* searchBooks(action) {
    try {
        const result = yield call(searchBooksAPI, action.payload);
        yield put(searchBooksSuccess(result.data));
    } catch (err) {
        yield put(searchBooksFailure(err.response?.data?.message || err.message));
    }
}

//   watchCreateBook       -  POST  /api/books   도서등록 ( ★관리자 전용, JWT Authorization 헤더는
//                                                axios 인터셉터가 자동으로 붙여줌 )
export function createBookAPI(payload) {
    const { dto, cover } = payload;      // dto: title/author/publisher/publishDate/category/... , cover: 표지파일(File)
    const formData = new FormData();
    Object.entries(dto || {}).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== "") {
            formData.append(k, v);
        }
    });
    if (cover) {
        formData.append("cover", cover);
    }
    return api.post(BOOK_API_BASE, formData, {
        headers: { "Content-Type": "multipart/form-data" },
    });
}
export function* createBook(action) {
    try {
        const result = yield call(createBookAPI, action.payload);
        yield put(createBookSuccess(result.data));
    } catch (err) {
        yield put(createBookFailure(err.response?.data?.message || err.message));
    }
}

//   watchUpdateBook       -  PATCH /api/books/{id}   도서수정 ( ★관리자 전용 )
export function updateBookAPI(payload) {
    const { bookId, dto, cover } = payload;
    const formData = new FormData();
    Object.entries(dto || {}).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== "") {
            formData.append(k, v);
        }
    });
    if (cover) {
        formData.append("cover", cover);
    }
    return api.patch(`${BOOK_API_BASE}/${bookId}`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
    });
}
export function* updateBook(action) {
    try {
        const result = yield call(updateBookAPI, action.payload);
        yield put(updateBookSuccess(result.data));
    } catch (err) {
        yield put(updateBookFailure(err.response?.data?.message || err.message));
    }
}

//   watchDeleteBook       -  DELETE /api/books/{id}   도서삭제 ( ★관리자 전용 )
export const deleteBookAPI = (id) => api.delete(`${BOOK_API_BASE}/${id}`);
export function* deleteBook(action) {
    try {
        yield call(deleteBookAPI, action.payload);
        yield put(deleteBookSuccess(action.payload));
    } catch (err) {
        yield put(deleteBookFailure(err.response?.data?.message || err.message));
    }
}

//   watchKakaoInsert      -  POST /api/books/kakao-insert?search=xxx   카카오 도서검색 자동등록 ( ★관리자 전용 )
//   검색버튼을 누르면 카카오 API에서 도서를 가져와 자동으로 DB에 저장한 후, 응답으로 등록건수를 돌려줍니다.
export const kakaoInsertAPI = (search) =>
    api.post(`${BOOK_API_BASE}/kakao-insert`, null, { params: { search } });
export function* kakaoInsert(action) {
    try {
        const result = yield call(kakaoInsertAPI, action.payload);
        yield put(kakaoInsertSuccess(result.data)); // { search, insertedCount }
    } catch (err) {
        yield put(kakaoInsertFailure(err.response?.data?.message || err.message));
    }
}

//  --- watch saga들 ---
function* watchFetchBooks() {      yield takeLatest(fetchBooksRequest.type,      fetchBooks); }
function* watchFetchBookDetail() { yield takeLatest(fetchBookDetailRequest.type, fetchBookDetail); }
function* watchSearchBooks() {     yield takeLatest(searchBooksRequest.type,     searchBooks); }
function* watchCreateBook() {      yield takeLatest(createBookRequest.type,      createBook); }
function* watchUpdateBook() {      yield takeLatest(updateBookRequest.type,      updateBook); }
function* watchDeleteBook() {      yield takeLatest(deleteBookRequest.type,      deleteBook); }
function* watchKakaoInsert() {     yield takeLatest(kakaoInsertRequest.type,     kakaoInsert); }

export default function* bookSaga() {
    yield all([
        call(watchFetchBooks),
        call(watchFetchBookDetail),
        call(watchSearchBooks),
        call(watchCreateBook),
        call(watchUpdateBook),
        call(watchDeleteBook),
        call(watchKakaoInsert),
    ]);
}
