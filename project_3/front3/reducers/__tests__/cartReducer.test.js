// __tests__/cartReducer.test.js
import cartReducer, {
    resetCartError,
    fetchCartRequest, fetchCartSuccess, fetchCartFailure,
    addToCartRequest, addToCartSuccess, addToCartFailure,
    updateCartItemRequest, updateCartItemSuccess, updateCartItemFailure,
    removeCartItemRequest, removeCartItemSuccess, removeCartItemFailure,
    clearCartRequest, clearCartSuccess, clearCartFailure,
    toggleSelectItem, selectAllItems, clearSelection,
} from '../cartReducer';

describe('cart slice reducer', () => {
    const initialState = {
        items: [],
        totalAmount: 0,
        loading: false,
        error: null,
    };

    // 더미SQL 데이터(스프링부트 완전정복 등)와 겹치지 않도록, 테스트 전용 도서명을 사용합니다.
    const bookA = { id: 101, bookTitle: '장바구니테스트도서A', bookCover: null, price: 15000, quantity: 2, subtotal: 30000, stockQuantity: 5 };
    const bookB = { id: 102, bookTitle: '장바구니테스트도서B', bookCover: null, price: 9000, quantity: 1, subtotal: 9000, stockQuantity: 10 };

    it('resetCartError', () => {
        const prev = { ...initialState, error: '이전 에러' };
        const state = cartReducer(prev, resetCartError());
        // 1. resetCartError() 실행 - 인자없음
        // 2. 리듀서 툴킷 - { type: resetCartError, payload: undefined } 객체만들기
        // 3. 리듀서의 resetCartError: (state, action) => {} 액션받아서처리 - error 만 초기화
        expect(state.error).toBeNull();
    });

    //////////////////////////////////////////// 장바구니 조회
    it('fetchCartRequest', () => {
        const state = cartReducer(initialState, fetchCartRequest());
        expect(state.loading).toBe(true);
        expect(state.error).toBeNull();
    });

    it('fetchCartSuccess', () => {
        const payload = { items: [bookA, bookB], totalAmount: 39000 };
        const state = cartReducer(initialState, fetchCartSuccess(payload));
        expect(state.loading).toBe(false);
        expect(state.items).toEqual([bookA, bookB]);
        expect(state.totalAmount).toBe(39000);
    });

    it('fetchCartSuccess - items/totalAmount 가 없는 응답이면 빈 배열/0 으로 안전하게 처리', () => {
        const state = cartReducer(initialState, fetchCartSuccess({}));
        expect(state.items).toEqual([]);
        expect(state.totalAmount).toBe(0);
    });

    it('fetchCartFailure', () => {
        const state = cartReducer(initialState, fetchCartFailure('장바구니 조회 실패'));
        expect(state.loading).toBe(false);
        expect(state.error).toBe('장바구니 조회 실패');
    });

    //////////////////////////////////////////// 담기
    it('addToCartRequest', () => {
        const state = cartReducer(initialState, addToCartRequest());
        expect(state.loading).toBe(true);
        expect(state.error).toBeNull();
    });

    it('addToCartSuccess', () => {
        const payload = { items: [bookA], totalAmount: 30000 };
        const state = cartReducer(initialState, addToCartSuccess(payload));
        expect(state.loading).toBe(false);
        expect(state.items).toEqual([bookA]);
        expect(state.totalAmount).toBe(30000);
    });

    it('addToCartFailure - 재고부족 등 실패 메시지 저장', () => {
        const state = cartReducer(initialState, addToCartFailure('[장바구니테스트도서A] 재고가 부족합니다.'));
        expect(state.loading).toBe(false);
        expect(state.error).toBe('[장바구니테스트도서A] 재고가 부족합니다.');
    });

    //////////////////////////////////////////// 수량수정
    it('updateCartItemRequest', () => {
        const state = cartReducer(initialState, updateCartItemRequest());
        expect(state.loading).toBe(true);
    });

    it('updateCartItemSuccess', () => {
        const updatedA = { ...bookA, quantity: 4, subtotal: 60000 };
        const payload = { items: [updatedA, bookB], totalAmount: 69000 };
        const state = cartReducer(initialState, updateCartItemSuccess(payload));
        expect(state.loading).toBe(false);
        expect(state.items[0].quantity).toBe(4);
        expect(state.totalAmount).toBe(69000);
    });

    it('updateCartItemFailure', () => {
        const state = cartReducer(initialState, updateCartItemFailure('재고가 부족합니다.'));
        expect(state.error).toBe('재고가 부족합니다.');
    });

    //////////////////////////////////////////// 항목삭제 ( ★총액 재계산 로직이 실제로 맞는지 꼼꼼히 검증 )
    it('removeCartItemRequest', () => {
        const state = cartReducer(initialState, removeCartItemRequest());
        expect(state.loading).toBe(true);
    });

    it('removeCartItemSuccess - 해당 항목만 제거되고 총액이 정확히 재계산되는지', () => {
        const prev = { ...initialState, items: [bookA, bookB], totalAmount: 39000 };
        const state = cartReducer(prev, removeCartItemSuccess(bookA.id)); // bookA(id:101) 삭제
        expect(state.loading).toBe(false);
        expect(state.items).toHaveLength(1);
        expect(state.items[0].id).toBe(bookB.id); // bookB 만 남아야 함
        expect(state.totalAmount).toBe(9000);     // 39000 - 30000 = 9000 로 재계산 확인
    });

    it('removeCartItemSuccess - 마지막 항목까지 지우면 총액이 0이 되는지', () => {
        const prev = { ...initialState, items: [bookB], totalAmount: 9000 };
        const state = cartReducer(prev, removeCartItemSuccess(bookB.id));
        expect(state.items).toHaveLength(0);
        expect(state.totalAmount).toBe(0);
    });

    it('removeCartItemFailure', () => {
        const state = cartReducer(initialState, removeCartItemFailure('본인의 장바구니 항목만 삭제할 수 있습니다.'));
        expect(state.error).toBe('본인의 장바구니 항목만 삭제할 수 있습니다.');
    });

    //////////////////////////////////////////// 전체비우기
    it('clearCartRequest', () => {
        const state = cartReducer(initialState, clearCartRequest());
        expect(state.loading).toBe(true);
    });

    it('clearCartSuccess - items 와 totalAmount 가 모두 초기화되는지', () => {
        const prev = { ...initialState, items: [bookA, bookB], totalAmount: 39000 };
        const state = cartReducer(prev, clearCartSuccess());
        expect(state.loading).toBe(false);
        expect(state.items).toEqual([]);
        expect(state.totalAmount).toBe(0);
    });

    it('clearCartFailure', () => {
        const state = cartReducer(initialState, clearCartFailure('장바구니 비우기 실패'));
        expect(state.error).toBe('장바구니 비우기 실패');
    });

    //////////////////////////////////////////// 선택(체크박스) - 프론트 전용 상태
    it('toggleSelectItem - 처음 선택하면 selectedIds 에 추가되는지', () => {
        const prev = { ...initialState, items: [bookA, bookB] };
        const state = cartReducer(prev, toggleSelectItem(bookA.id));
        expect(state.selectedIds).toEqual([bookA.id]);
    });

    it('toggleSelectItem - 이미 선택된 항목을 다시 누르면 선택해제(제거)되는지', () => {
        const prev = { ...initialState, items: [bookA, bookB], selectedIds: [bookA.id, bookB.id] };
        const state = cartReducer(prev, toggleSelectItem(bookA.id));
        expect(state.selectedIds).toEqual([bookB.id]); // bookA 만 빠져야 함
    });

    it('selectAllItems - 장바구니의 모든 항목 id 가 selectedIds 에 채워지는지', () => {
        const prev = { ...initialState, items: [bookA, bookB] };
        const state = cartReducer(prev, selectAllItems());
        expect(state.selectedIds).toEqual([bookA.id, bookB.id]);
    });

    it('clearSelection - selectedIds 가 빈 배열로 초기화되는지', () => {
        const prev = { ...initialState, items: [bookA, bookB], selectedIds: [bookA.id, bookB.id] };
        const state = cartReducer(prev, clearSelection());
        expect(state.selectedIds).toEqual([]);
    });
});
// npm test cartReducer
