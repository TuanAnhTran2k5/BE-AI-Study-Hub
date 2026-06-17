package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Response.RagDocumentResponse;
import AiStudyHub.BE.entity.RagDocument;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for converting {@link RagDocument} entity to DTOs.
 */
@Mapper(componentModel = "spring")
public interface RagDocumentMapper {

    RagDocumentResponse toRagDocumentResponse(RagDocument document);

    List<RagDocumentResponse> toRagDocumentResponseList(List<RagDocument> documents);
}
