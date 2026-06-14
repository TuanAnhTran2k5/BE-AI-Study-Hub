--data.sql
INSERT IGNORE INTO semester (semester_no, description)
VALUES
('1', 'Fundamental computing, programming, mathematics, and computer systems.'),
('2', 'Core programming, operating systems, web design, and networking.'),
('3', 'Data structures, database, statistics, and Java lab.'),
('4', 'Java web, software engineering, IoT, and teamwork.'),
('5', 'Software project, requirements, testing, UI/UX, and first combo subjects.'),
('6', 'Internship and academic writing.'),
('7', 'Architecture, project management, entrepreneurship, and advanced combo subjects.'),
('8', 'Ethics, political subjects, mobile, and advanced combo subjects.'),
('9', 'Final political subjects and capstone project.');


INSERT IGNORE INTO combo_subject (combo_code, combo_name)
VALUES
('SPRING_REACT', 'Spring Boot with React'),
('DOTNET', '.NET Developer'),
('REACT_NODEJS', 'React NodeJS Developer'),
('JS', 'Japanese Software Developer'),
('KS', 'Korean Bridge Software Engineer'),
('SAP', 'SAP Consultant'),
('DS', 'Data Science'),
('SECURITY', 'Cyber Security');


INSERT IGNORE INTO subject
(semester_id, combo_id, subject_code, subject_name, description, subject_type)
VALUES
-- Kỳ 1
((SELECT semester_id FROM semester WHERE semester_no='1'), NULL, 'CSI106', 'Introduction to Computer Science', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='1'), NULL, 'SSL101c', 'Academic Skills for University Success', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='1'), NULL, 'PRF192', 'Programming Fundamentals', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='1'), NULL, 'MAE101', 'Mathematics for Engineering', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='1'), NULL, 'CEA201', 'Computer Organization and Architecture', NULL, 'CORE'),

-- Kỳ 2
((SELECT semester_id FROM semester WHERE semester_no='2'), NULL, 'PRO192', 'Object-Oriented Programming', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='2'), NULL, 'MAD101', 'Discrete Mathematics', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='2'), NULL, 'OSG202', 'Operating Systems', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='2'), NULL, 'WED201c', 'Web Design', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='2'), NULL, 'NWC204', 'Computer Networking', NULL, 'CORE'),

-- Kỳ 3
((SELECT semester_id FROM semester WHERE semester_no='3'), NULL, 'JPD113', 'Elementary Japanese 1 - A1.1', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='3'), NULL, 'CSD201', 'Data Structures and Algorithms', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='3'), NULL, 'DBI202', 'Database Systems', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='3'), NULL, 'MAS291', 'Statistics & Probability', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='3'), NULL, 'LAB211', 'OOP with Java Lab', NULL, 'CORE'),

-- Kỳ 4
((SELECT semester_id FROM semester WHERE semester_no='4'), NULL, 'JPD123', 'Elementary Japanese 1 - A1.2', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='4'), NULL, 'IOT102', 'Internet of Things', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='4'), NULL, 'PRJ301', 'Java Web Application Development', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='4'), NULL, 'SSG104', 'Communication and In-Group Working Skills', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='4'), NULL, 'SWE202c', 'Introduction to Software Engineering', NULL, 'CORE'),

-- Kỳ 5 CORE
((SELECT semester_id FROM semester WHERE semester_no='5'), NULL, 'SWP391', 'Software Development Project', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='5'), NULL, 'WDU203c', 'The UI/UX Design', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='5'), NULL, 'SWR302', 'Software Requirements', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='5'), NULL, 'SWT301', 'Software Testing', NULL, 'CORE'),

-- Kỳ 6 CORE
((SELECT semester_id FROM semester WHERE semester_no='6'), NULL, 'OJT202', 'On The Job Training', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='6'), NULL, 'ENW493c', 'Research Methods & Academic Writing Skills', NULL, 'CORE'),

