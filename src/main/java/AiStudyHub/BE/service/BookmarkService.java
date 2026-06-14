package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.dto.Request.BookmarkRequest;
import AiStudyHub.BE.dto.Response.BookmarkResponse;
import AiStudyHub.BE.entity.Bookmark;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.BookmarkMapper;
import AiStudyHub.BE.repository.BookmarkRepo;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.impl.IBookmark;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookmarkService implements IBookmark {

    BookmarkRepo bookmarkRepo;
    UserRepo userRepo;
    DocumentRepo documentRepo;
    BookmarkMapper bookmarkMapper;

    @Override
    @Transactional
    public BookmarkResponse addBookmark(Long userId, BookmarkRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        Document document = documentRepo.findById(request.getDocumentId())
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND));

        // Check if already bookmarked to prevent duplicate insertions
        if (bookmarkRepo.existsByUser_UserIdAndDocument_DocumentId(userId, request.getDocumentId())) {
            throw new GlobalException(400, "Document already bookmarked");
        }

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .document(document)
                .build();

        bookmark = bookmarkRepo.save(bookmark);

        // Update the document's bookmarkCount cache
        int currentCount = document.getBookmarkCount() != null ? document.getBookmarkCount() : 0;
        document.setBookmarkCount(currentCount + 1);
        documentRepo.save(document);

        return bookmarkMapper.toResponse(bookmark);
    }

    @Override
    @Transactional
    public void removeBookmark(Long userId, Long documentId) {
        if (!bookmarkRepo.existsByUser_UserIdAndDocument_DocumentId(userId, documentId)) {
            throw new GlobalException(ErrorCode.NOT_FOUND);
        }

        // Single DB DELETE query (transactional/modifying)
        bookmarkRepo.deleteByUser_UserIdAndDocument_DocumentId(userId, documentId);

        // Update the document's bookmarkCount cache
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND));
        
        int currentCount = document.getBookmarkCount() != null ? document.getBookmarkCount() : 0;
        if (currentCount > 0) {
            document.setBookmarkCount(currentCount - 1);
            documentRepo.save(document);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookmarkResponse> getBookmarksByUser(Long userId) {
        if (!userRepo.existsById(userId)) {
            throw new GlobalException(ErrorCode.USER_NOT_FOUND);
        }

        // JOIN FETCH avoids N+1 queries during response mapping
        List<Bookmark> bookmarks = bookmarkRepo.findByUser_UserIdWithDocument(userId);
        return bookmarks.stream()
                .map(bookmarkMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBookmarked(Long userId, Long documentId) {
        return bookmarkRepo.existsByUser_UserIdAndDocument_DocumentId(userId, documentId);
    }
}
