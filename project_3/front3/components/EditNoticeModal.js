// components/EditNoticeModal.js  (SBOARD2 - 공지사항)
import { Modal, Form, Input, Button, Upload } from 'antd';

export default function EditNoticeModal({
  visible, onCancel, editNotice, onSubmit, uploadFile, setUploadFile
}) {
  return (
    <Modal title="공지사항 수정" open={visible} onCancel={onCancel} footer={null} destroyOnClose>
      <Form
        initialValues={{
          btitle: editNotice?.btitle,
          bcontent: editNotice?.bcontent,
        }}
        onFinish={onSubmit}
        layout="vertical"
      >
        <Form.Item name="btitle" label="제목" rules={[{ required: true, message: '제목을 입력하세요.' }]}>
          <Input />
        </Form.Item>
        <Form.Item name="bcontent" label="내용" rules={[{ required: true, message: '내용을 입력하세요.' }]}>
          <Input.TextArea rows={6} />
        </Form.Item>

        {/* 첨부파일 (새 파일 업로드시에만 교체, 안하면 기존 첨부파일 유지) */}
        <Form.Item label="첨부파일 (변경시에만 선택)">
          <Upload
            multiple={false}
            beforeUpload={() => false}
            maxCount={1}
            onChange={({ fileList }) => setUploadFile(fileList[0]?.originFileObj || null)}
          >
            <Button>파일 선택</Button>
          </Upload>
        </Form.Item>

        <Button type="primary" htmlType="submit">
          수정완료
        </Button>
      </Form>
    </Modal>
  );
}
