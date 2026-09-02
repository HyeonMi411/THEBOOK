// sagas/__tests__/authSaga.test.js  
// call - 동기 - 제너레이터함수 function* 일시중단 후 결과물 받기 / fork (비동기)
// put  - redux 액션처리
import { call, put, select }  from 'redux-saga/effects';
import   {
    signupRequest , signupSuccess , signupFailure,  resetUserState,
    loginRequest,loginSuccess,loginFailure,
    logoutRequest,logoutSuccess,logoutFailure,
    updateNicknameRequest, updateNicknameSuccess ,  updateNicknameFailure,
    updateProfileImageRequest , updateProfileImageSuccess , updateProfileImageFailure
}from '../../reducers/authReducer';
import {  signup, login, logout, updateNickname, updateProfileImage, }  from  '../authSaga';

// jest.mock('axios') 로 axios 모듈 전체를 자동목(auto-mock) 하면 axios.create() 가
// undefined 를 반환하게 되어, api/axios.js 안의 axios.create(...).interceptors... 에서
// "Cannot read properties of undefined (reading 'interceptors')" 로 즉시 크래시났었습니다.
// 아래 테스트들은 generator.next() 로 saga 를 직접 한단계씩 실행시키면서 CALL 이펙트에
// 가짜 응답을 수동으로 넣어주는 방식이라, 실제 axios 인스턴스가 네트워크를 타지 않습니다.
// 따라서 axios 를 모킹할 필요가 없어 jest.mock('axios') 를 제거했습니다.

