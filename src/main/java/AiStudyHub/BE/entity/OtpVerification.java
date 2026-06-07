package AiStudyHub.BE.entity;

import AiStudyHub.BE.constraint.OtpPurpose;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long otpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    User user;

    String email;
    String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    OtpPurpose purpose;

    LocalDateTime expiredAt;
    LocalDateTime verifiedAt;
    Boolean isUse = false;
    LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
