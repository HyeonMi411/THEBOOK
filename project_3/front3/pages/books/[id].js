// pages/books/[id].js
import React, { useEffect } from "react";
import { useRouter } from "next/router";
import { useSelector, useDispatch } from "react-redux";
import { Card, Spin, Image, Tag, Descriptions } from "antd";
import { fetchBookDetailRequest, resetBookState } from "../../reducers/bookReducer";

export default function BookDetailPage() {
  const router = useRouter();
  const { id } = router.query;
  const dispatch = useDispatch();

  const { currentBook, loading, error } = useSelector((state) => state.book);

  useEffect(() => {
    if (id) {
      dispatch(fetchBookDetailRequest(id));
    }
    return () => { dispatch(resetBookState()); };
  }, [id, dispatch]);

  if (loading || !currentBook) return <Spin />;
  if (error) return <p style={{ color: "red" }}>{error}</p>;

  return (
    <Card style={{ maxWidth: 800, margin: "0 auto" }}>
      <div style={{ display: "flex", gap: 24, flexWrap: "wrap" }}>
        {currentBook.bookCover && (
          <Image
            src={`http://localhost:8080/${currentBook.bookCover}`}
            alt={currentBook.title}
            style={{ width: 220, objectFit: "cover", borderRadius: 8 }}
          />
        )}
        <div style={{ flex: 1, minWidth: 240 }}>
          <h2>{currentBook.title}</h2>
          <p>{currentBook.author} · {currentBook.publisher}</p>
          <Tag color="blue">{currentBook.category}</Tag>
          {currentBook.ranking && <Tag color="volcano">{currentBook.ranking}</Tag>}

          <Descriptions column={1} style={{ marginTop: 16 }}>
            <Descriptions.Item label="출간일">{currentBook.publishDate}</Descriptions.Item>
            <Descriptions.Item label="페이지">{currentBook.pages}</Descriptions.Item>
            <Descriptions.Item label="가격">
              {currentBook.price != null ? `${currentBook.price.toLocaleString()}원` : "-"}
            </Descriptions.Item>
            <Descriptions.Item label="평점">{currentBook.rating ?? "-"}</Descriptions.Item>
            <Descriptions.Item label="등록자">{currentBook.userNickname}</Descriptions.Item>
          </Descriptions>
        </div>
      </div>

      <h4 style={{ marginTop: 24 }}>도서설명</h4>
      <p style={{ whiteSpace: "pre-wrap" }}>{currentBook.description}</p>
    </Card>
  );
}
