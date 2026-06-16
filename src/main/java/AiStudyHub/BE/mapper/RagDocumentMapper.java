package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Response.UploadDocumentResponse;
import AiStudyHub.BE.entity.RagDocument;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for converting {@link RagDocument} entity to DTOs.
 */
@Mapper(componentModel = "spring")
public interface RagDocumentMapper {

    UploadDocumentResponse toUploadDocumentResponse(RagDocument document);

    List<UploadDocumentResponse> toUploadDocumentResponseList(List<RagDocument> documents);
}
