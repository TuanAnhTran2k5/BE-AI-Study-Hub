package AiStudyHub.BE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "subject_syllabus_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectSyllabusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_syllabus_id", nullable = false)
    SubjectSyllabus subjectSyllabus;

    @Column(name = "pdf_url", length = 500)
    String pdfUrl;

    @Column(name = "plain_text", columnDefinition = "LONGTEXT")
    String plainText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_content", nullable = false)
    String jsonContent;

    @Column(nullable = false)
    Integer version;

    @Column(name = "updated_by", nullable = false, length = 100)
    String updatedBy;

    @Column(name = "updated_reason", columnDefinition = "TEXT")
    String updatedReason;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
