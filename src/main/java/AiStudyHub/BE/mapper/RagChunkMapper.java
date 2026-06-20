package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Response.RagChunkResponse;
import AiStudyHub.BE.entity.RagChunk;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting {@link RagChunk} entity to DTOs.
 */
@Mapper(componentModel = "spring")
public interface RagChunkMapper {

    @Mapping(target = "documentId", source = "document.id")
    RagChunkResponse toRagChunkResponse(RagChunk chunk);

    List<RagChunkResponse> toRagChunkResponseList(List<RagChunk> chunks);
}
