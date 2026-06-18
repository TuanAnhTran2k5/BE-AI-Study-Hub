package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Response.ComboSubjectResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SemesterResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;
import AiStudyHub.BE.entity.ComboSubject;
import AiStudyHub.BE.entity.Semester;
import AiStudyHub.BE.entity.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface CurriculumMapper {

    SemesterResponse toSemesterResponse(Semester semester);

    @Mapping(target = "semesterId", source = "semester.semesterId")
    @Mapping(target = "semesterNo", source = "semester.semesterNo")
    @Mapping(target = "comboId", source = "comboSubject.comboId")
    @Mapping(target = "comboCode", source = "comboSubject.comboCode")
    @Mapping(target = "comboName", source = "comboSubject.comboName")
    SubjectResponse toSubjectResponse(Subject subject);

    @Mapping(target = "subjects", ignore = true)
    ComboSubjectResponse toComboSubjectResponse(ComboSubject comboSubject);

    @Mapping(target = "success", constant = "true")
    @Mapping(target = "message", constant = "Semester deleted successfully")
    @Mapping(source = "semester.semesterId", target = "deletedId")
    @Mapping(target = "entityName", constant = "Semester")
    @Mapping(target = "entityIdentifier", expression = "java(String.valueOf(semester.getSemesterNo()))")
    @Mapping(source = "deletedAt", target = "deletedAt")
    DeleteResponse toDeleteResponse(Semester semester, LocalDateTime deletedAt);

    @Mapping(target = "success", constant = "true")
    @Mapping(target = "message", constant = "ComboSubject and its Subjects soft-deleted successfully")
    @Mapping(source = "combo.comboId", target = "deletedId")
    @Mapping(target = "entityName", constant = "ComboSubject")
    @Mapping(source = "combo.comboCode", target = "entityIdentifier")
    @Mapping(source = "deletedAt", target = "deletedAt")
    DeleteResponse toDeleteResponse(ComboSubject combo, LocalDateTime deletedAt);

    @Mapping(target = "success", constant = "true")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "subject.subjectId", target = "deletedId")
    @Mapping(target = "entityName", constant = "Subject")
    @Mapping(source = "subject.subjectCode", target = "entityIdentifier")
    @Mapping(source = "deletedAt", target = "deletedAt")
    DeleteResponse toDeleteResponse(Subject subject, String message, LocalDateTime deletedAt);
}
