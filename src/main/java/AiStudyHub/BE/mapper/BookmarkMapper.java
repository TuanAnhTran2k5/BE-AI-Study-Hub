package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Response.BookmarkResponse;
import AiStudyHub.BE.entity.Bookmark;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookmarkMapper {

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "document.documentId", target = "documentId")
    @Mapping(source = "document.title", target = "documentTitle")
    BookmarkResponse toResponse(Bookmark bookmark);
}
