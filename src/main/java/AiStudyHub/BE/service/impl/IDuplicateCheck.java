package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.entity.Document;

public interface IDuplicateCheck {

    Document performDuplicateCheck(Long documentId, byte[] fileBytes);
}
