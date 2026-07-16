package AiStudyHub.BE.config;

import AiStudyHub.BE.constraint.AuthProvider;
import AiStudyHub.BE.constraint.UserRole;
import AiStudyHub.BE.constraint.UserStatus;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.utils.CsvDatabaseSeederUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    CsvDatabaseSeederUtil seederUtil;
    UserRepo userRepo;
    PasswordEncoder passwordEncoder;
    SystemAdminProperties systemAdminProperties;

    @Override
    public void run(String... args) throws Exception {
        seederUtil.seedSemesters();
        seederUtil.seedComboSubjects();
        seederUtil.seedSubjectsAndCombos();
        seederUtil.seedScoreTypes();
        seederUtil.seedRankings();
        seederUtil.seedBadges();
        seedSystemAdmin();
    }

    private void seedSystemAdmin() {
        String email = systemAdminProperties.getEmail();
        if (email == null || email.isBlank()) {
            log.warn("System Admin Email is not configured. Skipping seeding.");
            return;
        }

        if (userRepo.findByEmail(email).isEmpty()) {
            log.info("Seeding System Admin Account: {}", email);
            User systemAdmin = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(systemAdminProperties.getPassword()))
                    .fullName(systemAdminProperties.getFullName())
                    .role(UserRole.AD)
                    .status(UserStatus.ACTIVE)
                    .authProvider(AuthProvider.LOCAL)
                    .build();
            userRepo.save(systemAdmin);
            log.info("System Admin Account seeded successfully.");
        }
    }
}

