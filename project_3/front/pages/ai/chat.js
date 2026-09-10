// pages/ai/chat.js - AI 문서 기반 질의응답(RAG) 챗봇
import React, { useState } from "react";
import { Card, Upload, Input, Button, message, Typography, Alert } from "antd";
import { UploadOutlined, SendOutlined } from "@ant-design/icons";
import { useSelector } from "react-redux";
import api from "../../api/axios";

const { TextArea } = Input;
const { Title, Paragraph } = Typography;

export default function AiChatPage() {
  const { user } = useSelector((state) => state.auth);

  const [fileList, setFileList] = useState([]);
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);
  const [chatLog, setChatLog] = useState([]); // [{question, answer}]

  const handleAsk = async () => {
    if (!question.trim()) {
      message.warning("질문을 입력하세요.");
      return;
    }
    setLoading(true);
    try {
      const formData = new FormData();
      if (fileList.length > 0) {
        formData.append("file", fileList[0]);
      }
      formData.append("question", question);

      const res = await api.post("/api/ai/rag/ask", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });

      setChatLog((prev) => [...prev, { question, answer: res.data.answer }]);
      setQuestion("");
    } catch (err) {
      message.error(
        err.response?.data?.answer || "답변을 가져오는 데 실패했습니다."
      );
    } finally {
      setLoading(false);
    }
  };

  if (!user) {
    return (
      <div style={{ maxWidth: 640, margin: "40px auto", padding: "0 16px" }}>
        <Alert
          type="warning"
          showIcon
          message="로그인이 필요합니다"
          description="AI 챗봇은 로그인한 회원만 사용할 수 있습니다."
        />
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 640, margin: "40px auto", padding: "0 16px" }}>
      <Title level={3}>AI 문서 챗봇</Title>
      <Paragraph type="secondary">
        PDF 파일을 첨부하고 질문하면, 그 문서 내용을 근거로 답변합니다. 파일을
        첨부하지 않으면 서버에 미리 등록된 기본 문서(있는 경우)를 사용합니다.
      </Paragraph>

      <Card style={{ marginBottom: 24 }}>
        <Upload
          beforeUpload={(file) => {
            setFileList([file]);
            return false; // 자동 업로드 방지 - 질문과 함께 한 번에 전송
          }}
          onRemove={() => setFileList([])}
          fileList={fileList}
          accept="application/pdf"
          maxCount={1}
        >
          <Button icon={<UploadOutlined />}>PDF 첨부 (선택)</Button>
        </Upload>

        <TextArea
          style={{ marginTop: 16 }}
          rows={3}
          placeholder="문서 내용에 대해 궁금한 점을 입력하세요."
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          onPressEnter={(e) => {
            if (!e.shiftKey) {
              e.preventDefault();
              handleAsk();
            }
          }}
        />

        <Button
          type="primary"
          icon={<SendOutlined />}
          style={{ marginTop: 12 }}
          loading={loading}
          onClick={handleAsk}
        >
          질문하기
        </Button>
      </Card>

      {chatLog.map((entry, idx) => (
        <Card key={idx} size="small" style={{ marginBottom: 12 }}>
          <Paragraph strong>Q. {entry.question}</Paragraph>
          <Paragraph style={{ whiteSpace: "pre-wrap" }}>
            A. {entry.answer}
          </Paragraph>
        </Card>
      ))}
    </div>
  );
}
