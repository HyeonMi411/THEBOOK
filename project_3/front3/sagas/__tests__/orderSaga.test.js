// sagas/__tests__/orderSaga.test.js
// call - 동기 - 제너레이터함수 function* 일시중단 후 결과물 받기
// put  - redux 액션처리
import { call, put } from 'redux-saga/effects';
import {
    createOrderRequest, createOrderSuccess, createOrderFailure,
    fetchMyOrdersRequest, fetchMyOrdersSuccess, fetchMyOrdersFailure,
    fetchOrderDetailRequest, fetchOrderDetailSuccess, fetchOrderDetailFailure,
    paymentReadyRequest, paymentReadySuccess, paymentReadyFailure,
    paymentApproveRequest, paymentApproveSuccess, paymentApproveFailure,
    paymentCancelRequest, paymentCancelSuccess, paymentCancelFailure,
    paymentFailRequest, paymentFailSuccess, paymentFailFailure,
} from '../../reducers/orderReducer';
import {
    createOrder, fetchMyOrders, fetchOrderDetail,
    paymentReady, paymentApprove, paymentCancel, paymentFail,
} from '../orderSaga';

// ★ authSaga.test.js 와 동일한 이유로 jest.mock('axios') 를 쓰지 않습니다.
//   generator.next() 로 saga 를 한단계씩 직접 실행시키면서 CALL 이펙트에 가짜 응답을
//   수동으로 넣어주는 방식이라, 실제 axios 인스턴스가 네트워크를 타지 않습니다.

// 더미SQL 데이터(스프링부트 완전정복 등)와 겹치지 않도록, 테스트 전용 도서명을 사용합니다.
const orderItem = { id: 901, bookId: 301, bookTitle: '오더사가테스트도서A', bookCover: null, price: 15000, quantity: 3 };
const pendingOrder = {
    id: 601, totalAmount: 45000, orderStatus: 'PENDING', tid: null,
    items: [orderItem], createdAt: '2026-01-01T00:00:00', approvedAt: null,
};

