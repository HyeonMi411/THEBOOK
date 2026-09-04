// pages/oauth2/signup.js
// 신규 소셜회원 전용 "가입확인(추가정보 입력)" 화면임.
// OAuth2SuccessHandler 는 처음 보는(email+provider 조합이 DB에 없는) 사용자를
// 곧바로 회원가입시키지 않고, 이 화면(?signupToken=...)으로 먼저 보냅니다.
// 여기서 닉네임을 확인/수정하고 "가입완료"를 눌러야 실제로 DB에 저장되고 로그인됨.
import { useEffect, useState } from "react";
import { useRouter } from "next/router";
import { useDispatch } from "react-redux";
import { loginSuccess } from "../../reducers/authReducer";
import axios from "axios";

export default function OAuth2SignupPage() {
  const router = useRouter();
  const dispatch = useDispatch();

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [preview, setPreview] = useState(null); // { email, provider, nicknameSuggestion, image }
  const [nickname, setNickname] = useState("");

  // 이메일 인증 상태 - 로컬 회원가입(signup.js)과 동일한 패턴
  const [emailCode, setEmailCode] = useState("");
  const [codeSent, setCodeSent] = useState(false);
  const [verified, setVerified] = useState(false);
  const [emailSending, setEmailSending] = useState(false);
  const [codeVerifying, setCodeVerifying] = useState(false);

  useEffect(() => {
    if (!router.isReady) return;
    const { signupToken } = router.query;
    if (!signupToken) {
      setError("가입확인 토큰이 없습니다. 로그인을 다시 시도해주세요.");
      setLoading(false);
      return;
    }
    fetchPreview(signupToken);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [router.isReady, router.query]);

  const fetchPreview = async (signupToken) => {
    try {
      const res = await axios.get("http://localhost:8080/auth/social/preview", {
        params: { signupToken },
      });
      setPreview(res.data);
      setNickname(res.data.nicknameSuggestion || "");
    } catch (err) {
      setError(
        err.response?.data?.error ||
          "가입확인 토큰이 만료되었거나 올바르지 않습니다. 로그인을 다시 시도해주세요."
      );
    } finally {
      setLoading(false);
    }
  };

  const handleSendEmailCode = async () => {
    if (!preview?.email) return;
    setEmailSending(true);
    setError(null);
    try {
      await axios.post("http://localhost:8080/auth/email/send-code", null, {
        params: { email: preview.email },
      });
      setCodeSent(true);
    } catch (err) {
      setError("인증번호 발송에 실패했습니다.");
    } finally {
      setEmailSending(false);
    }
  };

  const handleVerifyEmailCode = async () => {
    if (!emailCode) {
      setError("인증번호를 입력해주세요.");
      return;
    }
    setCodeVerifying(true);
    setError(null);
    try {
      await axios.post("http://localhost:8080/auth/email/verify-code", null, {
        params: { email: preview.email, code: emailCode },
      });
      setVerified(true);
    } catch (err) {
      setError("인증번호가 일치하지 않거나 만료되었습니다.");
    } finally {
      setCodeVerifying(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!nickname.trim()) {
      setError("닉네임을 입력해주세요.");
      return;
    }
    if (!verified) {
      setError("이메일 인증을 먼저 완료해주세요.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const { signupToken } = router.query;
      const res = await axios.post(
        "http://localhost:8080/auth/social/signup",
        { signupToken, nickname: nickname.trim() },
        { withCredentials: true } // 쿠키(refreshToken) 저장용
      );
      const { accessToken, user } = res.data;
      localStorage.setItem("accessToken", accessToken);
      dispatch(loginSuccess({ user, accessToken }));
      router.push("/mypage");
    } catch (err) {
      setError(err.response?.data?.error || "가입 처리 중 오류가 발생했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="signup-confirm-wrap">
        <p>확인 중입니다...</p>
      </div>
    );
  }

  if (error && !preview) {
    return (
      <div className="signup-confirm-wrap">
        <p style={{ color: "red" }}>{error}</p>
        <a href="/login">로그인 화면으로 돌아가기</a>
      </div>
    );
  }

  return (
    <div className="signup-confirm-wrap">
      <div className="signup-confirm-card">
        <h2>거의 다 왔어요 👋</h2>
        <p className="signup-confirm-desc">
          {preview?.provider} 계정({preview?.email})으로 처음 오셨네요.
          <br />
          닉네임을 확인하시고 가입을 완료해주세요.
        </p>

        <form onSubmit={handleSubmit}>
          <label htmlFor="nickname">닉네임</label>
          <input
            id="nickname"
            type="text"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            maxLength={20}
            disabled={submitting}
            autoFocus
          />

          {/* 이메일 인증 - 소셜 제공자가 이메일을 검증해줬더라도, 우리 서비스 자체적으로
              한 번 더 인증번호를 발송/확인해서 로컬 회원가입과 동일한 보안 수준을 유지 */}
          <div style={{ marginTop: 16 }}>
            <button
              type="button"
              className="btn"
              onClick={handleSendEmailCode}
              disabled={emailSending || verified}
            >
              {emailSending ? "발송중..." : "이메일 인증번호 받기"}
            </button>
            <input
              type="text"
              placeholder="인증번호 6자리"
              value={emailCode}
              onChange={(e) => setEmailCode(e.target.value)}
              disabled={!codeSent || verified}
              maxLength={6}
              style={{ marginLeft: 8, width: 140 }}
            />
            <button
              type="button"
              className="btn"
              onClick={handleVerifyEmailCode}
              disabled={!codeSent || verified || codeVerifying}
              style={{ marginLeft: 8 }}
            >
              {codeVerifying ? "확인중..." : "확인"}
            </button>
            {verified && (
              <p style={{ color: "green", marginTop: 4 }}>이메일 인증 완료</p>
            )}
          </div>

          {error && <p style={{ color: "red", marginTop: 8 }}>{error}</p>}

          <button type="submit" className="btn btn-primary-bs" disabled={submitting || !verified} style={{ marginTop: 16 }}>
            {submitting ? "가입 처리중..." : "가입완료"}
          </button>
        </form>
      </div>
    </div>
  );
}
