package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.DocumentDownloadResponse;
import AiStudyHub.BE.dto.Response.DocumentUpdateResponse;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;
import AiStudyHub.BE.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "subject", ignore = true)
    Document toDocument(DocumentUploadRequest request);

    @Mapping(target = "ownerId",   source = "owner.userId")
    @Mapping(target = "subjectId", source = "subject.subjectId")
    @Mapping(target = "message",   constant = "File uploaded and metadata saved successfully")
    DocumentUploadResponse toDocumentUploadResponse(Document document);



    @Mapping(source = "document.documentId", target = "documentId")
    @Mapping(source = "document.title", target = "title")
    @Mapping(source = "document.fileName", target = "fileName")
    @Mapping(source = "document.fileType", target = "fileType")
    @Mapping(source = "document.fileSize", target = "fileSize")
    @Mapping(source = "document.owner.userId", target = "ownerId")
    @Mapping(source = "document.owner.fullName", target = "ownerName")
    @Mapping(source = "firstDownload", target = "firstDownload")
    @Mapping(source = "addedPoint", target = "addedPoint")
    @Mapping(source = "ownerTotalScore", target = "ownerTotalScore")
    @Mapping(source = "downloadedAt", target = "downloadedAt")
    DocumentDownloadResponse toDocumentDownloadResponse(
            Document document,
            Boolean firstDownload,
            Integer addedPoint,
            Long ownerTotalScore,
            LocalDateTime downloadedAt
    );

    @Mapping(target = "success", constant = "true")
    @Mapping(target = "message", constant = "Document deleted successfully")
    @Mapping(source = "document.documentId", target = "deletedId")
    @Mapping(target = "entityName", constant = "Document")
    @Mapping(target = "entityIdentifier", expression = "java(document.getTitle() != null ? document.getTitle() : document.getFileName())")
    @Mapping(source = "deletedAt", target = "deletedAt")
    DeleteResponse toDeleteResponse(
            Document document,
            LocalDateTime deletedAt
    );

    @Mapping(source = "documentId", target = "documentId")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "subject.subjectId", target = "subjectId")
    @Mapping(source = "visibilityStatus", target = "visibilityStatus")
    @Mapping(source = "updatedAt", target = "updatedAt")
    DocumentUpdateResponse toDocumentUpdateResponse(Document document);
}
