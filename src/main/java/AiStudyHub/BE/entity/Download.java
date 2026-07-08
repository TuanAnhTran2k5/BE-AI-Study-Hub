package AiStudyHub.BE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "download", uniqueConstraints = {
    @UniqueConstraint(
        name = "uq_download_user_document",
        columnNames = {"userId", "documentId"}
    )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Download {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long downloadId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documentId", nullable = false)
    Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    User user;

    LocalDateTime downloadedAt;
    @Builder.Default
    Boolean scoreAwarded = false;

    @PrePersist
    public void prePersist() {
        downloadedAt = LocalDateTime.now();
    }
}