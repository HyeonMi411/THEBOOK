// __tests__/orderReducer.test.js
import orderReducer, {
    resetOrderState, resetPaymentState,
    createOrderRequest, createOrderSuccess, createOrderFailure,
    fetchMyOrdersRequest, fetchMyOrdersSuccess, fetchMyOrdersFailure,
    fetchOrderDetailRequest, fetchOrderDetailSuccess, fetchOrderDetailFailure,
    deleteOrderRequest, deleteOrderSuccess, deleteOrderFailure,
    paymentReadyRequest, paymentReadySuccess, paymentReadyFailure,
    paymentApproveRequest, paymentApproveSuccess, paymentApproveFailure,
    paymentCancelRequest, paymentCancelSuccess, paymentCancelFailure,
    paymentFailRequest, paymentFailSuccess, paymentFailFailure,
} from '../orderReducer';

describe('order slice reducer', () => {
    const initialState = {
        orders: [],
        currentOrder: null,
        currentPage: 1,
        totalPages: 1,
        totalElements: 0,
        pageSize: 12,
        loading: false,
        error: null,
        paymentLoading: false,
        paymentError: null,
        redirectUrl: null,
    };

    // 더미SQL 데이터(스프링부트 완전정복 등)와 겹치지 않도록, 테스트 전용 도서명을 사용합니다.
    const orderPayload = {
        id: 501,
        totalAmount: 45000,
        orderStatus: 'PENDING',
        tid: null,
        items: [
            { id: 901, bookId: 201, bookTitle: '주문테스트도서A', bookCover: null, price: 15000, quantity: 3 },
        ],
        createdAt: '2026-01-01T00:00:00',
        approvedAt: null,
    };

    it('resetOrderState', () => {
        const prev = { ...initialState, loading: true, error: '에러' };
        const state = orderReducer(prev, resetOrderState());
        // 1. resetOrderState() 실행 - 인자없음
        // 2. 리듀서 툴킷 - { type: resetOrderState, payload: undefined } 객체만들기
        // 3. 리듀서의 resetOrderState: (state, action) => {} 액션받아서처리 - loading/error 만 초기화
        expect(state.loading).toBe(false);
        expect(state.error).toBeNull();
    });

    it('resetPaymentState', () => {
        const prev = { ...initialState, paymentLoading: true, paymentError: '결제에러', redirectUrl: 'https://pg.kakao.com/x' };
        const state = orderReducer(prev, resetPaymentState());
        expect(state.paymentLoading).toBe(false);
        expect(state.paymentError).toBeNull();
        expect(state.redirectUrl).toBeNull();
    });

    //////////////////////////////////////////// 주문 생성
    it('createOrderRequest', () => {
        const state = orderReducer(initialState, createOrderRequest());
        expect(state.loading).toBe(true);
        expect(state.error).toBeNull();
    });

    it('createOrderSuccess - 생성된 주문이 currentOrder 에 저장되는지', () => {
        const state = orderReducer(initialState, createOrderSuccess(orderPayload));
        expect(state.loading).toBe(false);
        expect(state.currentOrder).toEqual(orderPayload);
        expect(state.currentOrder.orderStatus).toBe('PENDING'); // 생성 직후엔 PENDING 이어야 함
    });

    it('createOrderFailure - 재고부족/잘못된요청 등 실패 메시지 저장', () => {
        const state = orderReducer(initialState, createOrderFailure('재고가 부족합니다.'));
        expect(state.loading).toBe(false);
        expect(state.error).toBe('재고가 부족합니다.');
    });

    //////////////////////////////////////////// 내 주문내역 조회 (페이징)
    it('fetchMyOrdersRequest', () => {
        const state = orderReducer(initialState, fetchMyOrdersRequest());
        expect(state.loading).toBe(true);
    });

    it('fetchMyOrdersSuccess - 페이징 필드가 전부 정확히 반영되는지', () => {
        const pageResponse = {
            content: [orderPayload],
            currentPage: 2,
            pageSize: 12,
            totalElements: 13,
            totalPages: 2,
        };
        const state = orderReducer(initialState, fetchMyOrdersSuccess(pageResponse));
        expect(state.loading).toBe(false);
        expect(state.orders).toEqual([orderPayload]);
        expect(state.currentPage).toBe(2);
        expect(state.pageSize).toBe(12);
        expect(state.totalElements).toBe(13);
        expect(state.totalPages).toBe(2);
    });

    it('fetchMyOrdersFailure', () => {
        const state = orderReducer(initialState, fetchMyOrdersFailure('주문내역 조회 실패'));
        expect(state.error).toBe('주문내역 조회 실패');
    });

    //////////////////////////////////////////// 주문 상세 조회
    it('fetchOrderDetailRequest', () => {
        const state = orderReducer(initialState, fetchOrderDetailRequest());
        expect(state.loading).toBe(true);
    });

    it('fetchOrderDetailSuccess', () => {
        const state = orderReducer(initialState, fetchOrderDetailSuccess(orderPayload));
        expect(state.loading).toBe(false);
        expect(state.currentOrder).toEqual(orderPayload);
    });

    it('fetchOrderDetailFailure - 본인 주문이 아니면 거부 메시지 저장', () => {
        const state = orderReducer(initialState, fetchOrderDetailFailure('본인의 주문만 조회할 수 있습니다.'));
        expect(state.error).toBe('본인의 주문만 조회할 수 있습니다.');
    });

    //////////////////////////////////////////// ★주문 삭제 (결제전(PENDING) 주문만 가능)
    it('deleteOrderRequest', () => {
        const state = orderReducer(initialState, deleteOrderRequest());
        expect(state.loading).toBe(true);
        expect(state.error).toBeNull();
    });

    it('deleteOrderSuccess - 삭제한 주문만 목록에서 정확히 빠지는지', () => {
        const otherOrder = { ...orderPayload, id: 502 };
        const prev = { ...initialState, orders: [orderPayload, otherOrder] };
        const state = orderReducer(prev, deleteOrderSuccess(orderPayload.id)); // id=501 삭제
        expect(state.loading).toBe(false);
        expect(state.orders).toHaveLength(1);
        expect(state.orders[0].id).toBe(502); // otherOrder 만 남아야 함
    });

    it('deleteOrderFailure - 결제완료 주문 삭제 시도시 거부 메시지 저장', () => {
        const state = orderReducer(initialState, deleteOrderFailure('결제가 진행된 주문은 삭제할 수 없습니다. (현재 상태 : PAID)'));
        expect(state.loading).toBe(false);
        expect(state.error).toBe('결제가 진행된 주문은 삭제할 수 없습니다. (현재 상태 : PAID)');
    });

    //////////////////////////////////////////// ★카카오페이 결제준비
    it('paymentReadyRequest - redirectUrl 이 초기화되는지', () => {
        const prev = { ...initialState, redirectUrl: '이전URL' };
        const state = orderReducer(prev, paymentReadyRequest());
        expect(state.paymentLoading).toBe(true);
        expect(state.paymentError).toBeNull();
        expect(state.redirectUrl).toBeNull();
    });

    it('paymentReadySuccess - 카카오페이 결제창 URL 이 저장되는지', () => {
        const payload = { orderId: 501, tid: 'T_test_123', redirectUrl: 'https://mockup-pg.kakao.com/pc/501' };
        const state = orderReducer(initialState, paymentReadySuccess(payload));
        expect(state.paymentLoading).toBe(false);
        expect(state.redirectUrl).toBe('https://mockup-pg.kakao.com/pc/501');
    });

    it('paymentReadyFailure', () => {
        const state = orderReducer(initialState, paymentReadyFailure('이미 처리된 주문입니다.'));
        expect(state.paymentLoading).toBe(false);
        expect(state.paymentError).toBe('이미 처리된 주문입니다.');
    });

    //////////////////////////////////////////// ★카카오페이 결제승인 ( ★실제로 상태가 PAID로 바뀌는지 꼼꼼히 검증 )
    it('paymentApproveRequest', () => {
        const state = orderReducer(initialState, paymentApproveRequest());
        expect(state.paymentLoading).toBe(true);
        expect(state.paymentError).toBeNull();
    });

    it('paymentApproveSuccess - 승인된 주문(PAID)이 currentOrder 에 정확히 반영되는지', () => {
        const paidOrder = { ...orderPayload, orderStatus: 'PAID', tid: 'T_test_123', approvedAt: '2026-01-01T00:05:00' };
        const state = orderReducer(initialState, paymentApproveSuccess(paidOrder));
        expect(state.paymentLoading).toBe(false);
        expect(state.currentOrder.orderStatus).toBe('PAID');
        expect(state.currentOrder.approvedAt).not.toBeNull();
    });

    it('paymentApproveFailure', () => {
        const state = orderReducer(initialState, paymentApproveFailure('재고가 부족합니다.'));
        expect(state.paymentError).toBe('재고가 부족합니다.');
    });

    //////////////////////////////////////////// ★카카오페이 결제취소
    it('paymentCancelRequest', () => {
        const state = orderReducer(initialState, paymentCancelRequest());
        expect(state.paymentLoading).toBe(true);
    });

    it('paymentCancelSuccess - currentOrder 가 있으면 상태를 CANCELLED 로 바꾸는지', () => {
        const prev = { ...initialState, currentOrder: { ...orderPayload } };
        const state = orderReducer(prev, paymentCancelSuccess());
        expect(state.paymentLoading).toBe(false);
        expect(state.currentOrder.orderStatus).toBe('CANCELLED');
    });

    it('paymentCancelSuccess - currentOrder 가 없어도(null) 에러 없이 안전하게 처리되는지', () => {
        const state = orderReducer(initialState, paymentCancelSuccess()); // currentOrder: null 인 상태
        expect(state.paymentLoading).toBe(false);
        expect(state.currentOrder).toBeNull(); // 에러 없이 그대로 null 유지
    });

    it('paymentCancelFailure', () => {
        const state = orderReducer(initialState, paymentCancelFailure('본인의 주문만 처리할 수 있습니다.'));
        expect(state.paymentError).toBe('본인의 주문만 처리할 수 있습니다.');
    });

    //////////////////////////////////////////// ★카카오페이 결제실패
    it('paymentFailRequest', () => {
        const state = orderReducer(initialState, paymentFailRequest());
        expect(state.paymentLoading).toBe(true);
    });

    it('paymentFailSuccess - currentOrder 가 있으면 상태를 FAILED 로 바꾸는지', () => {
        const prev = { ...initialState, currentOrder: { ...orderPayload } };
        const state = orderReducer(prev, paymentFailSuccess());
        expect(state.paymentLoading).toBe(false);
        expect(state.currentOrder.orderStatus).toBe('FAILED');
    });

    it('paymentFailSuccess - currentOrder 가 없어도(null) 에러 없이 안전하게 처리되는지', () => {
        const state = orderReducer(initialState, paymentFailSuccess());
        expect(state.currentOrder).toBeNull();
    });

    it('paymentFailFailure', () => {
        const state = orderReducer(initialState, paymentFailFailure('처리 중 오류가 발생했습니다.'));
        expect(state.paymentError).toBe('처리 중 오류가 발생했습니다.');
    });
});
// npm test orderReducer
