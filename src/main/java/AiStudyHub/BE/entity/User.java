package AiStudyHub.BE.entity;

import AiStudyHub.BE.constraint.AuthProvider;
import AiStudyHub.BE.constraint.UserRole;
import AiStudyHub.BE.constraint.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(
        name = "user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long userId;

    String fullName;

    @Column(columnDefinition = "TEXT")
    String avatarUrl;

    Long totalScore;

    @Column(unique = true)
    String email;

    String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    UserRole role;

    //Limit Storage
    @Builder.Default
    @Column(nullable = false)
    Long storageUsed = 0L;
    @Builder.Default
    @Column(nullable = false)
    Long storageLimit = 2L * 1024 * 1024 * 1024; //2GB

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    UserStatus status;

    String banReason;
    LocalDateTime bannedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bannedBy", referencedColumnName = "userId")
    User bannedBy;

    @Enumerated(EnumType.STRING)
    AuthProvider authProvider;

    String googleId;

    LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (role == null) role = UserRole.US;
        if (status == null) status = UserStatus.PENDING;

        if (storageUsed == null)
            storageUsed = 0L;
        if (storageLimit == null)
            storageLimit = 2L * 1024 * 1024 * 1024;
        if (totalScore == null)
            totalScore = 0L;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.email;
    }
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
