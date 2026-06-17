package AiStudyHub.BE.dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RatingRequest {

    // Include in request; null -> FIELD_REQUIRED. Range 1..5 checked in service -> INVALID_RATING_VALUE
    Integer ratingValue;

    // Optional
    String comment;
}
