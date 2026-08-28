// pages/oauth2/signup.js
// ★신규 소셜회원 전용 "가입확인(추가정보 입력)" 화면입니다.
// OAuth2SuccessHandler 는 처음 보는(email+provider 조합이 DB에 없는) 사용자를
// 곧바로 회원가입시키지 않고, 이 화면(?signupToken=...)으로 먼저 보냅니다.
// 여기서 닉네임을 확인/수정하고 "가입완료"를 눌러야 실제로 DB에 저장되고 로그인됩니다.
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

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!nickname.trim()) {
      setError("닉네임을 입력해주세요.");
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

          {error && <p style={{ color: "red", marginTop: 8 }}>{error}</p>}

          <button type="submit" className="btn btn-primary-bs" disabled={submitting} style={{ marginTop: 16 }}>
            {submitting ? "가입 처리중..." : "가입완료"}
          </button>
        </form>
      </div>
    </div>
  );
}
