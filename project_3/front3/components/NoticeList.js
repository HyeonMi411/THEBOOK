// components/NoticeList.js  (SBOARD2 - 공지사항)
import React from 'react';
import { List, Button, Popconfirm, Tag } from 'antd';
import Link from 'next/link';
import moment from 'moment';

export default function NoticeList({ notices = [], isAdmin, handleEdit, handleDelete }) {
  return (
    <div>
      {/* 공지사항 목록 */}
      <h3> 공지사항 : {notices?.length || 0} </h3>

      <List
        itemLayout="horizontal"
        dataSource={notices}
        renderItem={(notice) => (
          <List.Item
            // ★관리자(isAdmin)일 때만 수정/삭제 버튼 노출 (공지사항 글쓰기는 관리자전용)
            actions={
              isAdmin
                ? [
                    <Button type="link" onClick={() => handleEdit(notice)}>수정</Button>,
                    <Popconfirm
                      title="정말 삭제하시겠습니까?"
                      onConfirm={() => handleDelete(notice.id)}
                      okText="예"
                      cancelText="아니오"
                    >
                      <Button type="link">삭제</Button>
                    </Popconfirm>,
                  ]
                : []
            }
          >
            <List.Item.Meta
              title={
                <Link href={`/notices/${notice.id}`}>
                  <a>{notice.btitle}</a>
                </Link>
              }
              description={
                <span>
                  <Tag>조회수 {notice.bhit}</Tag>
                  <span style={{ marginLeft: 8, color: "#999" }}>
                    {notice.userNickname} ·{" "}
                    {notice.createdAt ? moment(notice.createdAt).format("YYYY-MM-DD HH:mm") : ""}
                  </span>
                </span>
              }
            />
          </List.Item>
        )}
      />
    </div>
  );
}
