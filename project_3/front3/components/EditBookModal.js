// components/EditBookModal.js
import { Modal, Form, Input, Button, Upload, InputNumber, DatePicker } from 'antd';
import { UpOutlined } from '@ant-design/icons';
import moment from 'moment';

export default function EditBookModal({
  visible, onCancel, editBook, onSubmit, uploadCover, setUploadCover
}) {
  return (
    <Modal title="도서 수정" open={visible} onCancel={onCancel} footer={null} destroyOnClose>
      <Form
        initialValues={{
          title: editBook?.title,
          author: editBook?.author,
          publisher: editBook?.publisher,
          publishDate: editBook?.publishDate ? moment(editBook.publishDate) : null,
          category: editBook?.category,
          ranking: editBook?.ranking,
          price: editBook?.price,
          pages: editBook?.pages,
          description: editBook?.description,
        }}
        onFinish={onSubmit}
        layout="vertical"
      >
        <Form.Item name="title" label="도서명" rules={[{ required: true, message: '도서명을 입력하세요.' }]}>
          <Input />
        </Form.Item>
        <Form.Item name="author" label="저자" rules={[{ required: true, message: '저자를 입력하세요.' }]}>
          <Input />
        </Form.Item>
        <Form.Item name="publisher" label="출판사" rules={[{ required: true, message: '출판사를 입력하세요.' }]}>
          <Input />
        </Form.Item>
        <Form.Item name="publishDate" label="출간일" rules={[{ required: true, message: '출간일을 선택하세요.' }]}>
          <DatePicker style={{ width: "100%" }} />
        </Form.Item>
        <Form.Item name="category" label="카테고리" rules={[{ required: true, message: '카테고리를 입력하세요.' }]}>
          <Input />
        </Form.Item>
        <Form.Item name="ranking" label="랭킹(선택)">
          <Input />
        </Form.Item>
        <Form.Item name="pages" label="페이지수">
          <InputNumber style={{ width: "100%" }} min={0} />
        </Form.Item>
        <Form.Item name="price" label="가격">
          <InputNumber style={{ width: "100%" }} min={0} />
        </Form.Item>
        <Form.Item name="description" label="도서설명">
          <Input.TextArea rows={4} />
        </Form.Item>

        {/* 표지이미지 (새 파일 업로드시에만 교체, 안하면 기존 이미지 유지) */}
        <Form.Item label="표지이미지 (변경시에만 선택)">
          <Upload
            multiple={false}
            beforeUpload={() => false}
            maxCount={1}
            onChange={({ fileList }) => setUploadCover(fileList[0]?.originFileObj || null)}
          >
            <Button icon={<UpOutlined />}>표지이미지 선택</Button>
          </Upload>
        </Form.Item>

        <Button type="primary" htmlType="submit">
          수정완료
        </Button>
      </Form>
    </Modal>
  );
}
