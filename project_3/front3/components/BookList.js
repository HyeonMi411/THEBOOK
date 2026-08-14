// components/BookList.js
import React from 'react';
import { Card, Button, Popconfirm, Image, Tag, Row, Col } from 'antd';
import Link from 'next/link';

export default function BookList({ books = [], isAdmin, handleEdit, handleDelete }) {
  return (
    <div>
      {/* 도서목록 */}
      <h3> 도서 : {books?.length || 0} </h3>

      <Row gutter={[16, 16]}>
        {books?.map((book) => (
          <Col xs={24} sm={12} md={8} key={book.id}>
            <Card
              style={{ height: "100%" }}
              // ★관리자(isAdmin)일 때만 수정/삭제 버튼 노출 (도서등록은 관리자전용)
              actions={
                isAdmin
                  ? [
                      <Button type="link" onClick={() => handleEdit(book)}>수정</Button>,
                      <Popconfirm
                        title="정말 삭제하시겠습니까?"
                        onConfirm={() => handleDelete(book.id)}
                        okText="예"
                        cancelText="아니오"
                      >
                        <Button type="link">삭제</Button>
                      </Popconfirm>,
                    ]
                  : undefined
              }
            >
              {/* 도서 표지이미지 */}
              {book?.bookCover && (
                <div style={{ textAlign: "center", marginBottom: 12 }}>
                  <Image
                    src={`http://localhost:8080/${book.bookCover}`}
                    alt={book.title}
                    style={{ maxWidth: "100%", height: "220px", objectFit: "cover", borderRadius: "8px" }}
                  />
                </div>
              )}

              <Link href={`/books/${book.id}`}>
                <a style={{ fontSize: 17, fontWeight: "bold" }}>{book.title}</a>
              </Link>
              <p style={{ margin: "4px 0", color: "#555" }}>
                {book.author} · {book.publisher}
              </p>

              <div style={{ marginBottom: 8 }}>
                <Tag color="blue">{book.category}</Tag>
                {book.ranking && <Tag color="volcano">{book.ranking}</Tag>}
              </div>

              {book.price != null && (
                <p style={{ fontWeight: "bold" }}>{book.price.toLocaleString()}원</p>
              )}
              {book.rating != null && <p>⭐ {book.rating}</p>}

              <p style={{ color: "#999", fontSize: 12 }}>등록자 : {book.userNickname}</p>
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  );
}
