/**
 * next.config.js
 * ------------------------------------------------------------------
 *  redirects()
 *   루트(http://localhost:3000) 접속시 도서 목록(/books)으로 자동 이동시킵니다.
 *   permanent: false 로 해서 "임시 리다이렉트(307)"로 처리 - 나중에
 *   랜딩페이지가 따로 필요해지면 언제든 쉽게 되돌릴 수 있도록 하기 위함임.
 *   (permanent: true 로 하면 브라우저가 308로 강하게 캐싱해버려서, 나중에
 *   설정을 바꿔도 브라우저가 계속 예전 리다이렉트를 기억해버릴 수 있음)
 *
 *  eslint.ignoreDuringBuilds: true
 *   원본 프로젝트(boot1 기반)는 airbnb 스타일가이드(.eslintrc)를 쓰고 있는데,
 *   실제 코드 스타일은 그 규칙을 전부 지키도록 작성되어 있지 않음
 *   (따옴표 종류, 들여쓰기 칸수, 세미콜론 등 - 전부 "스타일" 문제일 뿐 실제
 *   동작에는 영향이 없음). 실제 "코드가 잘못됐는지"는 Next.js 의 컴파일
 *   (babel/webpack) 단계에서 이미 충분히 검증되므로, 스타일 규칙 위반 때문에
 *   프로덕션 빌드 자체가 실패하지 않도록 여기서 분리.
 * ------------------------------------------------------------------
 */
module.exports = {
  reactStrictMode: true,
  eslint: {
    ignoreDuringBuilds: true,
  },
  async redirects() {
    return [
      {
        source: '/',
        destination: '/books',
        permanent: false,
      },
    ];
  },
};