-- Kỳ 7 CORE
((SELECT semester_id FROM semester WHERE semester_no='7'), NULL, 'SWD392', 'Software Architecture and Design', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='7'), NULL, 'EXE101', 'Experiential Entrepreneurship 1', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='7'), NULL, 'PMG201c', 'Project Management', NULL, 'CORE'),

-- Kỳ 8 CORE
((SELECT semester_id FROM semester WHERE semester_no='8'), NULL, 'EXE201', 'Experiential Entrepreneurship 2', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='8'), NULL, 'ITE302c', 'Ethics in IT', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='8'), NULL, 'MLN122', 'Political Economics of Marxism - Leninism', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='8'), NULL, 'MLN111', 'Philosophy of Marxism - Leninism', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='8'), NULL, 'PRM393', 'Mobile Programming', NULL, 'CORE'),

-- Kỳ 9 CORE
((SELECT semester_id FROM semester WHERE semester_no='9'), NULL, 'MLN131', 'Scientific Socialism', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='9'), NULL, 'VNR202', 'History of Vietnam Communist Party', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='9'), NULL, 'HCM202', 'Ho Chi Minh Ideology', NULL, 'CORE'),
((SELECT semester_id FROM semester WHERE semester_no='9'), NULL, 'SEP490', 'SE Capstone Project', NULL, 'CORE');


INSERT IGNORE INTO subject
(semester_id, combo_id, subject_code, subject_name, description, subject_type)
VALUES
-- SPRING_REACT
((SELECT semester_id FROM semester WHERE semester_no='5'), (SELECT combo_id FROM combo_subject WHERE combo_code='SPRING_REACT'), 'HSF302', 'Working with Spring Framework', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='7'), (SELECT combo_id FROM combo_subject WHERE combo_code='SPRING_REACT'), 'SBA301', 'Integrate Single Page Application with Spring Boot', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='8'), (SELECT combo_id FROM combo_subject WHERE combo_code='SPRING_REACT'), 'MSS301', 'Microservices with Spring Cloud', NULL, 'COMBO'),

-- DOTNET
((SELECT semester_id FROM semester WHERE semester_no='5'), (SELECT combo_id FROM combo_subject WHERE combo_code='DOTNET'), 'PRN212', 'Basic Cross-Platform Application Programming With .NET', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='7'), (SELECT combo_id FROM combo_subject WHERE combo_code='DOTNET'), 'PRN222', 'Advanced Cross-Platform Application Programming With .NET', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='7'), (SELECT combo_id FROM combo_subject WHERE combo_code='DOTNET'), 'PRU213', 'Game Programming with C#', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='8'), (SELECT combo_id FROM combo_subject WHERE combo_code='DOTNET'), 'PRN232', 'Building Cross-Platform Back-End Application With .NET', NULL, 'COMBO'),

-- REACT_NODEJS
((SELECT semester_id FROM semester WHERE semester_no='5'), (SELECT combo_id FROM combo_subject WHERE combo_code='REACT_NODEJS'), 'FER202', 'Front-End Web Development with React', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='7'), (SELECT combo_id FROM combo_subject WHERE combo_code='REACT_NODEJS'), 'SDN302', 'Server-Side Development with NodeJS, Express, and MongoDB', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='8'), (SELECT combo_id FROM combo_subject WHERE combo_code='REACT_NODEJS'), 'WDP301', 'Web Development Project', NULL, 'COMBO'),

-- JS
((SELECT semester_id FROM semester WHERE semester_no='5'), (SELECT combo_id FROM combo_subject WHERE combo_code='JS'), 'JPD133', 'Elementary Japanese 1 - A1/A2', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='7'), (SELECT combo_id FROM combo_subject WHERE combo_code='JS'), 'JPD316', 'Japanese Intermediate 1 - B1/B2', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='8'), (SELECT combo_id FROM combo_subject WHERE combo_code='JS'), 'JPD326', 'Japanese Intermediate 2 - B2.1', NULL, 'COMBO'),

