//1. import, require
import { useEffect, useRef } from "react";
import { useRouter } from "next/router";
import { useDispatch } from "react-redux";
import { loginSuccess } from "../../reducers/authReducer";
import axios from "axios";
// String targetUrl = redirectUrl + "?accessToken=" + access;   // 쿼리스트링
//2. 부품 + export
export default function OAuth2CallbackPage(){
    const router   = useRouter();     // 경로이동
    const dispatch = useDispatch();// 스토어알림

    // 소셜 provider별 사람이 읽기 좋은 이름 (에러 안내 문구용)
    const PROVIDER_LABEL = {
        local: "이메일/비밀번호",
        google: "구글",
        kakao: "카카오",
        naver: "네이버",
    };

    useEffect(()=>{
        if(! router.isReady) return;
        const {accessToken, error, existingProvider, email} = router.query;

        // 같은 이메일로 다른 방법(local 또는 다른 소셜 provider)으로 이미 가입된
        // 경우 - OAuth2SuccessHandler 가 신규가입 대신 이 에러로 리다이렉트시킴.
        // 계정이 여러 개로 쪼개지는 걸 막기 위한 회원인증(이메일 소유 확인) 단계.
        if (error === "email_already_exists") {
            const label = PROVIDER_LABEL[existingProvider] || existingProvider || "기존 방법";
            alert(`이미 가입된 이메일(${email || ""})입니다. ${label} 로그인으로 이용해주세요.`);
            router.push("/login");
            return;
        }

        // 탈퇴한 계정으로 소셜로그인을 시도한 경우
        if (error === "account_deleted") {
            alert("탈퇴한 계정입니다.");
            router.push("/login");
            return;
        }

        if(accessToken){
            try{
                localStorage.setItem("accessToken" , accessToken);      // 토큰 저장
                fetchUser(accessToken); // 사용자 정보를 요청
            }catch(err){
                console.error( "OAuth2 callback error:", err);
                router.push("/login");
            }
        }
    } , [ router.isReady , router.query ]);
    
    const fetchUser = async( accessToken)=>{
        try{
            const res = await axios.get("http://localhost:8080/auth/me", {
                headers: { Authorization: `Bearer ${accessToken}` },
                withCredentials: true,  //쿠키전송용
            });
            const user = res.data;
            dispatch(loginSuccess({ user, accessToken}));
            router.push("/mypage");
        }catch(err){
            console.error("User fetch error:", err);
            router.push("/login");            
        }

    };
    return (<p>소셜 로그인 처리 중입니다.</p>);
}

// useSelector  - 전역상태 / useDispatch  - 스토어알림
// useState     - 변수    / useEffect    - 이벤트변경감지
// useRouter    - 경로