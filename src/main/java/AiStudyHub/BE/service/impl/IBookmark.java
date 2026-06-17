package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.BookmarkRequest;
import AiStudyHub.BE.dto.Response.BookmarkResponse;

import java.util.List;

public interface IBookmark {

    BookmarkResponse addBookmark(Long userId, BookmarkRequest request);

    boolean removeBookmark(Long userId, Long documentId);

    List<BookmarkResponse> getBookmarksByUser(Long userId);

    boolean isBookmarked(Long userId, Long documentId);
}
