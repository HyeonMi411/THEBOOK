// sagas/index.js
import { all, fork }  from 'redux-saga/effects';

import  authSaga  from  './authSaga';
import  postSaga  from  './postSaga';
import  bookSaga  from  './bookSaga';     // ★도서(BOOK)
import  noticeSaga  from  './noticeSaga'; // ★공지사항(SBOARD2)
//import  commentSaga  from  './commentSaga';

export default  function  *rootSaga(){
    yield all([
        fork( authSaga ) ,
        fork( postSaga ) ,
        fork( bookSaga ) ,     // ★
        fork( noticeSaga ) ,   // ★
       // fork( commentSaga ) ,
    ]);
}
// fork : 기다리지 않음 (다른일할수 있게 양보)   - 동시에 실행
// call : 기다림 (어떠한일이 끝날때까지 기다리기) - 결과물 필수적 