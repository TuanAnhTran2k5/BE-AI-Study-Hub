package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepo extends JpaRepository<Document, Long> {

}