// components/BookCoverImage.js
// 표지/썸네일 이미지가 없거나(src가 비어있음) 또는 URL은 있지만 실제 로드에 실패하는 경우
// (국립중앙도서관/카카오 썸네일은 종종 깨진 링크가 있습니다) 모두 기본 아이콘으로 자동 대체합니다.
import React, { useState, useEffect } from 'react';

export default function BookCoverImage({ src, alt, height = '100%', iconSize = 40, style }) {
  const [failed, setFailed] = useState(false);

  // src 가 바뀌면(다른 도서를 보게 되면) 에러상태 초기화
  useEffect(() => { setFailed(false); }, [src]);

  const showFallback = !src || failed;

  if (showFallback) {
    return (
      <div
        className="book-cover-fallback"
        style={{
          width: '100%', height, display: 'flex', alignItems: 'center', justifyContent: 'center',
          background: '#eef1f5', color: '#bbb', fontSize: iconSize, ...style,
        }}
      >
        📕
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt || '표지 이미지'}
      onError={() => setFailed(true)}
      style={{ width: '100%', height, objectFit: 'cover', ...style }}
    />
  );
}
