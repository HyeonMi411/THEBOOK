// components/Footer.js  - boot1(the703) fragments/footer.html 디자인을 그대로 재현
import React from 'react';

export default function Footer() {
  const year = new Date().getFullYear();
  return (
    <footer className="bs-footer">
      <div className="bs-footer-bottom">
        &copy; {year} <strong>BookStore</strong>. All Rights Reserved.
      </div>
    </footer>
  );
}
