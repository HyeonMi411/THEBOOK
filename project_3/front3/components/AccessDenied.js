// components/AccessDenied.js
// 도서등록/공지작성 등 관리자 전용 화면에 비로그인/일반회원이 접근했을 때 보여주는 안내 화면
import React from 'react';
import { useRouter } from 'next/router';

export default function AccessDenied({ needLogin, message, backHref = '/' }) {
  const router = useRouter();

  return (
    <div className="access-denied-wrap">
      <div className="access-denied-card">
        <div className="access-denied-icon">{needLogin ? '🔒' : '⛔'}</div>
        <div className="access-denied-title">
          {needLogin ? '로그인이 필요합니다' : '접근 권한이 없습니다'}
        </div>
        <p className="access-denied-desc">
          {message || '이 기능은 관리자(ROLE_ADMIN)만 이용할 수 있습니다.'}
        </p>
        <div className="access-denied-actions">
          {needLogin && (
            <a
              className="btn btn-primary-bs"
              onClick={(e) => { e.preventDefault(); router.push('/login'); }}
              href="/login"
            >
              로그인 하러가기
            </a>
          )}
          <a
            className="btn btn-outline"
            onClick={(e) => { e.preventDefault(); router.push(backHref); }}
            href={backHref}
          >
            목록으로
          </a>
        </div>
      </div>
    </div>
  );
}
