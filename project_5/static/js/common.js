// 공통 인증/네비게이션 처리
function getAccessToken() { return localStorage.getItem("accessToken"); }
function setTokens(access, refresh) {
    localStorage.setItem("accessToken", access);
    localStorage.setItem("refreshToken", refresh);
}
function clearTokens() {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
}
function authHeaders() {
    const t = getAccessToken();
    return t ? { "Authorization": "Bearer " + t } : {};
}
async function apiFetch(path, options = {}) {
    options.headers = { ...(options.headers || {}), ...authHeaders() };
    const res = await fetch(API_BASE + path, options);
    return res;
}

document.addEventListener("DOMContentLoaded", () => {
    const loggedIn = !!getAccessToken();
    document.getElementById("nav-login").style.display = loggedIn ? "none" : "inline";
    document.getElementById("nav-signup").style.display = loggedIn ? "none" : "inline";
    document.getElementById("nav-logout").style.display = loggedIn ? "inline" : "none";
    document.getElementById("nav-logout").addEventListener("click", (e) => {
        e.preventDefault();
        clearTokens();
        window.location.href = "/";
    });
});
