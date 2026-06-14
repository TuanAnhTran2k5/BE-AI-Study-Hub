package AiStudyHub.BE.config;

import AiStudyHub.BE.constraint.SubjectType;
import AiStudyHub.BE.entity.*;
import AiStudyHub.BE.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    RankingRepo rankingRepo;
    BadgeRepo badgeRepo;
    SemesterRepo semesterRepo;
    ComboSubjectRepo comboSubjectRepo;
    SubjectRepo subjectRepo;
    SemesterComboSubjectRepo semesterComboSubjectRepo;
    ScoreTypeRepo scoreTypeRepo;

    @Override
    public void run(String... args) throws Exception {
        seedSemesters();
        seedComboSubjects();
        seedSubjectsAndCombos();
        seedScoreTypes();
        seedRankings();
        seedBadges();
    }

    private void seedSemesters() {
        if (semesterRepo.count() == 0) {
            log.info("Seeding Semesters...");
            List<Semester> semesters = List.of(
                    Semester.builder().semesterNo("1").description("Fundamental computing, programming, mathematics, and computer systems.").build(),
                    Semester.builder().semesterNo("2").description("Core programming, operating systems, web design, and networking.").build(),
                    Semester.builder().semesterNo("3").description("Data structures, database, statistics, and Java lab.").build(),
                    Semester.builder().semesterNo("4").description("Java web, software engineering, IoT, and teamwork.").build(),
                    Semester.builder().semesterNo("5").description("Software project, requirements, testing, UI/UX, and first combo subjects.").build(),
                    Semester.builder().semesterNo("6").description("Internship and academic writing.").build(),
                    Semester.builder().semesterNo("7").description("Architecture, project management, entrepreneurship, and advanced combo subjects.").build(),
                    Semester.builder().semesterNo("8").description("Ethics, political subjects, mobile, and advanced combo subjects.").build(),
                    Semester.builder().semesterNo("9").description("Final political subjects and capstone project.").build()
            );
            semesterRepo.saveAll(semesters);
        }
    }

    private void seedComboSubjects() {
        if (comboSubjectRepo.count() == 0) {
            log.info("Seeding ComboSubjects...");
            List<ComboSubject> combos = List.of(
                    ComboSubject.builder().comboCode("SPRING_REACT").comboName("Spring Boot with React").build(),
                    ComboSubject.builder().comboCode("DOTNET").comboName(".NET Developer").build(),
                    ComboSubject.builder().comboCode("REACT_NODEJS").comboName("React NodeJS Developer").build(),
                    ComboSubject.builder().comboCode("JS").comboName("Japanese Software Developer").build(),
                    ComboSubject.builder().comboCode("KS").comboName("Korean Bridge Software Engineer").build(),
                    ComboSubject.builder().comboCode("SAP").comboName("SAP Consultant").build(),
                    ComboSubject.builder().comboCode("DS").comboName("Data Science").build(),
                    ComboSubject.builder().comboCode("SECURITY").comboName("Cyber Security").build()
            );
            comboSubjectRepo.saveAll(combos);
        }
    }

    private void seedSubjectsAndCombos() {
        if (subjectRepo.count() == 0) {
            log.info("Seeding Subjects...");
            Map<String, Semester> semesterMap = semesterRepo.findAll().stream().collect(Collectors.toMap(Semester::getSemesterNo, s -> s));
            Map<String, ComboSubject> comboMap = comboSubjectRepo.findAll().stream().collect(Collectors.toMap(ComboSubject::getComboCode, c -> c));

            List<Subject> subjects = List.of(
                    // Kỳ 1
                    Subject.builder().semester(semesterMap.get("1")).comboSubject(null).subjectCode("CSI106").subjectName("Introduction to Computer Science").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("1")).comboSubject(null).subjectCode("SSL101c").subjectName("Academic Skills for University Success").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("1")).comboSubject(null).subjectCode("PRF192").subjectName("Programming Fundamentals").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("1")).comboSubject(null).subjectCode("MAE101").subjectName("Mathematics for Engineering").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("1")).comboSubject(null).subjectCode("CEA201").subjectName("Computer Organization and Architecture").description(null).subjectType(SubjectType.CORE).build(),
                    
                    // Kỳ 2
                    Subject.builder().semester(semesterMap.get("2")).comboSubject(null).subjectCode("PRO192").subjectName("Object-Oriented Programming").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("2")).comboSubject(null).subjectCode("MAD101").subjectName("Discrete Mathematics").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("2")).comboSubject(null).subjectCode("OSG202").subjectName("Operating Systems").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("2")).comboSubject(null).subjectCode("WED201c").subjectName("Web Design").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("2")).comboSubject(null).subjectCode("NWC204").subjectName("Computer Networking").description(null).subjectType(SubjectType.CORE).build(),
                    
                    // Kỳ 3
                    Subject.builder().semester(semesterMap.get("3")).comboSubject(null).subjectCode("JPD113").subjectName("Elementary Japanese 1 - A1.1").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("3")).comboSubject(null).subjectCode("CSD201").subjectName("Data Structures and Algorithms").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("3")).comboSubject(null).subjectCode("DBI202").subjectName("Database Systems").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("3")).comboSubject(null).subjectCode("MAS291").subjectName("Statistics & Probability").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("3")).comboSubject(null).subjectCode("LAB211").subjectName("OOP with Java Lab").description(null).subjectType(SubjectType.CORE).build(),
                    
                    // Kỳ 4
                    Subject.builder().semester(semesterMap.get("4")).comboSubject(null).subjectCode("JPD123").subjectName("Elementary Japanese 1 - A1.2").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("4")).comboSubject(null).subjectCode("IOT102").subjectName("Internet of Things").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("4")).comboSubject(null).subjectCode("PRJ301").subjectName("Java Web Application Development").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("4")).comboSubject(null).subjectCode("SSG104").subjectName("Communication and In-Group Working Skills").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("4")).comboSubject(null).subjectCode("SWE202c").subjectName("Introduction to Software Engineering").description(null).subjectType(SubjectType.CORE).build(),
                    
                    // Kỳ 5 CORE
                    Subject.builder().semester(semesterMap.get("5")).comboSubject(null).subjectCode("SWP391").subjectName("Software Development Project").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("5")).comboSubject(null).subjectCode("WDU203c").subjectName("The UI/UX Design").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("5")).comboSubject(null).subjectCode("SWR302").subjectName("Software Requirements").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("5")).comboSubject(null).subjectCode("SWT301").subjectName("Software Testing").description(null).subjectType(SubjectType.CORE).build(),
                    
                    // Kỳ 6 CORE
                    Subject.builder().semester(semesterMap.get("6")).comboSubject(null).subjectCode("OJT202").subjectName("On The Job Training").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("6")).comboSubject(null).subjectCode("ENW493c").subjectName("Research Methods & Academic Writing Skills").description(null).subjectType(SubjectType.CORE).build(),
                    
                    // Kỳ 7 CORE
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(null).subjectCode("SWD392").subjectName("Software Architecture and Design").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(null).subjectCode("EXE101").subjectName("Experiential Entrepreneurship 1").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(null).subjectCode("PMG201c").subjectName("Project Management").description(null).subjectType(SubjectType.CORE).build(),
                    
                    // Kỳ 8 CORE
                    Subject.builder().semester(semesterMap.get("8")).comboSubject(null).subjectCode("EXE201").subjectName("Experiential Entrepreneurship 2").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("8")).comboSubject(null).subjectCode("ITE302c").subjectName("Ethics in IT").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("8")).comboSubject(null).subjectCode("MLN122").subjectName("Political Economics of Marxism - Leninism").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("8")).comboSubject(null).subjectCode("MLN111").subjectName("Philosophy of Marxism - Leninism").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("8")).comboSubject(null).subjectCode("PRM393").subjectName("Mobile Programming").description(null).subjectType(SubjectType.CORE).build(),
                    
                    // Kỳ 9 CORE
                    Subject.builder().semester(semesterMap.get("9")).comboSubject(null).subjectCode("MLN131").subjectName("Scientific Socialism").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("9")).comboSubject(null).subjectCode("VNR202").subjectName("History of Vietnam Communist Party").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("9")).comboSubject(null).subjectCode("HCM202").subjectName("Ho Chi Minh Ideology").description(null).subjectType(SubjectType.CORE).build(),
                    Subject.builder().semester(semesterMap.get("9")).comboSubject(null).subjectCode("SEP490").subjectName("SE Capstone Project").description(null).subjectType(SubjectType.CORE).build(),
                    
                    // SPRING_REACT
                    Subject.builder().semester(semesterMap.get("5")).comboSubject(comboMap.get("SPRING_REACT")).subjectCode("HSF302").subjectName("Working with Spring Framework").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(comboMap.get("SPRING_REACT")).subjectCode("SBA301").subjectName("Integrate Single Page Application with Spring Boot").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("8")).comboSubject(comboMap.get("SPRING_REACT")).subjectCode("MSS301").subjectName("Microservices with Spring Cloud").description(null).subjectType(SubjectType.COMBO).build(),
                    
                    // DOTNET
                    Subject.builder().semester(semesterMap.get("5")).comboSubject(comboMap.get("DOTNET")).subjectCode("PRN212").subjectName("Basic Cross-Platform Application Programming With .NET").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(comboMap.get("DOTNET")).subjectCode("PRN222").subjectName("Advanced Cross-Platform Application Programming With .NET").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(comboMap.get("DOTNET")).subjectCode("PRU213").subjectName("Game Programming with C#").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("8")).comboSubject(comboMap.get("DOTNET")).subjectCode("PRN232").subjectName("Building Cross-Platform Back-End Application With .NET").description(null).subjectType(SubjectType.COMBO).build(),
                    
                    // REACT_NODEJS
                    Subject.builder().semester(semesterMap.get("5")).comboSubject(comboMap.get("REACT_NODEJS")).subjectCode("FER202").subjectName("Front-End Web Development with React").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(comboMap.get("REACT_NODEJS")).subjectCode("SDN302").subjectName("Server-Side Development with NodeJS, Express, and MongoDB").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("8")).comboSubject(comboMap.get("REACT_NODEJS")).subjectCode("WDP301").subjectName("Web Development Project").description(null).subjectType(SubjectType.COMBO).build(),
                    
                    // JS
                    Subject.builder().semester(semesterMap.get("5")).comboSubject(comboMap.get("JS")).subjectCode("JPD133").subjectName("Elementary Japanese 1 - A1/A2").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(comboMap.get("JS")).subjectCode("JPD316").subjectName("Japanese Intermediate 1 - B1/B2").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("8")).comboSubject(comboMap.get("JS")).subjectCode("JPD326").subjectName("Japanese Intermediate 2 - B2.1").description(null).subjectType(SubjectType.COMBO).build(),
                    
                    // KS
                    Subject.builder().semester(semesterMap.get("5")).comboSubject(comboMap.get("KS")).subjectCode("KOR311").subjectName("Korean 1").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(comboMap.get("KS")).subjectCode("KOR321").subjectName("Korean 2").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("8")).comboSubject(comboMap.get("KS")).subjectCode("KOR411").subjectName("Korean 3").description(null).subjectType(SubjectType.COMBO).build(),
                    
                    // SAP
                    Subject.builder().semester(semesterMap.get("5")).comboSubject(comboMap.get("SAP")).subjectCode("SAP341").subjectName("SAP Introduction").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(comboMap.get("SAP")).subjectCode("SAP311").subjectName("SAP Business Process").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(comboMap.get("SAP")).subjectCode("SAP331").subjectName("SAP Configuration").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("8")).comboSubject(comboMap.get("SAP")).subjectCode("SAP321").subjectName("SAP Project").description(null).subjectType(SubjectType.COMBO).build(),
                    
                    // DS
                    Subject.builder().semester(semesterMap.get("5")).comboSubject(comboMap.get("DS")).subjectCode("DMS301m").subjectName("Data Management System").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(comboMap.get("DS")).subjectCode("ADS301m").subjectName("Applied Data Science").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(comboMap.get("DS")).subjectCode("DAT301m").subjectName("Data Analytics").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("8")).comboSubject(comboMap.get("DS")).subjectCode("DSS301").subjectName("Data Science Specialization").description(null).subjectType(SubjectType.COMBO).build(),
                    
                    // SECURITY
                    Subject.builder().semester(semesterMap.get("5")).comboSubject(comboMap.get("SECURITY")).subjectCode("CRY303c").subjectName("Cryptography").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(comboMap.get("SECURITY")).subjectCode("ISC301").subjectName("Information Security").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("7")).comboSubject(comboMap.get("SECURITY")).subjectCode("ISC302").subjectName("Advanced Information Security").description(null).subjectType(SubjectType.COMBO).build(),
                    Subject.builder().semester(semesterMap.get("8")).comboSubject(comboMap.get("SECURITY")).subjectCode("CPV301").subjectName("Cybersecurity Practice").description(null).subjectType(SubjectType.COMBO).build()
            );
            subjectRepo.saveAll(subjects);
            
            // Generate SemesterComboSubject
            if (semesterComboSubjectRepo.count() == 0) {
                log.info("Seeding SemesterComboSubject...");
                List<SemesterComboSubject> scsList = subjects.stream()
                        .filter(s -> s.getSubjectType() == SubjectType.COMBO)
                        .map(s -> SemesterComboSubject.builder()
                                .semester(s.getSemester())
                                .combo(s.getComboSubject())
                                .build())
                        // Distinct by semesterId + comboId
                        .collect(Collectors.toMap(
                                scs -> scs.getSemester().getSemesterNo() + "-" + scs.getCombo().getComboCode(),
                                scs -> scs,
                                (existing, replacement) -> existing))
                        .values()
                        .stream()
                        .toList();
                semesterComboSubjectRepo.saveAll(scsList);
            }
        }
    }

    private void seedScoreTypes() {
        if (scoreTypeRepo.count() == 0) {
            log.info("Seeding ScoreTypes...");
            List<ScoreType> scoreTypes = List.of(
                    ScoreType.builder().typeCode("UPLOAD_PUBLIC").typeName("Upload public document").defaultPoint(5).build(),
                    ScoreType.builder().typeCode("DOC_DOWNLOAD").typeName("Document downloaded").defaultPoint(5).build(),
                    ScoreType.builder().typeCode("BOOKMARK").typeName("Document bookmarked").defaultPoint(3).build(),
                    ScoreType.builder().typeCode("GOOD_RATING").typeName("Good rating received").defaultPoint(5).build(),
                    ScoreType.builder().typeCode("RATING_REPUTATION").typeName("Daily rating reputation (computed by average-rating threshold)").defaultPoint(0).build(),
                    ScoreType.builder().typeCode("REPORT_MINOR_FIRST_PENALTY").typeName("Minor report first penalty").defaultPoint(-5).build(),
                    ScoreType.builder().typeCode("REPORT_MINOR_FINAL_PENALTY").typeName("Minor report final penalty").defaultPoint(-10).build(),
                    ScoreType.builder().typeCode("ADS_CONTENT_PENALTY").typeName("Advertisement content penalty").defaultPoint(-15).build(),
                    ScoreType.builder().typeCode("DUPLICATE_CONTENT_PENALTY").typeName("Duplicate content penalty").defaultPoint(-20).build()
            );
            scoreTypeRepo.saveAll(scoreTypes);
        }
    }

    private void seedRankings() {
        if (rankingRepo.count() == 0) {
            log.info("Seeding Rankings...");
            List<Ranking> rankings = List.of(
                    Ranking.builder().rankName("Bronze").minScore(0).maxScore(100).storageBonus(0).displayPriority("1").build(),
                    Ranking.builder().rankName("Silver").minScore(101).maxScore(300).storageBonus(500 * 1024 * 1024).displayPriority("2").build(),
                    Ranking.builder().rankName("Gold").minScore(301).maxScore(700).storageBonus(1024 * 1024 * 1024).displayPriority("3").build(),
                    Ranking.builder().rankName("Elite Scholar").minScore(701).maxScore(Integer.MAX_VALUE).storageBonus(2L * 1024 * 1024 * 1024 > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)(2L * 1024 * 1024 * 1024)).displayPriority("4").build()
            );
            rankingRepo.saveAll(rankings);
            log.info("Seeding Rankings completed.");
        }
    }

    private void seedBadges() {
        if (badgeRepo.count() == 0) {
            log.info("Seeding Badges...");
            List<Badge> badges = List.of(
                    Badge.builder().badgeName("First Upload").description("User upload file đầu tiên").conditionText("Upload 1 document").iconUrl("first_upload_icon").build(),
                    Badge.builder().badgeName("Helpful Student").description("Tài liệu đạt 50 lượt tải").conditionText("Any document reaches 50 downloads").iconUrl("helpful_student_icon").build(),
                    Badge.builder().badgeName("Trusted Author").description("Tổng lượt tải đạt 500").conditionText("Total downloads reach 500").iconUrl("trusted_author_icon").build(),
                    Badge.builder().badgeName("Top Weekly Contributor").description("Top 3 điểm hoạt động tuần, dựa trên Reputation Score").conditionText("Top 3 weekly scores").iconUrl("top_weekly_icon").build()
            );
            badgeRepo.saveAll(badges);
            log.info("Seeding Badges completed.");
        }
    }
}
