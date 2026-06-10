package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;
import AiStudyHub.BE.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "subject", ignore = true)
    Document toDocument(DocumentUploadRequest request);

    @Mapping(target = "ownerId",   source = "owner.userId")
    @Mapping(target = "subjectId", source = "subject.subjectId")
    @Mapping(target = "message",   constant = "File uploaded and metadata saved successfully")
    DocumentUploadResponse toDocumentUploadResponse(Document document);
}
