package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Response.BookmarkResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.entity.Bookmark;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface BookmarkMapper {

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "document.documentId", target = "documentId")
    @Mapping(source = "document.title", target = "documentTitle")
    BookmarkResponse toResponse(Bookmark bookmark);

    @Mapping(target = "success", constant = "true")
    @Mapping(target = "message", constant = "Bookmark removed successfully")
    @Mapping(source = "bookmark.bookmarkId", target = "deletedId")
    @Mapping(target = "entityName", constant = "Bookmark")
    @Mapping(source = "bookmark.document.title", target = "entityIdentifier")
    @Mapping(source = "deletedAt", target = "deletedAt")
    DeleteResponse toDeleteResponse(Bookmark bookmark, LocalDateTime deletedAt);
}
