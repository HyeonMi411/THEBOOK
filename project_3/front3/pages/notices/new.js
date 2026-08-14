// pages/notices/new.js  ( ★공지사항 글쓰기는 관리자(ROLE_ADMIN)만 가능 )
import React, { useState, useEffect } from "react";
import { Card, Form, Input, Button, message, Upload, Result } from "antd";
import { useSelector, useDispatch } from "react-redux";
import { useRouter } from "next/router";
import { createNoticeRequest, resetNoticeState } from "../../reducers/noticeReducer";

export default function NewNoticePage() {
  const router = useRouter();
  const dispatch = useDispatch();

  const { loading, error, success } = useSelector((state) => state.notice);
  const { user } = useSelector((state) => state.auth);

  const [file, setFile] = useState(null);

  const isAdmin = user?.role === "ROLE_ADMIN";

  const onFinish = (values) => {
    const dto = { btitle: values.btitle, bcontent: values.bcontent };
    dispatch(createNoticeRequest({ dto, file }));
  };

  useEffect(() => {
    if (success) {
      message.success("공지사항이 성공적으로 작성되었습니다.");
      setFile(null);
      dispatch(resetNoticeState());
      router.push("/notices");
    }
    return () => {
      if (success) { dispatch(resetNoticeState()); }
    };
  }, [success, router, dispatch]);

  // ★비로그인/일반회원은 접근 불가 (실제 저장은 백엔드 @PreAuthorize 가 최종적으로 막아줌)
  if (!user) {
    return <Result status="403" title="로그인이 필요합니다." subTitle="공지사항 작성은 관리자만 가능합니다." />;
  }
  if (!isAdmin) {
    return <Result status="403" title="접근 권한이 없습니다." subTitle="공지사항 작성은 관리자(ROLE_ADMIN)만 가능합니다." />;
  }

  return (
    <Card title="공지사항 작성" style={{ maxWidth: 600, margin: "0 auto" }}>
      <Form onFinish={onFinish} layout="vertical">
        <Form.Item label="제목" name="btitle" rules={[{ required: true, message: '제목을 입력하세요.' }]}>
          <Input placeholder="공지사항 제목" />
        </Form.Item>
        <Form.Item label="내용" name="bcontent" rules={[{ required: true, message: '내용을 입력하세요.' }]}>
          <Input.TextArea rows={6} placeholder="공지사항 내용을 입력하세요." />
        </Form.Item>

        {/* 첨부파일 */}
        <Form.Item label="첨부파일 (선택)">
          <Upload
            multiple={false}
            beforeUpload={() => false}
            maxCount={1}
            onChange={({ fileList }) => setFile(fileList[0]?.originFileObj || null)}
          >
            <Button>파일 선택</Button>
          </Upload>
        </Form.Item>

        <Button type="primary" htmlType="submit" loading={loading}>
          공지사항 작성
        </Button>
        {error && <p style={{ color: "red" }}>{error}</p>}
      </Form>
    </Card>
  );
}
