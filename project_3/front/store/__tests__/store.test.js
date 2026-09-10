// store/__tests__/store.test.js
import { makeStore } from '../configureStore'; // 스토어 설정 파일 경로에 맞게 수정해주세요
import { fetchBooksRequest } from '../../reducers/bookReducer';

describe('Redux Store and Saga Middleware', () => {
    it('should create store successfully with saga middleware', () => {
        const store = makeStore();

        // 1. 초기 상태(initialState) 확인
        const state = store.getState();
        expect(state).toHaveProperty('auth');
        expect(state).toHaveProperty('book');

        // 2. 사가 태스크(sagaTask)가 정상적으로 등록되었는지 확인
        expect(store.sagaTask).toBeDefined();

        // 3. 액션 디스패치 테스트 (리듀서가 정상 동작하는지 확인)
        store.dispatch(fetchBooksRequest());
        const updatedState = store.getState();
        expect(updatedState.book.loading).toBe(true);
    });
});
// npm test