describe('auth saga' , ()=>{
    afterEach(()=>{  jest.clearAllMocks()  });  // afterEach  - 
    // --- 회원가입 ---
    it('signup success' , ()=>{  
        const userData = { email: '1@1' , password:'1' };  1
        const action   = signupRequest(userData);
        const generator= signup(action);

        //1. 1단계 API 호출 (call)
        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        //2. api 성공했다라는 가정하에 결과 값을 전달
        const mockResponse = { data:  { id:1, email: '1@1'} };  3
        const putStep = generator.next(  mockResponse  ).value;

        //3. 2단계 성공액션 디스패치
        expect(putStep).toEqual(  put(signupSuccess(mockResponse.data))   );
        expect(generator.next().done).toBe(true);  // 제너레이터 완전종료 done
    }); 
    // -- 로그인 --
    it('login' , ()=>{  
        const userData = { email: '1@1' , password:'1' };  1
        const action   = loginRequest(userData);
        const generator= login(action);

        //1. 1단계 API 호출 (call)
        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        //2. api 성공했다라는 가정하에 결과 값을 전달
        // 백엔드는 ResponseEntity.ok(Map.of("accessToken", accessToken, "user", user)) 형태로
        // 응답하고, authSaga.js 의 login() 은 result.data.user / result.data.accessToken 을
        // 각각 꺼내서 loginSuccess({user, accessToken}) 로 dispatch 합니다.
        const user = { id:1, email: '1@1' , nickname:'first'};
        const accessToken = 'test-access-token';
        const mockResponse = { data: { accessToken, user } };  3
        const putStep = generator.next(  mockResponse  ).value;

        //3. 2단계 성공액션 디스패치
        expect(putStep).toEqual(  put(loginSuccess({ user, accessToken }))   );
        expect(generator.next().done).toBe(true);  // 제너레이터 완전종료 done
    }); 

    // -- 로그아웃 --
    it('logout - local(일반) 로그인 사용자는 소셜 로그아웃 URL 조회 없이 바로 완료되고 /login 으로 이동하는지', () => {
        const originalLocation = window.location;
        delete window.location;
        window.location = { href: '' }; // window.location.href 대입을 안전하게 가로채기 위한 모킹

        const action = logoutRequest();
        const generator = logout(action);

        // 1. 1단계 - 우리 서비스 로그아웃 API 호출 (call)
        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        // 2. api 성공 - localStorage/쿠키 정리 후 provider 를 select 로 조회
        const selectStep = generator.next().value;
        expect(selectStep.type).toBe('SELECT');

        // 3. provider 가 'local'(또는 없음) 이므로 소셜 로그아웃 URL 조회 없이 바로 완료
        const putStep = generator.next('local').value; // state.auth.user?.provider 결과로 'local' 을 전달
        expect(putStep).toEqual(put(logoutSuccess()));

        // 4. AppLayout 이 더 이상 이동을 책임지지 않으므로, saga 가 직접 /login 으로 이동시켜야 함
        expect(generator.next().done).toBe(true);
        expect(window.location.href).toBe('/login');

        window.location = originalLocation;
    });

    it('logout - 카카오 로그인 사용자는 카카오 로그아웃 URL 조회 후 이동하는지', () => {
        const originalLocation = window.location;
        delete window.location;
        window.location = { href: '' }; // window.location.href 대입을 안전하게 가로채기 위한 모킹

        const action = logoutRequest();
        const generator = logout(action);

        generator.next(); // CALL(logoutApi)
        const selectStep = generator.next().value; // SELECT(provider)
        expect(selectStep.type).toBe('SELECT');

        // provider = 'kakao' 로 응답
        const callSocialUrlStep = generator.next('kakao').value; // CALL(socialLogoutUrlApi)
        expect(callSocialUrlStep.type).toBe('CALL');

        const mockResponse = { data: { supported: true, logoutUrl: 'https://kauth.kakao.com/oauth/logout?client_id=test&logout_redirect_uri=http://localhost:3000/login' } };
        const putStep = generator.next(mockResponse).value;

        // 카카오 로그아웃 URL을 받으면 logoutSuccess 를 dispatch 하고 그 URL 로 이동해야 함
        expect(putStep).toEqual(put(logoutSuccess()));
        expect(generator.next().done).toBe(true);
        expect(window.location.href).toBe(mockResponse.data.logoutUrl);

        window.location = originalLocation; // 원상복구
    });

    it('logout - 네이버 로그인 사용자는 (비공식) 네이버 로그아웃 URL로 이동하는지', () => {
        const originalLocation = window.location;
        delete window.location;
        window.location = { href: '' };

        const action = logoutRequest();
        const generator = logout(action);

        generator.next(); // CALL(logoutApi)
        generator.next(); // SELECT(provider)

        const callSocialUrlStep = generator.next('naver').value; // CALL(socialLogoutUrlApi)
        expect(callSocialUrlStep.type).toBe('CALL');

        const mockResponse = { data: { supported: true, logoutUrl: 'https://nid.naver.com/nidlogin.logout?returl=http://localhost:3000/login' } };
        const putStep = generator.next(mockResponse).value;

        expect(putStep).toEqual(put(logoutSuccess()));
        expect(generator.next().done).toBe(true);
        expect(window.location.href).toBe(mockResponse.data.logoutUrl);

        window.location = originalLocation;
    });

    it('logout - 구글 로그인 사용자는 자동 로그아웃 미지원 안내가 뜨고 /login 으로 이동하는지', () => {
        const alertSpy = jest.spyOn(window, 'alert').mockImplementation(() => {});
        const originalLocation = window.location;
        delete window.location;
        window.location = { href: '' };

        const action = logoutRequest();
        const generator = logout(action);

        generator.next(); // CALL(logoutApi)
        generator.next(); // SELECT(provider)

        const callSocialUrlStep = generator.next('google').value; // CALL(socialLogoutUrlApi)
        expect(callSocialUrlStep.type).toBe('CALL');

        const mockResponse = { data: { supported: false, message: 'google 은(는) 소셜 계정 자체를 자동으로 로그아웃할 수 없습니다.' } };
        const putStep = generator.next(mockResponse).value;

        expect(alertSpy).toHaveBeenCalledWith(mockResponse.data.message);
        expect(putStep).toEqual(put(logoutSuccess()));

        // 소셜 로그아웃 리다이렉트를 지원 안 하는 경우도, saga 가 직접 /login 으로 이동시켜야 함
        expect(generator.next().done).toBe(true);
        expect(window.location.href).toBe('/login');

        alertSpy.mockRestore();
        window.location = originalLocation;
    });

    // -- 닉네임수정 --
    it('updateNickname' , ()=>{  
        const payload = { userId:1   ,  nickname: 'new'}; 
        const action   = updateNicknameRequest( payload );
        const generator= updateNickname(action);

        //1. 1단계 API 호출 (call)
        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        //2. api 성공했다라는 가정하에 결과 값을 전달
        const mockResponse = { data:  { id:1,   nickname:'new'} }; 
        const putStep = generator.next( mockResponse  ).value;

        //3. 2단계 성공액션 디스패치
        expect(putStep).toEqual(  put(updateNicknameSuccess(  mockResponse.data ))   );
        expect(generator.next().done).toBe(true);  // 제너레이터 완전종료 done
    }); 

    // -- 프로필수정 --
    it('updateProfileImage' , ()=>{  
        const payload = { userId: 1, file: new Blob(['test']) };   // mypage.js
        const action   = updateProfileImageRequest( payload );
        const generator= updateProfileImage(action);

        //1. 1단계 API 호출 (call)
        const callStep = generator.next().value;
        expect(callStep.type).toBe('CALL');

        //2. api 성공했다라는 가정하에 결과 값을 전달
        const mockResponse = { data:  { id:1,   ufile:'profile.png'} }; 
        const putStep = generator.next( mockResponse  ).value;

        //3. 2단계 성공액션 디스패치
        expect(putStep).toEqual(  put(updateProfileImageSuccess(  mockResponse.data ))   );
        expect(generator.next().done).toBe(true);  // 제너레이터 완전종료 done
    }); 

});

// npm test  authSaga.test.js