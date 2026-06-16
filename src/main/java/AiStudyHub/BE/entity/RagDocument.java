package AiStudyHub.BE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * JPA entity representing metadata for an uploaded RAG document.
 */
@Entity
@Table(name = "rag_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "original_file_name", nullable = false)
    String originalFileName;

    @Column(name = "content_type", nullable = false)
    String contentType;

    @Column(name = "file_size", nullable = false)
    Long fileSize;

    @Column(name = "uploaded_by", nullable = false)
    String uploadedBy;

    @Column(name = "upload_date", nullable = false)
    LocalDateTime uploadDate;

    @Column(name = "status", nullable = false, length = 30)
    String status; // e.g. PENDING, INDEXED, FAILED

    @Column(name = "document_id")
    Long documentId;

    @PrePersist
    public void prePersist() {
        if (this.uploadDate == null) {
            this.uploadDate = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}