describe('order saga', () => {
    afterEach(() => { jest.clearAllMocks(); });

    // -- 주문 생성 --
    it('createOrder success - 바로구매', () => {
        const payload = { bookId: 301, quantity: 3 };
        const action = createOrderRequest(payload);
        const generator = createOrder(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = { data: pendingOrder };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(createOrderSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('createOrder failure - 재고부족이면 createOrderFailure 가 디스패치되는지', () => {
        const payload = { bookId: 301, quantity: 999 };
        const action = createOrderRequest(payload);
        const generator = createOrder(action);

        generator.next();

        const mockError = { response: { data: { error: '[오더사가테스트도서A] 재고가 부족합니다. (현재 재고 : 5권)' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(createOrderFailure('[오더사가테스트도서A] 재고가 부족합니다. (현재 재고 : 5권)')));
        expect(generator.next().done).toBe(true);
    });

    // -- 내 주문내역 조회 (페이징) --
    it('fetchMyOrders success', () => {
        const action = fetchMyOrdersRequest({ page: 1, size: 12 });
        const generator = fetchMyOrders(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = {
            data: { content: [pendingOrder], currentPage: 1, pageSize: 12, totalElements: 1, totalPages: 1 },
        };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(fetchMyOrdersSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('fetchMyOrders failure', () => {
        const action = fetchMyOrdersRequest({ page: 1, size: 12 });
        const generator = fetchMyOrders(action);

        generator.next();

        const mockError = { response: { data: { error: '주문내역 조회 실패' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(fetchMyOrdersFailure('주문내역 조회 실패')));
        expect(generator.next().done).toBe(true);
    });

    // -- 주문 상세 조회 --
    it('fetchOrderDetail success', () => {
        const action = fetchOrderDetailRequest(601);
        const generator = fetchOrderDetail(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = { data: pendingOrder };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(fetchOrderDetailSuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('fetchOrderDetail failure - 본인 주문이 아니면 거부되는지', () => {
        const action = fetchOrderDetailRequest(999);
        const generator = fetchOrderDetail(action);

        generator.next();

        const mockError = { response: { data: { error: '본인의 주문만 조회할 수 있습니다.' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(fetchOrderDetailFailure('본인의 주문만 조회할 수 있습니다.')));
        expect(generator.next().done).toBe(true);
    });

    // -- ★카카오페이 결제준비 --
    it('paymentReady success - tid/redirectUrl 을 받아오는지', () => {
        const action = paymentReadyRequest(601);
        const generator = paymentReady(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const mockResponse = {
            data: { orderId: 601, tid: 'T_test_saga_123', redirectUrl: 'https://mockup-pg.kakao.com/pc/601' },
        };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(paymentReadySuccess(mockResponse.data)));
        expect(generator.next().done).toBe(true);
    });

    it('paymentReady failure - 이미 처리된 주문이면 거부되는지', () => {
        const action = paymentReadyRequest(601);
        const generator = paymentReady(action);

        generator.next();

        const mockError = { response: { data: { error: '이미 처리된 주문입니다.' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(paymentReadyFailure('이미 처리된 주문입니다.')));
        expect(generator.next().done).toBe(true);
    });

    // -- ★카카오페이 결제승인 ( ★재고차감 결과가 반영된 주문상태(PAID)가 정확히 dispatch 되는지 검증 ) --
    it('paymentApprove success - 결제승인 후 주문상태가 PAID 로 반영된 응답이 정확히 dispatch 되는지', () => {
        const payload = { orderId: 601, pgToken: 'pg_token_test_saga' };
        const action = paymentApproveRequest(payload);
        const generator = paymentApprove(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        // ★백엔드에서 결제승인 시 재고가 실제로 차감되고, 그 결과 주문상태가 PAID 로
        //   바뀐 응답을 돌려줍니다. 이 응답이 saga 를 통해 정확히 그대로 dispatch 되는지 확인합니다.
        const paidOrder = { ...pendingOrder, orderStatus: 'PAID', tid: 'T_test_saga_123', approvedAt: '2026-01-01T00:05:00' };
        const mockResponse = { data: paidOrder };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(paymentApproveSuccess(mockResponse.data)));
        // ★toEqual() 이 payload 전체(=paidOrder, orderStatus:'PAID' 포함)를 이미 검증하지만,
        //   "재고차감 결과가 PAID 로 정확히 반영됐는지"를 명시적으로 한 번 더 확인합니다.
        expect(mockResponse.data.orderStatus).toBe('PAID');
        expect(generator.next().done).toBe(true);
    });

    it('paymentApprove failure - 승인 시점에 재고가 바닥났으면 거부되는지', () => {
        const payload = { orderId: 601, pgToken: 'pg_token_test_saga' };
        const action = paymentApproveRequest(payload);
        const generator = paymentApprove(action);

        generator.next();

        const mockError = { response: { data: { error: '[오더사가테스트도서A] 재고가 부족합니다.' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(paymentApproveFailure('[오더사가테스트도서A] 재고가 부족합니다.')));
        expect(generator.next().done).toBe(true);
    });

    // -- ★카카오페이 결제취소 -- ( ★API 응답값 자체는 안 쓰고 성공 여부만 dispatch )
    it('paymentCancel success', () => {
        const action = paymentCancelRequest(601);
        const generator = paymentCancel(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const putStep = generator.next().value;

        expect(putStep).toEqual(put(paymentCancelSuccess()));
        expect(generator.next().done).toBe(true);
    });

    it('paymentCancel failure', () => {
        const action = paymentCancelRequest(601);
        const generator = paymentCancel(action);

        generator.next();

        const mockError = { response: { data: { error: '본인의 주문만 처리할 수 있습니다.' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(paymentCancelFailure('본인의 주문만 처리할 수 있습니다.')));
        expect(generator.next().done).toBe(true);
    });

    // -- ★카카오페이 결제실패 --
    it('paymentFail success', () => {
        const action = paymentFailRequest(601);
        const generator = paymentFail(action);

        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        const putStep = generator.next().value;

        expect(putStep).toEqual(put(paymentFailSuccess()));
        expect(generator.next().done).toBe(true);
    });

    it('paymentFail failure', () => {
        const action = paymentFailRequest(601);
        const generator = paymentFail(action);

        generator.next();

        const mockError = { response: { data: { error: '처리 중 오류가 발생했습니다.' } } };
        const putStep = generator.throw(mockError).value;

        expect(putStep).toEqual(put(paymentFailFailure('처리 중 오류가 발생했습니다.')));
        expect(generator.next().done).toBe(true);
    });
});

// npm test orderSaga.test.js
