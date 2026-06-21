package AiStudyHub.BE.config;

import AiStudyHub.BE.utils.CsvDatabaseSeederUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DatabaseSeeder implements CommandLineRunner {

    CsvDatabaseSeederUtil seederUtil;

    @Override
    public void run(String... args) throws Exception {
        seederUtil.seedSemesters();
        seederUtil.seedComboSubjects();
        seederUtil.seedSubjectsAndCombos();
        seederUtil.seedScoreTypes();
        seederUtil.seedRankings();
        seederUtil.seedBadges();
    }
}
