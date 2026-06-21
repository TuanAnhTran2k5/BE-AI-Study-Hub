package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Request.BookmarkRequest;
import AiStudyHub.BE.dto.Response.BookmarkResponse;

import java.util.List;

import AiStudyHub.BE.dto.Response.DeleteResponse;

public interface IBookmark {

    BookmarkResponse addBookmark(Long userId, BookmarkRequest request);

    DeleteResponse removeBookmark(Long userId, Long documentId);

    List<BookmarkResponse> getBookmarksByUser(Long userId);

    boolean isBookmarked(Long userId, Long documentId);
}
