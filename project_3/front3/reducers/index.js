// reducers/index.js
import {combineReducers}  from "@reduxjs/toolkit";
import authReducer from './authReducer';
import postReducer from './postReducer';
import bookReducer from './bookReducer';       // 도서(BOOK)
import noticeReducer from './noticeReducer';   // 공지사항(SBOARD2)
import cartReducer from './cartReducer';       // 장바구니
import orderReducer from './orderReducer';     // 주문/결제

const rootReducer = combineReducers({
    auth: authReducer ,   // state.auth
    post: postReducer ,   // state.post
    book: bookReducer ,   // state.book     
    notice: noticeReducer , // state.notice 
    cart: cartReducer ,   // state.cart     
    order: orderReducer , // state.order    
});

export default rootReducer;