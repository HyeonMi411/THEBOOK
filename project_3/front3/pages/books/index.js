// pages/books/index.js
import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from "react-redux";
import {
  fetchBooksRequest, updateBookRequest, deleteBookRequest, resetBookState
} from "../../reducers/bookReducer";
import { Spin, Input, message } from 'antd';
import BookList from '../../components/BookList';
import EditBookModal from '../../components/EditBookModal';

const { Search } = Input;

export default function BooksPage() {
  const dispatch = useDispatch();
  const { user } = useSelector((state) => state.auth);
  const { books, loading, error } = useSelector((state) => state.book);

  const isAdmin = user?.role === "ROLE_ADMIN"; // ★도서등록/수정/삭제는 관리자만

  const [isEditModalVisible, setIsEditModalVisible] = useState(false);
  const [uploadCover, setUploadCover] = useState(null);
  const [editBook, setEditBook] = useState(null);

  // 페이지가 처음뜰때 전체 도서조회
  useEffect(() => {
    dispatch(fetchBooksRequest());
  }, [dispatch]);

  const handleCategorySearch = (category) => {
    dispatch(fetchBooksRequest(category || undefined));
  };

  const handleEdit = (book) => {
    setEditBook(book);
    setIsEditModalVisible(true);
    setUploadCover(null);
  };

  const handleEditSubmit = (values) => {
    dispatch(updateBookRequest({
      bookId: editBook.id,
      dto: {
        title: values.title,
        author: values.author,
        publisher: values.publisher,
        publishDate: values.publishDate ? values.publishDate.format("YYYY-MM-DD") : undefined,
        category: values.category,
        ranking: values.ranking,
        pages: values.pages,
        price: values.price,
        description: values.description,
      },
      cover: uploadCover,
    }));
    setIsEditModalVisible(false);
    setEditBook(null);
    message.success("도서가 수정되었습니다.");
  };

  const handleDelete = (bookId) => {
    dispatch(deleteBookRequest(bookId));
    message.success("도서가 삭제되었습니다.");
  };

  useEffect(() => {
    return () => { dispatch(resetBookState()); };
  }, [dispatch]);

  return (
    <div>
      <Search
        placeholder="카테고리로 검색 (예: IT, 소설, 인문 ...)"
        allowClear
        onSearch={handleCategorySearch}
        style={{ maxWidth: 400, marginBottom: 20 }}
      />

      {loading && <Spin />}
      {error && <p style={{ color: "red" }}>{error}</p>}

      <BookList
        books={books}
        isAdmin={isAdmin}
        handleEdit={handleEdit}
        handleDelete={handleDelete}
      />

      <EditBookModal
        visible={isEditModalVisible}
        onCancel={() => setIsEditModalVisible(false)}
        editBook={editBook}
        onSubmit={handleEditSubmit}
        uploadCover={uploadCover}
        setUploadCover={setUploadCover}
      />
    </div>
  );
}
