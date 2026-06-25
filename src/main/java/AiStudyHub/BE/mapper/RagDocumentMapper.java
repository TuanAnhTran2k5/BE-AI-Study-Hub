package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.RagDocumentResponse;
import AiStudyHub.BE.entity.RagDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface RagDocumentMapper {

    RagDocumentResponse toRagDocumentResponse(RagDocument document);

    List<RagDocumentResponse> toRagDocumentResponseList(List<RagDocument> documents);

    @Mapping(target = "success", constant = "true")
    @Mapping(target = "message", constant = "RagDocument deleted successfully")
    @Mapping(source = "document.document.documentId", target = "deletedId")
    @Mapping(target = "entityName", constant = "RagDocument")
    @Mapping(source = "document.originalFileName", target = "entityIdentifier")
    @Mapping(source = "deletedAt", target = "deletedAt")
    DeleteResponse toDeleteResponse(RagDocument document, LocalDateTime deletedAt);
}
