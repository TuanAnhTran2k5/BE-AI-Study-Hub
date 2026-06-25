package AiStudyHub.BE.service;

import AiStudyHub.BE.entity.Document;

public interface IDuplicateCheck {

    Document performDuplicateCheck(Long documentId, byte[] fileBytes);
}
