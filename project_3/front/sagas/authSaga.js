// sagas/authSaga.js
import { all, call, put, select, takeLatest} from  'redux-saga/effects';
import  api  from  '../api/axios';   
import {signupRequest , signupSuccess , signupFailure,  resetUserState,
    loginRequest,loginSuccess,loginFailure,
    logoutRequest,logoutSuccess,logoutFailure,
    withdrawRequest,withdrawSuccess,withdrawFailure,
    updateNicknameRequest, updateNicknameSuccess ,  updateNicknameFailure,
    updateProfileImageRequest , updateProfileImageSuccess , updateProfileImageFailure,
    loadUserRequest, loadUserSuccess , loadUserFailure,   
    refreshTokenSuccess
} from '../reducers/authReducer';
import Cookies from 'js-cookie';  

const USER_API_BASE = '/auth';  

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

// 소셜 로그아웃 URL 조회 - provider 별로 브라우저에 남은 소셜계정 세션까지 끊는 방법을 물어봅니다.
export const socialLogoutUrlApi = (provider) => api.get(`${USER_API_BASE}/social/logout-url`, { params: { provider } });

export function*  logout(){ 
    try{
        yield call(logoutApi);  

        if(  typeof  window != "undefined"){
            localStorage.removeItem("accessToken");
            Cookies.remove("accessToken");
        }

        // 우리 서비스 로그아웃은 끝났지만, 카카오/네이버 같은 소셜 계정 자체의
        // 브라우저 로그인 세션은 그대로 남아있을 수 있음. (그래서 로그아웃 직후
        // "카카오로 로그인"을 다시 누르면 비밀번호 확인 없이 바로 재로그인 되어버림)
        // provider 가 소셜(local 이 아님)이면, 그 계정 세션까지 끊는 방법을 서버에 물어봅니다.
        const provider = yield select((state) => state.auth.user?.provider);

        if (provider && provider !== 'local') {
            try {
                const result = yield call(socialLogoutUrlApi, provider);
                if (result.data.supported && result.data.logoutUrl) {
                    // 카카오 - 이 URL로 이동하면 브라우저의 카카오계정 세션 자체가 만료되고,
                    // 로그아웃 처리 후 다시 로그인 화면으로 돌아옵니다.
                    yield put(logoutSuccess());
                    window.location.href = result.data.logoutUrl;
                    return; // 페이지가 이동하므로 이후 로직은 실행할 필요 없음
                }
                // 네이버 등 자동 로그아웃 미지원 - 우리 서비스 로그아웃은 정상 완료됐으니
                // 그대로 진행하되, 사용자에게 안내만 해줍니다.
                if (!result.data.supported && typeof window !== 'undefined') {
                    // eslint-disable-next-line no-alert
                    alert(result.data.message);
                }
            } catch (e) {
                // 조회가 실패해도 우리 서비스 로그아웃 자체는 이미 완료됐으므로 조용히 넘어갑니다.
            }
        }

        yield put(  logoutSuccess() );

        // provider 가 local 이거나 소셜 자동로그아웃 미지원(구글)이면 여기까지 옵니다.
        // 카카오/네이버는 이미 위에서 그 사이트로 이동하며 return 됐으므로 여기 안 옵니다.
        if (typeof window !== "undefined") {
            window.location.href = "/login";
        }
    }catch(err){
        yield put(  logoutFailure( err.response?.data?.message || err.message ) );
    }
}

// ---  회원탈퇴  DELETE  :  /auth/me  넘겨줄 데이터 x  (AccessToken 으로 본인 식별)  ---
export  const  withdrawApi = (  )=> api.delete( `${USER_API_BASE}/me`);

export function*  withdraw(){
    try{
        yield call(withdrawApi);

        if(  typeof  window != "undefined"){
            localStorage.removeItem("accessToken");
            Cookies.remove("accessToken");
        }

        yield put(  withdrawSuccess() );

        if (typeof window !== "undefined") {
            window.location.href = "/login";
        }
    }catch(err){
        yield put(  withdrawFailure( err.response?.data?.message || err.message ) );
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
// 새로고침, 혹은 카카오페이 결제창(외부 도메인)에서 우리 사이트로 돌아오는 것처럼
// 브라우저가 완전히 새로 페이지를 로드하는 경우, Redux 스토어가 처음부터 다시
// 만들어져서 state.auth.user 가 null 로 초기화됨. localStorage 의 accessToken
// 은 그대로 살아있으므로, 이 API로 "그 토큰이 진짜 유효한 사용자의 것인지" 확인하고
// user 정보를 다시 채워넣음.
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
function* watchWithdraw(){          yield  takeLatest( withdrawRequest.type            , withdraw );    }
function* watchUpdateNickname(){    yield  takeLatest( updateNicknameRequest.type      , updateNickname );    }
function* watchUpdateProfileImage(){yield  takeLatest( updateProfileImageRequest.type  , updateProfileImage );   }
function* watchLoadUser(){yield  takeLatest( loadUserRequest.type  , loadUser );   }

export default  function * authSaga(){
    yield all([
        call(watchSignup),
        call(watchLogin),
        call(watchLogout),
        call(watchWithdraw),
        call(watchUpdateNickname),
        call(watchUpdateProfileImage),
        call(watchLoadUser),
    ]);
}
 
