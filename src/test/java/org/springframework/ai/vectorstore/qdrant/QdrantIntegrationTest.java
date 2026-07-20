package org.springframework.ai.vectorstore.qdrant;

import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.repository.RagChunkRepository;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.impl.RagSystemService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.AopTestUtils;

import java.util.List;
import java.util.Map;

@SpringBootTest(classes = AiStudyHub.BE.BeApplication.class)
public class QdrantIntegrationTest {

    @Autowired private RagSystemService ragSystemService;
    @Autowired private UserRepo userRepo;
    @Autowired private VectorStore vectorStore;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private RagChunkRepository ragChunkRepository;

    private void setAuth(String email) {
        User user = userRepo.findByEmail(email).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    public void reindexAllAndVerify() {
        // Re-index all documents with DB chunk fallback
        String[][] docOwners = {
                {"1", "aistudyhub062026@gmail.com"},
                {"3", "staboyvn12@gmail.com"},
                {"5", "aistudyhub062026@gmail.com"},
                {"6", "aistudyhub062026@gmail.com"},
                {"7", "aistudyhub062026@gmail.com"},
        };
        for (String[] pair : docOwners) {
            try {
                setAuth(pair[1]);
                RagSystemService target = AopTestUtils.getTargetObject(ragSystemService);
                target.indexDocument(Long.parseLong(pair[0]));
                System.out.println("✓ Doc " + pair[0] + " indexed");
            } catch (Exception e) {
                System.out.println("✗ Doc " + pair[0] + " failed: " + e.getMessage());
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        // Also try indexing any other docs in DB
        List<Map<String, Object>> allDocs = jdbcTemplate.queryForList(
                "select d.document_id, d.visibility_status, d.owner_id, u.email, " +
                "rd.id as rag_id, rd.status as rag_status, " +
                "(select count(*) from rag_chunk rc where rc.document_id = rd.id) as chunk_count " +
                "from document d " +
                "join user u on u.user_id = d.owner_id " +
                "left join rag_document rd on rd.document_id = d.document_id " +
                "where d.upload_status = 'COMPLETED'"
        );
        System.out.println("\n=== ALL DOCUMENTS ===");
        for (Map<String, Object> doc : allDocs) {
            System.out.println(doc);
            Long docId = ((Number) doc.get("document_id")).longValue();
            Long ragId = doc.get("rag_id") != null ? ((Number) doc.get("rag_id")).longValue() : null;
            // Re-index docs not already covered above
            if (ragId != null && (docId > 7)) {
                String email = (String) doc.get("email");
                try {
                    setAuth(email);
                    RagSystemService target = AopTestUtils.getTargetObject(ragSystemService);
                    target.indexDocument(docId);
                    System.out.println("  ✓ Doc " + docId + " indexed");
                } catch (Exception e) {
                    System.out.println("  ✗ Doc " + docId + " failed: " + e.getMessage());
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }
        }

        // Verify Qdrant has vectors by querying without filter
        System.out.println("\n=== QDRANT TOTAL VECTORS ===");
        List<Document> all = vectorStore.similaritySearch(
                SearchRequest.builder().query("document summary").topK(50).similarityThreshold(0.0).build()
        );
        System.out.println("Total vectors in Qdrant: " + all.size());

        // Show chunk content for each unique docId
        System.out.println("\n=== CHUNK PREVIEW BY DOCUMENT ID ===");
        all.stream()
                .collect(java.util.stream.Collectors.groupingBy(d -> d.getMetadata().get("documentId")))
                .forEach((docId, chunks) -> {
                    System.out.println("DocumentId=" + docId + " | " + chunks.size() + " chunk(s)");
                    chunks.forEach(c -> System.out.println("  [" + c.getText().length() + " chars] " +
                            c.getText().substring(0, Math.min(150, c.getText().length()))));
                });
    }
}
