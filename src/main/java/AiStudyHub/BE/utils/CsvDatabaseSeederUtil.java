package AiStudyHub.BE.utils;

import AiStudyHub.BE.constraint.SubjectType;
import AiStudyHub.BE.entity.*;
import AiStudyHub.BE.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStreamReader;
import java.io.Reader;
import org.springframework.core.io.ClassPathResource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CsvDatabaseSeederUtil {

    private final SemesterRepo semesterRepo;
    private final ComboSubjectRepo comboSubjectRepo;
    private final SubjectRepo subjectRepo;
    private final SemesterComboSubjectRepo semesterComboSubjectRepo;
    private final ScoreTypeRepo scoreTypeRepo;
    private final RankingRepo rankingRepo;
    private final BadgeRepo badgeRepo;

    @Transactional
    public void seedSemesters() {
        if (semesterRepo.count() > 0) return;
        log.info("Seeding Semesters from CSV...");
        try (Reader reader = new InputStreamReader(new ClassPathResource("csv/semesters.csv").getInputStream())) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);
            for (CSVRecord record : records) {
                semesterRepo.save(Semester.builder()
                        .semesterNo(record.get("SemesterNo"))
                        .description(record.get("Description"))
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to seed Semesters", e);
        }
    }

    @Transactional
    public void seedComboSubjects() {
        if (comboSubjectRepo.count() > 0) return;
        log.info("Seeding ComboSubjects from CSV...");
        try (Reader reader = new InputStreamReader(new ClassPathResource("csv/combo_subjects.csv").getInputStream())) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);
            for (CSVRecord record : records) {
                comboSubjectRepo.save(ComboSubject.builder()
                        .comboCode(record.get("ComboCode"))
                        .comboName(record.get("ComboName"))
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to seed ComboSubjects", e);
        }
    }

    @Transactional
    public void seedSubjectsAndCombos() {
        if (subjectRepo.count() > 0) return;
        log.info("Seeding Subjects from CSV...");
        try (Reader reader = new InputStreamReader(new ClassPathResource("csv/subjects.csv").getInputStream())) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);
            Map<String, Semester> semesterMap = semesterRepo.findAll().stream().collect(Collectors.toMap(Semester::getSemesterNo, s -> s));
            Map<String, ComboSubject> comboMap = comboSubjectRepo.findAll().stream().collect(Collectors.toMap(ComboSubject::getComboCode, c -> c));
            List<Subject> newSubjects = new ArrayList<>();

            for (CSVRecord record : records) {
                String semesterNo = record.get("SemesterNo").trim();
                String comboCode = record.get("ComboCode").trim();
                String subjectCode = record.get("SubjectCode").trim();
                String subjectName = record.get("SubjectName").trim();
                String subjectTypeStr = record.get("SubjectType").trim().toUpperCase();

                newSubjects.add(Subject.builder()
                        .semester(semesterMap.get(semesterNo))
                        .comboSubject(comboCode.isEmpty() ? null : comboMap.get(comboCode))
                        .subjectCode(subjectCode)
                        .subjectName(subjectName)
                        .subjectType(SubjectType.valueOf(subjectTypeStr))
                        .build());
            }
            subjectRepo.saveAll(newSubjects);

            log.info("Seeding SemesterComboSubject...");
            List<SemesterComboSubject> scsList = newSubjects.stream()
                    .filter(s -> s.getSubjectType() == SubjectType.COMBO && s.getSemester() != null && s.getComboSubject() != null)
                    .map(s -> SemesterComboSubject.builder()
                            .semester(s.getSemester())
                            .combo(s.getComboSubject())
                            .build())
                    .collect(Collectors.toMap(
                            scs -> scs.getSemester().getSemesterNo() + "-" + scs.getCombo().getComboCode(),
                            scs -> scs, (existing, replacement) -> existing))
                    .values().stream().toList();
            semesterComboSubjectRepo.saveAll(scsList);
        } catch (Exception e) {
            log.error("Failed to seed Subjects", e);
        }
    }

    @Transactional
    public void seedScoreTypes() {
        if (scoreTypeRepo.count() > 0) return;
        log.info("Seeding ScoreTypes from CSV...");
        try (Reader reader = new InputStreamReader(new ClassPathResource("csv/score_types.csv").getInputStream())) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);
            List<ScoreType> scoreTypes = new ArrayList<>();
            for (CSVRecord record : records) {
                scoreTypes.add(ScoreType.builder()
                        .typeCode(record.get("TypeCode"))
                        .typeName(record.get("TypeName"))
                        .defaultPoint(Integer.parseInt(record.get("DefaultPoint")))
                        .build());
            }
            scoreTypeRepo.saveAll(scoreTypes);
        } catch (Exception e) {
            log.error("Failed to seed ScoreTypes", e);
        }
    }

    @Transactional
    public void seedRankings() {
        if (rankingRepo.count() > 0) return;
        log.info("Seeding Rankings from CSV...");
        try (Reader reader = new InputStreamReader(new ClassPathResource("csv/rankings.csv").getInputStream())) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);
            List<Ranking> rankings = new ArrayList<>();
            for (CSVRecord record : records) {
                rankings.add(Ranking.builder()
                        .rankName(record.get("RankName"))
                        .minScore(Integer.parseInt(record.get("MinScore")))
                        .maxScore(Integer.parseInt(record.get("MaxScore")))
                        .storageBonus(Long.parseLong(record.get("StorageBonus")))
                        .displayPriority(record.get("DisplayPriority"))
                        .build());
            }
            rankingRepo.saveAll(rankings);
            log.info("Seeding Rankings completed.");
        } catch (Exception e) {
            log.error("Failed to seed Rankings", e);
        }
    }

    @Transactional
    public void seedBadges() {
        if (badgeRepo.count() > 0) return;
        log.info("Seeding Badges from CSV...");
        try (Reader reader = new InputStreamReader(new ClassPathResource("csv/badges.csv").getInputStream())) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);
            List<Badge> badges = new ArrayList<>();
            for (CSVRecord record : records) {
                badges.add(Badge.builder()
                        .badgeName(record.get("BadgeName"))
                        .description(record.get("Description"))
                        .conditionText(record.get("ConditionText"))
                        .iconUrl(record.get("IconUrl"))
                        .build());
            }
            badgeRepo.saveAll(badges);
            log.info("Seeding Badges completed.");
        } catch (Exception e) {
            log.error("Failed to seed Badges", e);
        }
    }
}
