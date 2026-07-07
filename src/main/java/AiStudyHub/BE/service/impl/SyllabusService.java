package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.entity.SubjectSyllabus;
import AiStudyHub.BE.entity.SubjectSyllabusHistory;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.SubjectRepo;
import AiStudyHub.BE.repository.SubjectSyllabusHistoryRepo;
import AiStudyHub.BE.repository.SubjectSyllabusRepo;
import AiStudyHub.BE.service.ISyllabusService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyllabusService implements ISyllabusService {

    private final SubjectRepo subjectRepo;
    private final SubjectSyllabusRepo subjectSyllabusRepo;
    private final SubjectSyllabusHistoryRepo subjectSyllabusHistoryRepo;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SubjectSyllabus initSyllabusUpload(Long subjectId, String pdfUrl, byte[] pdfBytes, String adminUsername) {
        log.info("Initializing syllabus upload for subject ID: {} by admin: {}", subjectId, adminUsername);
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> new GlobalException(404, "Subject not found with ID: " + subjectId));

        // 1. Extract plain text from PDF using Tika
        String plainText = "";
        try (InputStream stream = new ByteArrayInputStream(pdfBytes)) {
            BodyContentHandler handler = new BodyContentHandler(-1); // No limit
            AutoDetectParser parser = new AutoDetectParser();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            parser.parse(stream, handler, metadata, context);
            plainText = handler.toString();
        } catch (Exception e) {
            log.error("Failed to extract text from PDF for subject ID: {}", subjectId, e);
            throw new GlobalException(500, "Failed to parse PDF file content", e);
        }

        if (plainText.trim().isEmpty()) {
            throw new GlobalException(400, "PDF contains no extractable text");
        }

        // 2. Save or update the subject syllabus record in UPLOADED state
        SubjectSyllabus syllabus = subjectSyllabusRepo.findBySubjectSubjectId(subjectId)
                .orElseGet(() -> SubjectSyllabus.builder()
                        .subject(subject)
                        .embeddingVersion(0)
                        .build());

        syllabus.setPdfUrl(pdfUrl);
        syllabus.setPlainText(plainText);
        syllabus.setJsonContent("{}"); // Initial empty JSON
        syllabus.setSyncStatus("UPLOADED");
        syllabus.setParserVersion("regex-v1.0-gpt-4o-mini");
        syllabus.setEmbeddingModel("text-embedding-3-small");
        syllabus = subjectSyllabusRepo.saveAndFlush(syllabus);

        // 3. Trigger async parsing
        // We will call the method directly since it is marked with @Async
        // (Self-invocation bypass can be avoided by utilizing ApplicationEventPublisher or calling through proxy, 
        //  but the cleanest Spring way is to publish an event or trigger it via proxy. Let's publish a spring event).
        eventPublisher.publishEvent(new SyllabusProcessEvent(syllabus.getId()));

        return syllabus;
    }

    @Async
    @EventListener
    @Transactional
    public void handleSyllabusProcessEvent(SyllabusProcessEvent event) {
        parseSyllabusAsync(event.getSyllabusId());
    }

    @Override
    @Transactional
    public void parseSyllabusAsync(Long syllabusId) {
        log.info("Starting async syllabus parsing for ID: {}", syllabusId);
        SubjectSyllabus syllabus = subjectSyllabusRepo.findById(syllabusId).orElse(null);
        if (syllabus == null) {
            log.error("SubjectSyllabus not found with ID: {}", syllabusId);
            return;
        }

        try {
            syllabus.setSyncStatus("PARSING");
            subjectSyllabusRepo.saveAndFlush(syllabus);

            String plainText = syllabus.getPlainText();
            String subjectCode = syllabus.getSubject().getSubjectCode();

            // 1. Slice plain text to identify sections (Rule-based parsing to save token cost)
            String generalText = extractSegment(plainText, "", "material(s)");
            String materialsText = extractSegment(plainText, "material(s)", "LO(s)");
            String loText = extractSegment(plainText, "LO(s)", "sessions");
            String sessionsText = extractSegment(plainText, "sessions", "Constructive question(s)");
            String assessmentsText = extractSegment(plainText, "assessment(s)", "");

            if (sessionsText.isEmpty()) {
                // Fallback search if headers are formatted slightly differently
                sessionsText = extractSegment(plainText, "Session\tTopic", "Constructive question");
            }

            log.info("Slicing complete. General: {} chars, Materials: {} chars, LO: {} chars, Sessions: {} chars, Assessments: {} chars",
                    generalText.length(), materialsText.length(), loText.length(), sessionsText.length(), assessmentsText.length());

            // 2. Call LLM structured parsing on each segment sequentially
            String generalSchema = "{\n  \"subjectCode\": \"mã môn học (e.g. " + subjectCode + ")\",\n  \"subjectName\": \"tên tiếng Anh môn học\",\n  \"credits\": 3,\n  \"passingScore\": 5,\n  \"description\": \"mô tả tóm tắt môn học\"\n}";
            String generalJson = callLlmToParseSegment(generalText, generalSchema);

            String materialsSchema = "[\n  {\n    \"name\": \"tên tài liệu\",\n    \"author\": \"tác giả\",\n    \"publisher\": \"nhà xuất bản\",\n    \"publishedDate\": \"năm xuất bản\",\n    \"edition\": \"phiên bản\",\n    \"isbn\": \"mã ISBN\",\n    \"isMain\": true/false\n  }\n]";
            String materialsJson = callLlmToParseSegment(materialsText, materialsSchema);

            String loSchema = "[\n  {\n    \"code\": \"mã LO (e.g. CLO1)\",\n    \"detail\": \"chi tiết chuẩn đầu ra\"\n  }\n]";
            String loJson = callLlmToParseSegment(loText, loSchema);

            String assessmentsSchema = "[\n  {\n    \"type\": \"loại đầu điểm (e.g. Lab, Progress Test, Presentation, Final Exam)\",\n    \"weight\": \"trọng số (e.g. 20%)\",\n    \"completionCriteria\": \"tiêu chí hoàn thành (e.g. 4.0)\"\n  }\n]";
            String assessmentsJson = callLlmToParseSegment(assessmentsText, assessmentsSchema);

            String sessionsSchema = "[\n  {\n    \"no\": 1,\n    \"topic\": \"tên chủ đề buổi học\",\n    \"tasks\": \"nhiệm vụ sinh viên\",\n    \"lo\": \"mã LO liên kết (e.g. CLO1)\"\n  }\n]";
            String sessionsJson = callLlmToParseSegment(sessionsText, sessionsSchema);

            // 3. Assemble JSON parts
            ObjectNode parentNode = objectMapper.createObjectNode();
            parentNode.set("general", parseJsonNode(generalJson));
            parentNode.set("materials", parseJsonNode(materialsJson));
            parentNode.set("learningOutcomes", parseJsonNode(loJson));
            parentNode.set("assessments", parseJsonNode(assessmentsJson));
            parentNode.set("sessions", parseJsonNode(sessionsJson));

            String finalJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parentNode);
            
            // 4. Update Database state to PARSED
            syllabus.setJsonContent(finalJson);
            syllabus.setSyncStatus("PARSED");
            subjectSyllabusRepo.saveAndFlush(syllabus);

            log.info("Syllabus parsed successfully into JSON. Triggering Vector Store indexing...");
            
            // 5. Index to Qdrant
            syncToVectorStore(syllabus.getId());

        } catch (Exception e) {
            log.error("Failed to parse syllabus ID: {}", syllabusId, e);
            syllabus.setSyncStatus("FAILED");
            subjectSyllabusRepo.saveAndFlush(syllabus);
        }
    }

    @Override
    @Transactional
    public void syncToVectorStore(Long syllabusId) {
        log.info("Starting Vector Store sync for syllabus ID: {}", syllabusId);
        SubjectSyllabus syllabus = subjectSyllabusRepo.findById(syllabusId).orElse(null);
        if (syllabus == null) {
            log.error("SubjectSyllabus not found with ID: {}", syllabusId);
            return;
        }

        try {
            syllabus.setSyncStatus("INDEXING");
            subjectSyllabusRepo.saveAndFlush(syllabus);

            String subjectCode = syllabus.getSubject().getSubjectCode();
            JsonNode rootNode = objectMapper.readTree(syllabus.getJsonContent());

            List<Document> virtualDocs = new ArrayList<>();

            // 1. General Document
            JsonNode generalNode = rootNode.get("general");
            if (generalNode != null && !generalNode.isNull()) {
                String content = String.format("=== THÔNG TIN CHUNG MÔN HỌC %s ===\n" +
                                "Tên môn học: %s\n" +
                                "Số tín chỉ: %s\n" +
                                "Điểm qua môn tối thiểu: %s\n" +
                                "Mô tả môn học: %s\n",
                        subjectCode,
                        generalNode.path("subjectName").asText("N/A"),
                        generalNode.path("credits").asText("3"),
                        generalNode.path("passingScore").asText("5.0"),
                        generalNode.path("description").asText("N/A"));
                virtualDocs.add(createVirtualDocument(subjectCode, "general", content));
            }

            // 2. Materials Document
            JsonNode materialsNode = rootNode.get("materials");
            if (materialsNode != null && materialsNode.isArray()) {
                StringBuilder sb = new StringBuilder("=== TÀI LIỆU HỌC TẬP MÔN HỌC " + subjectCode + " ===\n");
                for (JsonNode mat : materialsNode) {
                    String isMainStr = mat.path("isMain").asBoolean(false) ? "[Tài liệu chính]" : "[Tài liệu phụ]";
                    sb.append(String.format("- %s %s. Tác giả: %s. Nhà xuất bản: %s (%s). ISBN: %s\n",
                            isMainStr,
                            mat.path("name").asText(""),
                            mat.path("author").asText(""),
                            mat.path("publisher").asText(""),
                            mat.path("publishedDate").asText(""),
                            mat.path("isbn").asText("")));
                }
                virtualDocs.add(createVirtualDocument(subjectCode, "materials", sb.toString()));
            }

            // 3. Learning Outcomes (CLO)
            JsonNode loNode = rootNode.get("learningOutcomes");
            if (loNode != null && loNode.isArray()) {
                StringBuilder sb = new StringBuilder("=== CHUẨN ĐẦU RA MÔN HỌC (LO) " + subjectCode + " ===\n");
                for (JsonNode lo : loNode) {
                    sb.append(String.format("- %s: %s\n",
                            lo.path("code").asText(""),
                            lo.path("detail").asText("")));
                }
                virtualDocs.add(createVirtualDocument(subjectCode, "clo", sb.toString()));
            }

            // 4. Assessments
            JsonNode assessNode = rootNode.get("assessments");
            if (assessNode != null && assessNode.isArray()) {
                StringBuilder sb = new StringBuilder("=== CẤU TRÚC ĐIỂM ĐÁNH GIÁ MÔN HỌC " + subjectCode + " ===\n");
                for (JsonNode as : assessNode) {
                    sb.append(String.format("- %s: Trọng số: %s. Tiêu chí hoàn thành tối thiểu: %s\n",
                            as.path("type").asText(""),
                            as.path("weight").asText(""),
                            as.path("completionCriteria").asText("N/A")));
                }
                virtualDocs.add(createVirtualDocument(subjectCode, "assessments", sb.toString()));
            }

            // 5. Sessions (Divided into 15-sessions groups to keep tokens small)
            JsonNode sessionsNode = rootNode.get("sessions");
            if (sessionsNode != null && sessionsNode.isArray()) {
                List<JsonNode> sessions = new ArrayList<>();
                sessionsNode.forEach(sessions::add);

                int size = sessions.size();
                for (int i = 0; i < size; i += 15) {
                    int end = Math.min(i + 15, size);
                    String sectionName = "sessions_" + (i + 1) + "_" + end;
                    StringBuilder sb = new StringBuilder("=== TIẾN TRÌNH CHI TIẾT CÁC PHIÊN HỌC (SESSIONS " + (i + 1) + "-" + end + ") MÔN HỌC " + subjectCode + " ===\n");
                    for (int j = i; j < end; j++) {
                        JsonNode s = sessions.get(j);
                        sb.append(String.format("Session %d: Chủ đề: %s | Nhiệm vụ: %s | Chuẩn đầu ra liên kết: %s\n",
                                s.path("no").asInt(j + 1),
                                s.path("topic").asText("N/A"),
                                s.path("tasks").asText("N/A"),
                                s.path("lo").asText("N/A")));
                    }
                    virtualDocs.add(createVirtualDocument(subjectCode, sectionName, sb.toString()));
                }
            }

            // 6. Delete old vectors of this subject from Qdrant
            List<String> vectorIds = List.of(
                    generateStableVectorId(subjectCode, "general"),
                    generateStableVectorId(subjectCode, "materials"),
                    generateStableVectorId(subjectCode, "clo"),
                    generateStableVectorId(subjectCode, "assessments"),
                    generateStableVectorId(subjectCode, "sessions_1_15"),
                    generateStableVectorId(subjectCode, "sessions_16_30"),
                    generateStableVectorId(subjectCode, "sessions_31_45"),
                    generateStableVectorId(subjectCode, "sessions_46_60")
            );

            try {
                log.info("Deleting old Qdrant vectors for subject: {}", subjectCode);
                vectorStore.delete(vectorIds);
            } catch (Exception e) {
                log.warn("Failed to delete some vector IDs (could be because they don't exist yet): {}", e.getMessage());
            }

            // 7. Push new documents to Qdrant
            log.info("Uploading {} virtual syllabus documents to Qdrant...", virtualDocs.size());
            vectorStore.add(virtualDocs);

            // 8. Update DB state
            syllabus.setSyncStatus("SUCCESS");
            syllabus.setEmbeddingVersion(syllabus.getEmbeddingVersion() + 1);
            subjectSyllabusRepo.saveAndFlush(syllabus);
            log.info("Successfully updated Qdrant Vector Store for syllabus: {}", subjectCode);

        } catch (Exception e) {
            log.error("Failed to sync Qdrant for syllabus ID: {}", syllabusId, e);
            syllabus.setSyncStatus("FAILED");
            subjectSyllabusRepo.saveAndFlush(syllabus);
        }
    }

    @Override
    @Transactional
    public SubjectSyllabus updateSyllabus(Long subjectId, String jsonContent, String adminUsername, String reason) {
        log.info("Updating syllabus JSON for subject ID: {} by admin: {}", subjectId, adminUsername);

        // 1. Verify JSON format
        try {
            objectMapper.readTree(jsonContent);
        } catch (Exception e) {
            throw new GlobalException(400, "Invalid JSON content format");
        }

        // 2. Fetch current syllabus
        SubjectSyllabus syllabus = subjectSyllabusRepo.findBySubjectSubjectId(subjectId)
                .orElseThrow(() -> new GlobalException(404, "Syllabus not found for subject ID: " + subjectId));

        // 3. Save snapshot to history
        int latestVersion = subjectSyllabusHistoryRepo.findBySubjectSyllabusIdOrderByVersionDesc(syllabus.getId())
                .stream()
                .map(SubjectSyllabusHistory::getVersion)
                .findFirst()
                .orElse(0);

        SubjectSyllabusHistory history = SubjectSyllabusHistory.builder()
                .subjectSyllabus(syllabus)
                .pdfUrl(syllabus.getPdfUrl())
                .plainText(syllabus.getPlainText())
                .jsonContent(syllabus.getJsonContent())
                .version(latestVersion + 1)
                .updatedBy(adminUsername)
                .updatedReason(reason)
                .build();
        subjectSyllabusHistoryRepo.save(history);

        // 4. Update current syllabus state
        syllabus.setJsonContent(jsonContent);
        syllabus.setSyncStatus("INDEXING");
        syllabus = subjectSyllabusRepo.saveAndFlush(syllabus);

        // 5. Trigger async Qdrant re-sync
        final Long syllabusIdToSync = syllabus.getId();
        eventPublisher.publishEvent(new SyllabusSyncRequestEvent(syllabusIdToSync));

        return syllabus;
    }

    @Async
    @EventListener
    @Transactional
    public void handleSyllabusSyncEvent(SyllabusSyncRequestEvent event) {
        syncToVectorStore(event.getSyllabusId());
    }

    @Override
    public List<SubjectSyllabusHistory> getHistory(Long subjectId) {
        SubjectSyllabus syllabus = subjectSyllabusRepo.findBySubjectSubjectId(subjectId)
                .orElseThrow(() -> new GlobalException(404, "Syllabus not found for subject ID: " + subjectId));
        return subjectSyllabusHistoryRepo.findBySubjectSyllabusIdOrderByVersionDesc(syllabus.getId());
    }

    @Override
    @Transactional
    public SubjectSyllabus rollback(Long subjectId, Long historyId, String adminUsername) {
        log.info("Rolling back syllabus for subject ID: {} to history ID: {} by admin: {}", subjectId, historyId, adminUsername);

        SubjectSyllabus syllabus = subjectSyllabusRepo.findBySubjectSubjectId(subjectId)
                .orElseThrow(() -> new GlobalException(404, "Syllabus not found for subject ID: " + subjectId));

        SubjectSyllabusHistory history = subjectSyllabusHistoryRepo.findById(historyId)
                .orElseThrow(() -> new GlobalException(404, "Syllabus history not found with ID: " + historyId));

        if (!history.getSubjectSyllabus().getId().equals(syllabus.getId())) {
            throw new GlobalException(400, "Syllabus history record does not match the subject's syllabus");
        }

        // Save current state as history snapshot before rolling back
        int latestVersion = subjectSyllabusHistoryRepo.findBySubjectSyllabusIdOrderByVersionDesc(syllabus.getId())
                .stream()
                .map(SubjectSyllabusHistory::getVersion)
                .findFirst()
                .orElse(0);

        SubjectSyllabusHistory currentSnapshot = SubjectSyllabusHistory.builder()
                .subjectSyllabus(syllabus)
                .pdfUrl(syllabus.getPdfUrl())
                .plainText(syllabus.getPlainText())
                .jsonContent(syllabus.getJsonContent())
                .version(latestVersion + 1)
                .updatedBy(adminUsername)
                .updatedReason("Rollback to version " + history.getVersion())
                .build();
        subjectSyllabusHistoryRepo.save(currentSnapshot);

        // Restore state
        syllabus.setPdfUrl(history.getPdfUrl());
        syllabus.setPlainText(history.getPlainText());
        syllabus.setJsonContent(history.getJsonContent());
        syllabus.setSyncStatus("INDEXING");
        syllabus = subjectSyllabusRepo.saveAndFlush(syllabus);

        // Sync to Qdrant
        final Long syllabusIdToSync = syllabus.getId();
        eventPublisher.publishEvent(new SyllabusSyncRequestEvent(syllabusIdToSync));

        return syllabus;
    }

    @Override
    @Transactional
    public void deleteSyllabus(Long subjectId) {
        log.info("Deleting syllabus for subject ID: {}", subjectId);
        SubjectSyllabus syllabus = subjectSyllabusRepo.findBySubjectSubjectId(subjectId)
                .orElseThrow(() -> new GlobalException(404, "Syllabus not found for subject ID: " + subjectId));

        String subjectCode = syllabus.getSubject().getSubjectCode();

        // 1. Build list of vector IDs to delete from Qdrant
        List<String> vectorIds = new ArrayList<>();
        vectorIds.add(generateStableVectorId(subjectCode, "general"));
        vectorIds.add(generateStableVectorId(subjectCode, "materials"));
        vectorIds.add(generateStableVectorId(subjectCode, "clo"));
        vectorIds.add(generateStableVectorId(subjectCode, "assessments"));

        try {
            JsonNode rootNode = objectMapper.readTree(syllabus.getJsonContent());
            JsonNode sessionsNode = rootNode.get("sessions");
            if (sessionsNode != null && sessionsNode.isArray()) {
                int size = sessionsNode.size();
                for (int i = 0; i < size; i += 15) {
                    int end = Math.min(i + 15, size);
                    vectorIds.add(generateStableVectorId(subjectCode, "sessions_" + (i + 1) + "_" + end));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse json content for vector cleanup: {}", e.getMessage());
        }

        // 2. Delete from Qdrant
        try {
            log.info("Deleting Qdrant vectors for subject {}: {}", subjectCode, vectorIds);
            vectorStore.delete(vectorIds);
        } catch (Exception e) {
            log.error("Failed to delete vectors from Qdrant for subject: {}", subjectCode, e);
        }

        // 3. Delete history records
        subjectSyllabusHistoryRepo.deleteBySubjectSyllabusId(syllabus.getId());

        // 4. Delete the syllabus record itself
        subjectSyllabusRepo.delete(syllabus);
        log.info("Syllabus and its history deleted successfully for subject ID: {}", subjectId);
    }

    // ==========================================
    //            HELPER METHODS
    // ==========================================

    private String extractSegment(String text, String startHeader, String endHeader) {
        if (text == null || text.isEmpty()) return "";

        int startIndex = 0;
        if (!startHeader.isEmpty()) {
            Pattern startPattern = Pattern.compile(Pattern.quote(startHeader), Pattern.CASE_INSENSITIVE);
            Matcher startMatcher = startPattern.matcher(text);
            if (startMatcher.find()) {
                startIndex = startMatcher.end();
            } else {
                return ""; // Header not found
            }
        }

        int endIndex = text.length();
        if (!endHeader.isEmpty()) {
            Pattern endPattern = Pattern.compile(Pattern.quote(endHeader), Pattern.CASE_INSENSITIVE);
            Matcher endMatcher = endPattern.matcher(text);
            if (endMatcher.find(startIndex)) {
                endIndex = endMatcher.start();
            }
        }

        return text.substring(startIndex, endIndex).trim();
    }

    private String callLlmToParseSegment(String segmentText, String expectedSchemaFormat) {
        if (segmentText.trim().isEmpty()) {
            return expectedSchemaFormat.startsWith("[") ? "[]" : "{}";
        }

        String promptText = """
                Bạn là một chuyên gia bóc tách dữ liệu giáo trình Đại học FPT.
                Hãy phân tích đoạn văn bản thô (Plain Text) dưới đây và trả về dữ liệu cấu trúc dưới dạng chuỗi JSON chính xác theo Schema yêu cầu.
                
                Đoạn văn bản thô:
                \"\"\"
                {segmentText}
                \"\"\"
                
                Định dạng JSON đầu ra mẫu:
                {expectedSchemaFormat}
                
                Yêu cầu bắt buộc:
                1. Chỉ trả về chuỗi JSON thô hợp lệ. Không bao gồm các ký tự định dạng markdown như ```json hoặc ```, không có lời dẫn hay giải thích.
                2. Phải trích xuất đầy đủ tất cả dữ liệu từ đoạn văn bản thô, không được tóm tắt hay tự ý lược bỏ thông tin.
                3. Đảm bảo cú pháp JSON hoàn hảo (dấu ngoặc kép, dấu phẩy phân tách).
                """;

        try {
            PromptTemplate template = new PromptTemplate(promptText);
            Map<String, Object> params = Map.of(
                    "segmentText", segmentText,
                    "expectedSchemaFormat", expectedSchemaFormat
            );
            Prompt prompt = template.create(params);
            String response = chatClient.prompt(prompt).call().content();

            if (response != null) {
                response = response.trim();
                if (response.startsWith("```json")) {
                    response = response.substring(7);
                }
                if (response.startsWith("```")) {
                    response = response.substring(3);
                }
                if (response.endsWith("```")) {
                    response = response.substring(0, response.length() - 3);
                }
                return response.trim();
            }
            return expectedSchemaFormat.startsWith("[") ? "[]" : "{}";
        } catch (Exception e) {
            log.error("LLM parsing error for segment: {}", segmentText.substring(0, Math.min(50, segmentText.length())), e);
            return expectedSchemaFormat.startsWith("[") ? "[]" : "{}";
        }
    }

    private JsonNode parseJsonNode(String jsonStr) {
        try {
            return objectMapper.readTree(jsonStr);
        } catch (Exception e) {
            log.error("Failed to parse JSON string: {}", jsonStr, e);
            return objectMapper.createObjectNode();
        }
    }

    private Document createVirtualDocument(String subjectCode, String section, String textContent) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentType", "SYSTEM_SYLLABUS");
        metadata.put("subjectCode", subjectCode);
        metadata.put("section", section);

        String vectorId = generateStableVectorId(subjectCode, section);
        return new Document(vectorId, textContent, metadata);
    }

    private String generateStableVectorId(String subjectCode, String section) {
        String key = subjectCode + "_syllabus_" + section;
        return UUID.nameUUIDFromBytes(key.getBytes()).toString();
    }

    // Spring Application Event Classes to decouple async actions
    @Getter
    @RequiredArgsConstructor
    public static class SyllabusProcessEvent {
        private final Long syllabusId;
    }

    @Getter
    @RequiredArgsConstructor
    public static class SyllabusSyncRequestEvent {
        private final Long syllabusId;
    }
}