-- KS
((SELECT semester_id FROM semester WHERE semester_no='5'), (SELECT combo_id FROM combo_subject WHERE combo_code='KS'), 'KOR311', 'Korean 1', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='7'), (SELECT combo_id FROM combo_subject WHERE combo_code='KS'), 'KOR321', 'Korean 2', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='8'), (SELECT combo_id FROM combo_subject WHERE combo_code='KS'), 'KOR411', 'Korean 3', NULL, 'COMBO'),

-- SAP
((SELECT semester_id FROM semester WHERE semester_no='5'), (SELECT combo_id FROM combo_subject WHERE combo_code='SAP'), 'SAP341', 'SAP Introduction', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='7'), (SELECT combo_id FROM combo_subject WHERE combo_code='SAP'), 'SAP311', 'SAP Business Process', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='7'), (SELECT combo_id FROM combo_subject WHERE combo_code='SAP'), 'SAP331', 'SAP Configuration', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='8'), (SELECT combo_id FROM combo_subject WHERE combo_code='SAP'), 'SAP321', 'SAP Project', NULL, 'COMBO'),

-- DS
((SELECT semester_id FROM semester WHERE semester_no='5'), (SELECT combo_id FROM combo_subject WHERE combo_code='DS'), 'DMS301m', 'Data Management System', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='7'), (SELECT combo_id FROM combo_subject WHERE combo_code='DS'), 'ADS301m', 'Applied Data Science', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='7'), (SELECT combo_id FROM combo_subject WHERE combo_code='DS'), 'DAT301m', 'Data Analytics', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='8'), (SELECT combo_id FROM combo_subject WHERE combo_code='DS'), 'DSS301', 'Data Science Specialization', NULL, 'COMBO'),

-- SECURITY
((SELECT semester_id FROM semester WHERE semester_no='5'), (SELECT combo_id FROM combo_subject WHERE combo_code='SECURITY'), 'CRY303c', 'Cryptography', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='7'), (SELECT combo_id FROM combo_subject WHERE combo_code='SECURITY'), 'ISC301', 'Information Security', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='7'), (SELECT combo_id FROM combo_subject WHERE combo_code='SECURITY'), 'ISC302', 'Advanced Information Security', NULL, 'COMBO'),
((SELECT semester_id FROM semester WHERE semester_no='8'), (SELECT combo_id FROM combo_subject WHERE combo_code='SECURITY'), 'CPV301', 'Cybersecurity Practice', NULL, 'COMBO');


INSERT IGNORE INTO semester_combo_subject (semester_id, combo_id)
SELECT DISTINCT semester_id, combo_id
FROM subject
WHERE subject_type = 'COMBO';

INSERT INTO score_type (type_code, type_name, default_point)
VALUES
    ('UPLOAD_PUBLIC', 'Upload public document', 5),
-- typeCode must match DocumentService.downloadPublicDocument (find-or-create 'DOC_DOWNLOAD', +5)
    ('DOC_DOWNLOAD', 'Document downloaded', 5),
    ('BOOKMARK', 'Document bookmarked', 3),
    ('GOOD_RATING', 'Good rating received', 5),

-- Rating reputation (daily job; actual score is computed dynamically from the
-- average-rating threshold table: +10/+8/+5/+2/+1/-5 when ratingCount >= 10,
-- so default_point here is nominal/unused by the job logic).
    ('RATING_REPUTATION', 'Daily rating reputation (computed by average-rating threshold)', 0),

-- Report Management - Level 1
    ('REPORT_MINOR_FIRST_PENALTY', 'Minor report first penalty', -5),
    ('REPORT_MINOR_FINAL_PENALTY', 'Minor report final penalty', -10),

-- Report Management - Level 2
    ('ADS_CONTENT_PENALTY', 'Advertisement content penalty', -15),
    ('DUPLICATE_CONTENT_PENALTY', 'Duplicate content penalty', -20)
    ON DUPLICATE KEY UPDATE
                         type_name = VALUES(type_name),
                         default_point = VALUES(default_point);