package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;
import AiStudyHub.BE.entity.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface SubjectMapper {

    @Mapping(target = "semesterId", source = "semester.semesterId")
    @Mapping(target = "semesterNo", source = "semester.semesterNo")
    @Mapping(target = "comboId", source = "comboSubject.comboId")
    @Mapping(target = "comboCode", source = "comboSubject.comboCode")
    @Mapping(target = "comboName", source = "comboSubject.comboName")
    SubjectResponse toSubjectResponse(Subject subject);

    @Mapping(target = "success", constant = "true")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "subject.subjectId", target = "deletedId")
    @Mapping(target = "entityName", constant = "Subject")
    @Mapping(source = "subject.subjectCode", target = "entityIdentifier")
    @Mapping(source = "deletedAt", target = "deletedAt")
    DeleteResponse toDeleteResponse(Subject subject, String message, LocalDateTime deletedAt);
}
