package AiStudyHub.BE;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.RagDocument;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.RagDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class BeApplicationTests {

	@Autowired
	private DocumentRepo documentRepo;

	@Autowired
	private RagDocumentRepository ragDocumentRepository;

	@Test
	void printDatabaseState() {
		System.out.println("=== DIAGNOSTIC DATABASE STATE ===");
		List<Document> docs = documentRepo.findAll();
		System.out.println("Total Documents: " + docs.size());
		for (Document doc : docs) {
			System.out.printf("Doc ID: %d | Title: %s | FileName: %s | FileUrl: %s | Visibility: %s%n",
					doc.getDocumentId(), doc.getTitle(), doc.getFileName(), doc.getFileUrl(), doc.getVisibilityStatus());
		}

		List<RagDocument> ragDocs = ragDocumentRepository.findAll();
		System.out.println("Total RAG Documents: " + ragDocs.size());
		for (RagDocument rd : ragDocs) {
			System.out.printf("RAG ID: %d | Doc ID: %d | Status: %s | OriginalName: %s%n",
					rd.getId(), rd.getDocument().getDocumentId(), rd.getStatus(), rd.getOriginalFileName());
		}
		System.out.println("=================================");
	}

}
