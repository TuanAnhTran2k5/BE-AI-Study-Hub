package AiStudyHub.BE.entity;

import AiStudyHub.BE.constraint.ModerationStatus;
import AiStudyHub.BE.constraint.VisibilityStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownerId", nullable = false)
    User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subjectId", nullable = false)
    Subject subject;

    String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    VisibilityStatus visibilityStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    ModerationStatus moderationStatus;

    @Builder.Default
    Double averageRating = 0.0;
    String fileName;

    @Column(columnDefinition = "TEXT")
    String fileUrl;

    String fileType;
    Long fileSize;

    @Column(columnDefinition = "TEXT")
    String simHashContent;

    @Builder.Default
    Integer ratingCount = 0;

    @Builder.Default
    Integer downloadCount = 0;
    @Builder.Default
    Integer bookmarkCount = 0;
    @Builder.Default
    Integer reportCount = 0;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (visibilityStatus == null) visibilityStatus = VisibilityStatus.PRIVATE;
        if (moderationStatus == null) moderationStatus = ModerationStatus.NORMAL;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
