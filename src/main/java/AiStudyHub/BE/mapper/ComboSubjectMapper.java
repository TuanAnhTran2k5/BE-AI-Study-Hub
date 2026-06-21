package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Response.ComboSubjectResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.entity.ComboSubject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface ComboSubjectMapper {

    @Mapping(target = "subjects", ignore = true)
    ComboSubjectResponse toComboSubjectResponse(ComboSubject comboSubject);

    @Mapping(target = "success", constant = "true")
    @Mapping(target = "message", constant = "ComboSubject and its Subjects soft-deleted successfully")
    @Mapping(source = "combo.comboId", target = "deletedId")
    @Mapping(target = "entityName", constant = "ComboSubject")
    @Mapping(source = "combo.comboCode", target = "entityIdentifier")
    @Mapping(source = "deletedAt", target = "deletedAt")
    DeleteResponse toDeleteResponse(ComboSubject combo, LocalDateTime deletedAt);
}
