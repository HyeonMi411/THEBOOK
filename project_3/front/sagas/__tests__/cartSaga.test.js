// sagas/__tests__/cartSaga.test.js
// call - 동기 - 제너레이터함수 function* 일시중단 후 결과물 받기
// put  - redux 액션처리
import { call, put } from 'redux-saga/effects';
import {
    fetchCartRequest, fetchCartSuccess, fetchCartFailure,
    addToCartRequest, addToCartSuccess, addToCartFailure,
    updateCartItemRequest, updateCartItemSuccess, updateCartItemFailure,
    removeCartItemRequest, removeCartItemSuccess, removeCartItemFailure,
    clearCartRequest, clearCartSuccess, clearCartFailure,
} from '../../reducers/cartReducer';
import { fetchCart, addToCart, updateCartItem, removeCartItem, clearCart } from '../cartSaga';

// authSaga.test.js 와 동일한 이유로 jest.mock('axios') 를 쓰지 않음.
// generator.next() 로 saga 를 한단계씩 직접 실행시키면서 CALL 이펙트에 가짜 응답을
// 수동으로 넣어주는 방식이라, 실제 axios 인스턴스가 네트워크를 타지 않음.

// 더미SQL 데이터(스프링부트 완전정복 등)와 겹치지 않도록, 테스트 전용 도서명을 사용.
const bookA = { id: 201, bookId: 111, bookTitle: '카트사가테스트도서A', bookCover: null, price: 15000, quantity: 2, subtotal: 30000, stockQuantity: 5 };
const bookB = { id: 202, bookId: 112, bookTitle: '카트사가테스트도서B', bookCover: null, price: 9000, quantity: 1, subtotal: 9000, stockQuantity: 10 };

describe('cart saga', () => {
    afterEach(() => { jest.clearAllMocks(); });

    // -- 장바구니 조회 --
    it('fetchCart success', () => {
        const action = fetchCartRequest();
        const generator = fetchCart(action);

        // 1. 1단계 API 호출 (call)
        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        // 2. api 성공했다라는 가정하에 결과 값을 전달
        const mockResponse = { data: { items: [bookA, bookB], totalAmount: 39000 } };
        const putStep = generator.next(mockResponse).value;

        // 3. 2단계 성공액션 디스패치
        expect(putStep).toEqual(put(fetchCartSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true); // 제너레이터 완전종료 done
    });

    it('fetchCart failure - 네트워크 오류 등으로 실패하면 fetchCartFailure 가 디스패치되는지', () => {
        const action = fetchCartRequest();
        const generator = fetchCart(action);

        generator.next(); // CALL 단계 진행

        const mockError = { response: { data: { error: '장바구니 조회 실패' } } };
        const putStep = generator.throw(mockError).value; // catch 블록으로 진입시킴

        expect(putStep).toEqual(put(fetchCartFailure('장바구니 조회 실패')));
        expect(generator.next().done).toBe(true);
    });

    // -- 담기 --
    it('addToCart success', () => {
        const payload = { bookId: 111, quantity: 2 };
        const action = addToCartRequest(payload);
        const generator = addToCart(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = { data: { items: [bookA], totalAmount: 30000 } };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(addToCartSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('addToCart failure - 재고부족이면 addToCartFailure 가 디스패치되는지', () => {
        const payload = { bookId: 111, quantity: 100 }; // 재고보다 많이 담기 시도
        const action = addToCartRequest(payload);
        const generator = addToCart(action);

        generator.next();

        const mockError = { response: { data: { error: '[카트사가테스트도서A] 재고가 부족합니다. (현재 재고 : 5권)' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(addToCartFailure('[카트사가테스트도서A] 재고가 부족합니다. (현재 재고 : 5권)')));
        expect(generator.next().done).toBe(true);
    });

    // -- 수량수정 --
    it('updateCartItem success', () => {
        const payload = { itemId: 201, quantity: 4 };
        const action = updateCartItemRequest(payload);
        const generator = updateCartItem(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const updatedA = { ...bookA, quantity: 4, subtotal: 60000 };
        const mockResponse = { data: { items: [updatedA, bookB], totalAmount: 69000 } };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(updateCartItemSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('updateCartItem failure', () => {
        const payload = { itemId: 201, quantity: 999 };
        const action = updateCartItemRequest(payload);
        const generator = updateCartItem(action);

        generator.next();

        const mockError = { response: { data: { error: '재고가 부족합니다.' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(updateCartItemFailure('재고가 부족합니다.')));
        expect(generator.next().done).toBe(true);
    });

    // -- 항목삭제 -- ( API 응답값 자체는 안 쓰고 action.payload(itemId)를 그대로 success에 담아 dispatch )
    it('removeCartItem success', () => {
        const itemId = 201;
        const action = removeCartItemRequest(itemId);
        const generator = removeCartItem(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        // DELETE 응답은 보통 body 가 없으므로 undefined 를 넘겨도 무방
        const putStep = generator.next(undefined).value;

        expect(putStep).toEqual(put(removeCartItemSuccess(itemId)));
        expect(generator.next().done).toBe(true);
    });

    it('removeCartItem failure - 본인 항목이 아니면 거부되는지', () => {
        const itemId = 999;
        const action = removeCartItemRequest(itemId);
        const generator = removeCartItem(action);

        generator.next();

        const mockError = { response: { data: { error: '본인의 장바구니 항목만 삭제할 수 있습니다.' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(removeCartItemFailure('본인의 장바구니 항목만 삭제할 수 있습니다.')));
        expect(generator.next().done).toBe(true);
    });

    // -- 전체비우기 --
    it('clearCart success', () => {
        const action = clearCartRequest();
        const generator = clearCart(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const putStep = generator.next().value;

        expect(putStep).toEqual(put(clearCartSuccess()));
        expect(generator.next().done).toBe(true);
    });

    it('clearCart failure', () => {
        const action = clearCartRequest();
        const generator = clearCart(action);

        generator.next();

        const mockError = { response: { data: { error: '장바구니 비우기 실패' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(clearCartFailure('장바구니 비우기 실패')));
        expect(generator.next().done).toBe(true);
    });
});

// npm test cartSaga.test.js
