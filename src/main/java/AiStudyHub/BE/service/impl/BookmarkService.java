package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.dto.Request.BookmarkRequest;
import AiStudyHub.BE.dto.Response.BookmarkResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.entity.Bookmark;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.BookmarkMapper;
import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.repository.BookmarkRepo;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.IBookmark;
import AiStudyHub.BE.service.IGamification;
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
    IGamification gamificationService;

    @Override
    @Transactional
    public BookmarkResponse addBookmark(Long userId, BookmarkRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        Document document = documentRepo.findById(request.getDocumentId())
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND));

        // If this is a copy, bookmark the original public document
        Document targetDocument = document.getSourceDocument() != null ? document.getSourceDocument() : document;

        // Check if already bookmarked to prevent duplicate insertions
        if (bookmarkRepo.existsByUserUserIdAndDocumentDocumentId(userId, targetDocument.getDocumentId())) {
            throw new GlobalException(400, "Document already bookmarked");
        }

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .document(targetDocument)
                .build();

        bookmark = bookmarkRepo.saveAndFlush(bookmark);

        // Update the target document's bookmarkCount cache
        int currentCount = targetDocument.getBookmarkCount() != null ? targetDocument.getBookmarkCount() : 0;
        targetDocument.setBookmarkCount(currentCount + 1);
        documentRepo.save(targetDocument);

        // Award points if the original document is PUBLIC and not owned by the bookmarking user
        boolean isPublic = targetDocument.getVisibilityStatus() == VisibilityStatus.PUBLIC;
        boolean isNotSelf = !targetDocument.getOwner().getUserId().equals(userId);

        if (isPublic && isNotSelf) {
            gamificationService.awardBookmarkScore(
                    user.getUserId(),
                    user.getFullName(),
                    targetDocument.getOwner().getUserId(),
                    targetDocument.getDocumentId(),
                    targetDocument.getTitle(),
                    targetDocument.getVisibilityStatus().name()
            );
        }

        BookmarkResponse response = bookmarkMapper.toResponse(bookmark);
        response.setBookmarkCount((long) targetDocument.getBookmarkCount());
        response.setIsBookmarked(true);
        return response;
    }

    @Override
    @Transactional
    public DeleteResponse removeBookmark(Long userId, Long documentId) {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND));
        
        // If this is a copy, resolve to the original public document
        Document targetDocument = document.getSourceDocument() != null ? document.getSourceDocument() : document;

        Bookmark bookmark = bookmarkRepo.findByUserUserIdAndDocumentDocumentId(userId, targetDocument.getDocumentId())
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND));

        bookmarkRepo.delete(bookmark);

        // Update the target document's bookmarkCount cache
        int currentCount = targetDocument.getBookmarkCount() != null ? targetDocument.getBookmarkCount() : 0;
        if (currentCount > 0) {
            targetDocument.setBookmarkCount(currentCount - 1);
            documentRepo.save(targetDocument);
        }
        
        return DeleteResponse.builder()
                .success(true)
                .message("Bookmark removed successfully")
                .deletedId(bookmark.getBookmarkId())
                .entityName("Bookmark")
                .entityIdentifier(String.valueOf(bookmark.getBookmarkId()))
                .deletedAt(java.time.LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookmarkResponse> getBookmarksByUser(Long userId) {
        if (!userRepo.existsById(userId)) {
            throw new GlobalException(ErrorCode.USER_NOT_FOUND);
        }

        List<Bookmark> bookmarks = bookmarkRepo.findByUserUserId(userId);
        return bookmarks.stream()
                .map(bookmarkMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBookmarked(Long userId, Long documentId) {
        return bookmarkRepo.existsByUserUserIdAndDocumentDocumentId(userId, documentId);
    }
}
