// sagas/authSaga.js
import { all, call, put, takeLatest} from  'redux-saga/effects';
import  api  from  '../api/axios';   //###
import {signupRequest , signupSuccess , signupFailure,  resetUserState,
    loginRequest,loginSuccess,loginFailure,
    logoutRequest,logoutSuccess,logoutFailure,
    updateNicknameRequest, updateNicknameSuccess ,  updateNicknameFailure,
    updateProfileImageRequest , updateProfileImageSuccess , updateProfileImageFailure,
    loadUserRequest, loadUserSuccess , loadUserFailure,   //###
    refreshTokenSuccess
} from '../reducers/authReducer';
import Cookies from 'js-cookie';  //###

const USER_API_BASE = '/auth';  //###

// ---  회원가입  POST  /api/users ---
// POST : http://localhost:8080/auth/signup
export  const  signupApi = ( formData )=> api.post(  `${USER_API_BASE}/signup` , formData , {
    headers: { "Content-Type": "multipart/form-data"  },
} ); // /api/users

//■2.  signup(action) - action.payload 사용자가 입력한 값 (회원정보)
export  function*   signup(action){
    // action = { type: auth/signupRequest, payload: { email:'1@1' , password:'1'} }
    try{
        const result = yield  call( signupApi,  action.payload  );  //■3.  result.data
        yield  put(signupSuccess(result.data)); // 처리결과 put
    }catch(err){
        yield  put(signupFailure(err.response?.data?.message || err.message));
    }
}  

// ---  로그인       ---
// POST :     /auth/login
export  const  loginApi = ( payload )=> api.post( `${USER_API_BASE}/login`, payload); 
export function*  login( action ){
    // {email:'1@1', password:'1', provider:'local'}
    // action = {type:user/fetchUserRequest , payload:1}
    try{
        const result = yield call(loginApi , action.payload);  //■3) 
        // result = ResponseEntity<Map<String, Object>>   boot
        /* return ResponseEntity.ok(Map.of(  "accessToken", accessToken,  "user", user  )); */
       const accessToken = result.data?.accessToken;
       const user        = result.data?.user;

       if(  user  && accessToken ){
            if(  typeof  window != "undefined"){
                localStorage.setItem("accessToken" , accessToken);
                Cookies.set("accessToken", accessToken);
            }

            yield put(  loginSuccess( { user, accessToken} ) );
       } 
    }catch(err){
        yield put(  loginFailure( err.response?.data?.message || err.message ) );
    }
} 

// ---  토큰재발급   @PostMapping("/refresh")
export  const refreshApi = ()=>{   return   api.post(`${USER_API_BASE}/refresh`);   };
export  function * refresh(){
    try{
        const  result = yield call(refreshApi);
        const  newAccessToken =   result.data?.accessToken || null;
        // CSR 환경에서 localStorage와 쿠키에 저장
        if(  typeof  window != "undefined"  && newAccessToken){
            localStorage.setItem("accessToken" , newAccessToken);
            Cookies.set("accessToken" , newAccessToken);
        }
        yield  put(refreshTokenSuccess({ accessToken :  newAccessToken }));
    }catch(err){
        yield put(  refreshFailure( err.response?.data?.message || err.message ) );
        yield put(  logout());
    }
}

// ---  로그아웃  POST  :  /auth/logout  넘겨줄 데이터 x    ---
export  const  logoutApi = (  )=> api.post( `${USER_API_BASE}/logout`);

export function*  logout(){ 
    try{
        yield call(logoutApi);  

        if(  typeof  window != "undefined"){
            localStorage.removeItem("accessToken");
            Cookies.remove("accessToken");
        }

        yield put(  logoutSuccess() );
    }catch(err){
        yield put(  logoutFailure( err.response?.data?.message || err.message ) );
    }
}


// ---  업데이트 닉네임  PATCH :  /auth/{userId}/nickname  ,  params를 통해서 닉네임넘기기 ---
export const updateNicknameApi=({userId,nickname})=> api.patch( `${USER_API_BASE}/${userId}/nickname`, null ,{
    params:{nickname} ,
}); 
export function*  updateNickname( action ){ 
    try{
        const result = yield call(updateNicknameApi , action.payload);   
        yield put( updateNicknameSuccess( result.data ) );
    }catch(err){
        yield put(  updateNicknameFailure( err.response?.data?.message || err.message ) );
    }
}  
// ---  업데이트 프로필이미지  PATCH:  /auth/{userId}/profile-image  , formData  ---
export   function updateProfileImageApi({userId,file}){ 
    const formData = new FormData();
    formData.append("ufile" , file);
    return  api.patch( `${USER_API_BASE}/${userId}/profile-image`, formData ,{
          headers : {"Content-Type": "multipart/form-data" }
    });    
}
export function*  updateProfileImage( action ){ 
    try{
        const result = yield call(updateProfileImageApi , action.payload);   
        yield put( updateProfileImageSuccess( result.data ) );
    }catch(err){
        yield put(  updateProfileImageFailure( err.response?.data?.message || err.message ) );
    }
}   

// ---  유저 정보 로드  ---
// ★새로고침, 혹은 카카오페이 결제창(외부 도메인)에서 우리 사이트로 돌아오는 것처럼
//   브라우저가 완전히 새로 페이지를 로드하는 경우, Redux 스토어가 처음부터 다시
//   만들어져서 state.auth.user 가 null 로 초기화됩니다. localStorage 의 accessToken
//   은 그대로 살아있으므로, 이 API로 "그 토큰이 진짜 유효한 사용자의 것인지" 확인하고
//   user 정보를 다시 채워넣습니다.
export const loadUserApi = () => api.get(`${USER_API_BASE}/me`);
export function * loadUser(action){
    try {
        const result = yield call(loadUserApi);
        yield put(loadUserSuccess(result.data));
    } catch (err) {
        yield put(loadUserFailure(err.response?.data?.message || err.message));
    }
}

//■1) takeLatest : 여러번요청와도 1번만 
function* watchSignup(){            yield  takeLatest( signupRequest.type              , signup);       } 
function* watchLogin(){             yield  takeLatest( loginRequest.type               , login );       }
function* watchLogout(){            yield  takeLatest( logoutRequest.type              , logout );      }
function* watchUpdateNickname(){    yield  takeLatest( updateNicknameRequest.type      , updateNickname );    }
function* watchUpdateProfileImage(){yield  takeLatest( updateProfileImageRequest.type  , updateProfileImage );   }
function* watchLoadUser(){yield  takeLatest( loadUserRequest.type  , loadUser );   }

export default  function * authSaga(){
    yield all([
        call(watchSignup),
        call(watchLogin),
        call(watchLogout),
        call(watchUpdateNickname),
        call(watchUpdateProfileImage),
        call(watchLoadUser),
    ]);
}
 
