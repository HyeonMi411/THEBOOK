// sagas/__tests__/authSaga.test.js  
// call - 동기 - 제너레이터함수 function* 일시중단 후 결과물 받기 / fork (비동기)
// put  - redux 액션처리
import { call, put }  from 'redux-saga/effects';
import   {
    signupRequest , signupSuccess , signupFailure,  resetUserState,
    loginRequest,loginSuccess,loginFailure,
    logoutRequest,logoutSuccess,logoutFailure,
    updateNicknameRequest, updateNicknameSuccess ,  updateNicknameFailure,
    updateProfileImageRequest , updateProfileImageSuccess , updateProfileImageFailure
}from '../../reducers/authReducer';
import {  signup, login, logout, updateNickname, updateProfileImage, }  from  '../authSaga';

// ★ jest.mock('axios') 로 axios 모듈 전체를 자동목(auto-mock) 하면 axios.create() 가
//   undefined 를 반환하게 되어, api/axios.js 안의 axios.create(...).interceptors... 에서
//   "Cannot read properties of undefined (reading 'interceptors')" 로 즉시 크래시났었습니다.
//   아래 테스트들은 generator.next() 로 saga 를 직접 한단계씩 실행시키면서 CALL 이펙트에
//   가짜 응답을 수동으로 넣어주는 방식이라, 실제 axios 인스턴스가 네트워크를 타지 않습니다.
//   따라서 axios 를 모킹할 필요가 없어 jest.mock('axios') 를 제거했습니다.

describe('auth saga' , ()=>{
    afterEach(()=>{  jest.clearAllMocks()  });  //  afterEach  - 
    // --- 회원가입 ---
    it('signup success' , ()=>{  
        const userData = { email: '1@1' , password:'1' };  //##1
        const action   = signupRequest(userData);  //##2
        const generator= signup(action);

        //1. 1단계 API 호출 (call)
        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        //2. api 성공했다라는 가정하에 결과 값을 전달
        const mockResponse = { data:  { id:1, email: '1@1'} };  //##3
        const putStep = generator.next(  mockResponse  ).value;

        //3. 2단계 성공액션 디스패치
        expect(putStep).toEqual(  put(signupSuccess(mockResponse.data))   );  //##4
        expect(generator.next().done).toBe(true);  // 제너레이터 완전종료 done
    }); 
    // -- 로그인 --
    it('login' , ()=>{  
        const userData = { email: '1@1' , password:'1' };  //##1
        const action   = loginRequest(userData);  //##2
        const generator= login(action);

        //1. 1단계 API 호출 (call)
        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        //2. api 성공했다라는 가정하에 결과 값을 전달
        // ★백엔드는 ResponseEntity.ok(Map.of("accessToken", accessToken, "user", user)) 형태로
        //   응답하고, authSaga.js 의 login() 은 result.data.user / result.data.accessToken 을
        //   각각 꺼내서 loginSuccess({user, accessToken}) 로 dispatch 합니다.
        const user = { id:1, email: '1@1' , nickname:'first'};
        const accessToken = 'test-access-token';
        const mockResponse = { data: { accessToken, user } };  //##3
        const putStep = generator.next(  mockResponse  ).value;

        //3. 2단계 성공액션 디스패치
        expect(putStep).toEqual(  put(loginSuccess({ user, accessToken }))   );  //##4
        expect(generator.next().done).toBe(true);  // 제너레이터 완전종료 done
    }); 

    // -- 로그아웃 --
    it('logout' , ()=>{   
        const action   = logoutRequest( );   //##  view
        const generator= logout(action);

        //1. 1단계 API 호출 (call)
        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        //2. api 성공했다라는 가정하에 결과 값을 전달
        const putStep = generator.next().value;

        //3. 2단계 성공액션 디스패치
        expect(putStep).toEqual(  put(logoutSuccess())   );  //##4
        expect(generator.next().done).toBe(true);  // 제너레이터 완전종료 done
    }); 

    // -- 닉네임수정 --
    it('updateNickname' , ()=>{  
        const payload = { userId:1   ,  nickname: 'new'}; 
        const action   = updateNicknameRequest( payload );   //##  view
        const generator= updateNickname(action);

        //1. 1단계 API 호출 (call)
        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        //2. api 성공했다라는 가정하에 결과 값을 전달
        const mockResponse = { data:  { id:1,   nickname:'new'} }; 
        const putStep = generator.next( mockResponse  ).value;

        //3. 2단계 성공액션 디스패치
        expect(putStep).toEqual(  put(updateNicknameSuccess(  mockResponse.data ))   );  //##4
        expect(generator.next().done).toBe(true);  // 제너레이터 완전종료 done
    }); 

    // -- 프로필수정 --
    it('updateProfileImage' , ()=>{  
        const payload = { userId: 1, file: new Blob(['test']) };   // mypage.js
        const action   = updateProfileImageRequest( payload );   //##  view
        const generator= updateProfileImage(action);

        //1. 1단계 API 호출 (call)
        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        //2. api 성공했다라는 가정하에 결과 값을 전달
        const mockResponse = { data:  { id:1,   ufile:'profile.png'} }; 
        const putStep = generator.next( mockResponse  ).value;

        //3. 2단계 성공액션 디스패치
        expect(putStep).toEqual(  put(updateProfileImageSuccess(  mockResponse.data ))   );  //##4
        expect(generator.next().done).toBe(true);  // 제너레이터 완전종료 done
    }); 

});

// npm test  authSaga.test.js