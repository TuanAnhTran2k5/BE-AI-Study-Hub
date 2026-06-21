package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SemesterResponse;
import AiStudyHub.BE.entity.Semester;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface SemesterMapper {

    SemesterResponse toSemesterResponse(Semester semester);

    @Mapping(target = "success", constant = "true")
    @Mapping(target = "message", constant = "Semester deleted successfully")
    @Mapping(source = "semester.semesterId", target = "deletedId")
    @Mapping(target = "entityName", constant = "Semester")
    @Mapping(target = "entityIdentifier", expression = "java(String.valueOf(semester.getSemesterNo()))")
    @Mapping(source = "deletedAt", target = "deletedAt")
    DeleteResponse toDeleteResponse(Semester semester, LocalDateTime deletedAt);
}
