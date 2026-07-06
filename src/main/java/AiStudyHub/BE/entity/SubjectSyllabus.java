package AiStudyHub.BE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "subject_syllabus",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_subject_syllabus_subject", columnNames = "subject_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectSyllabus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    Subject subject;

    @Column(name = "pdf_url", length = 500)
    String pdfUrl;

    @Column(name = "plain_text", columnDefinition = "LONGTEXT")
    String plainText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_content", nullable = false)
    String jsonContent;

    @Column(name = "parser_version", length = 50)
    String parserVersion;

    @Column(name = "embedding_model", length = 100)
    String embeddingModel;

    @Builder.Default
    @Column(name = "embedding_version")
    Integer embeddingVersion = 1;

    @Builder.Default
    @Column(name = "sync_status", length = 30)
    String syncStatus = "UPLOADED";

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
        if (embeddingVersion == null) embeddingVersion = 1;
        if (syncStatus == null) syncStatus = "UPLOADED";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
