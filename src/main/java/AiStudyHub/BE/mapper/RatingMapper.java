package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Response.RatingResponse;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Rating;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RatingMapper {

    @Mapping(target = "ratingId",      source = "rating.ratingId")
    @Mapping(target = "documentId",    source = "document.documentId")
    @Mapping(target = "ratingValue",   source = "rating.ratingValue")
    @Mapping(target = "averageRating", source = "document.averageRating")
    @Mapping(target = "ratingCount",   source = "document.ratingCount")
    RatingResponse toRatingResponse(Rating rating, Document document);
}
