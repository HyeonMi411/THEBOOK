// pages/books/new.js  ( ★도서등록은 관리자(ROLE_ADMIN)만 가능 )
import React, { useState, useEffect } from "react";
import { Card, Form, Input, Button, message, Upload, InputNumber, DatePicker, Result } from "antd";
import { useSelector, useDispatch } from "react-redux";
import { useRouter } from "next/router";
import { createBookRequest, resetBookState } from "../../reducers/bookReducer";
import { UpOutlined } from "@ant-design/icons";

export default function NewBookPage() {
  const router = useRouter();
  const dispatch = useDispatch();

  const { loading, error, success } = useSelector((state) => state.book);
  const { user } = useSelector((state) => state.auth);

  const [coverFile, setCoverFile] = useState(null);

  const isAdmin = user?.role === "ROLE_ADMIN";

  const onFinish = (values) => {
    const dto = {
      title: values.title,
      author: values.author,
      publisher: values.publisher,
      publishDate: values.publishDate ? values.publishDate.format("YYYY-MM-DD") : undefined,
      category: values.category,
      ranking: values.ranking,
      pages: values.pages,
      price: values.price,
      description: values.description,
    };
    dispatch(createBookRequest({ dto, cover: coverFile }));
  };

  useEffect(() => {
    if (success) {
      message.success("도서가 성공적으로 등록되었습니다.");
      setCoverFile(null);
      dispatch(resetBookState());
      router.push("/books");
    }
    return () => {
      if (success) { dispatch(resetBookState()); }
    };
  }, [success, router, dispatch]);

  // ★비로그인/일반회원은 접근 불가 (실제 저장은 백엔드 @PreAuthorize 가 최종적으로 막아주지만,
  //   화면단에서도 미리 안내해서 불필요한 요청을 막습니다.)
  if (!user) {
    return <Result status="403" title="로그인이 필요합니다." subTitle="도서등록은 관리자만 가능합니다." />;
  }
  if (!isAdmin) {
    return <Result status="403" title="접근 권한이 없습니다." subTitle="도서등록은 관리자(ROLE_ADMIN)만 가능합니다." />;
  }

  return (
    <Card title="도서 등록" style={{ maxWidth: 600, margin: "0 auto" }}>
      <Form onFinish={onFinish} layout="vertical">
        <Form.Item label="도서명" name="title" rules={[{ required: true, message: '도서명을 입력하세요.' }]}>
          <Input placeholder="도서명" />
        </Form.Item>
        <Form.Item label="저자" name="author" rules={[{ required: true, message: '저자를 입력하세요.' }]}>
          <Input placeholder="저자" />
        </Form.Item>
        <Form.Item label="출판사" name="publisher" rules={[{ required: true, message: '출판사를 입력하세요.' }]}>
          <Input placeholder="출판사" />
        </Form.Item>
        <Form.Item label="출간일" name="publishDate" rules={[{ required: true, message: '출간일을 선택하세요.' }]}>
          <DatePicker style={{ width: "100%" }} />
        </Form.Item>
        <Form.Item label="카테고리" name="category" rules={[{ required: true, message: '카테고리를 입력하세요.' }]}>
          <Input placeholder="예: IT, 소설, 인문 ..." />
        </Form.Item>
        <Form.Item label="랭킹(선택)" name="ranking">
          <Input placeholder="예: TOP1" />
        </Form.Item>
        <Form.Item label="페이지수" name="pages">
          <InputNumber style={{ width: "100%" }} min={0} />
        </Form.Item>
        <Form.Item label="가격" name="price">
          <InputNumber style={{ width: "100%" }} min={0} />
        </Form.Item>
        <Form.Item label="도서설명" name="description">
          <Input.TextArea rows={4} placeholder="도서 상세설명을 입력하세요." />
        </Form.Item>

        {/* 표지이미지 업로드 */}
        <Form.Item label="표지이미지">
          <Upload
            multiple={false}
            beforeUpload={() => false}
            maxCount={1}
            listType="picture-card"
            onChange={({ fileList }) => setCoverFile(fileList[0]?.originFileObj || null)}
          >
            <Button icon={<UpOutlined />}>표지이미지 선택</Button>
          </Upload>
        </Form.Item>

        <Button type="primary" htmlType="submit" loading={loading}>
          도서 등록
        </Button>
        {error && <p style={{ color: "red" }}>{error}</p>}
      </Form>
    </Card>
  );
}
