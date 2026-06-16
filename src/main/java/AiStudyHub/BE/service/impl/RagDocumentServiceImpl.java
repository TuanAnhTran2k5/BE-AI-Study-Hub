package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Response.UploadDocumentResponse;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.RagChunk;
import AiStudyHub.BE.entity.RagDocument;
import AiStudyHub.BE.exception.InvalidFileException;
import AiStudyHub.BE.exception.RagProcessingException;
import AiStudyHub.BE.exception.ResourceNotFoundException;
import AiStudyHub.BE.exception.VectorStoreException;
import AiStudyHub.BE.mapper.RagDocumentMapper;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.RagChunkRepository;
import AiStudyHub.BE.repository.RagDocumentRepository;
import AiStudyHub.BE.service.RagDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for {@link RagDocumentService}.
 * Handles file reading, parsing, splitting into chunks, embedding generation,
 * vector database persistence in Qdrant, and relational storage in MySQL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagDocumentServiceImpl implements RagDocumentService {

    private final RagDocumentRepository ragDocumentRepository;
    private final RagChunkRepository ragChunkRepository;
    private final DocumentRepo documentRepo;
    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;
    private final RagDocumentMapper ragDocumentMapper;

    @Override
    @Transactional
    public UploadDocumentResponse indexDocument(Long documentId) {
        log.info("Triggering manual indexing for document ID: {}", documentId);

        RagDocument ragDoc = ragDocumentRepository.findById(documentId)
                .orElse(null);

        if (ragDoc == null) {
            // Check if it exists in main document database
            Document mainDoc = documentRepo.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

            ragDoc = RagDocument.builder()
                    .documentId(mainDoc.getDocumentId())
                    .originalFileName(mainDoc.getFileName())
                    .contentType(mainDoc.getFileType())
                    .fileSize(mainDoc.getFileSize())
                    .uploadedBy(mainDoc.getOwner().getEmail())
                    .status("PENDING")
                    .build();
            ragDoc = ragDocumentRepository.save(ragDoc);
        }

        Long mainDocId = ragDoc.getDocumentId() != null ? ragDoc.getDocumentId() : documentId;
        Document mainDoc = documentRepo.findById(mainDocId)
                .orElseThrow(() -> new ResourceNotFoundException("Main document not found with ID: " + mainDocId));

        if (mainDoc.getFileUrl() == null || mainDoc.getFileUrl().isEmpty()) {
            throw new InvalidFileException("Main document file URL is empty");
        }

        try {
            log.info("Downloading document content from URL: {}", mainDoc.getFileUrl());
            byte[] fileBytes = URI.create(mainDoc.getFileUrl()).toURL().openStream().readAllBytes();
            
            // Clean up existing RAG resources before re-indexing
            cleanExistingRagResources(ragDoc);

            indexDocumentContent(ragDoc, fileBytes);
            ragDoc.setStatus("INDEXED");
            ragDoc = ragDocumentRepository.save(ragDoc);
        } catch (Exception e) {
            log.error("Failed to manually index document: {}", mainDoc.getFileName(), e);
            ragDoc.setStatus("FAILED");
            ragDocumentRepository.save(ragDoc);
            throw new RagProcessingException("Failed to index document contents", e);
        }

        return ragDocumentMapper.toUploadDocumentResponse(ragDoc);
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        log.info("Deleting RAG document with ID: {}", documentId);
        RagDocument document = ragDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("RagDocument not found with ID: " + documentId));

        cleanExistingRagResources(document);
        ragDocumentRepository.delete(document);
        log.info("Deleted RagDocument metadata");
    }

    @Override
    public UploadDocumentResponse getDocument(Long documentId) {
        RagDocument document = ragDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("RagDocument not found with ID: " + documentId));
        return ragDocumentMapper.toUploadDocumentResponse(document);
    }

    @Transactional
    public void indexDocumentContent(RagDocument document, byte[] fileBytes) {
        log.info("Extracting and indexing text for RagDocument ID: {}", document.getId());

        try {
            // 1. Read document using TikaDocumentReader
            Resource resource = new ByteArrayResource(fileBytes);
            TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
            List<org.springframework.ai.document.Document> documents = tikaReader.read();

            if (documents.isEmpty()) {
                log.warn("No text extracted from document ID: {}", document.getId());
                return;
            }

            // Combine all text segments
            String fullText = documents.stream()
                    .map(org.springframework.ai.document.Document::getText)
                    .collect(Collectors.joining("\n"));

            // 2. Split text into chunks
            org.springframework.ai.document.Document parentDoc = new org.springframework.ai.document.Document(fullText);
            List<org.springframework.ai.document.Document> chunks = textSplitter.apply(List.of(parentDoc));
            log.info("Split document ID: {} into {} chunks", document.getId(), chunks.size());

            // 3. Save chunks in MySQL and write to Qdrant
            List<RagChunk> ragChunks = new ArrayList<>();
            List<org.springframework.ai.document.Document> docsToVectorStore = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                org.springframework.ai.document.Document chunk = chunks.get(i);

                RagChunk ragChunk = RagChunk.builder()
                        .document(document)
                        .chunkIndex(i)
                        .content(chunk.getText())
                        .embeddingCreated(true)
                        .build();
                ragChunks.add(ragChunk);

                // Prepare metadata for vector store
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("documentId", document.getDocumentId() != null ? document.getDocumentId().toString() : document.getId().toString());
                metadata.put("originalFileName", document.getOriginalFileName());
                metadata.put("uploadedBy", document.getUploadedBy());
                metadata.put("chunkIndex", i);

                // Deterministic UUID for vector ID based on document ID and chunk index
                String vectorId = UUID.nameUUIDFromBytes((document.getId().toString() + "_" + i).getBytes()).toString();

                org.springframework.ai.document.Document vectorDoc = new org.springframework.ai.document.Document(vectorId, chunk.getText(), metadata);
                docsToVectorStore.add(vectorDoc);
            }

            // Batch save chunks to database
            ragChunkRepository.saveAll(ragChunks);

            // Write vectors to Qdrant
            log.info("Sending chunks to Qdrant vector store...");
            vectorStore.add(docsToVectorStore);
            log.info("Successfully indexed chunks in Qdrant.");

        } catch (Exception e) {
            log.error("Error during ETL index process for document ID {}", document.getId(), e);
            throw new RagProcessingException("Failed to parse and index document content", e);
        }
    }

    private void cleanExistingRagResources(RagDocument document) {
        List<RagChunk> chunks = ragChunkRepository.findByDocumentId(document.getId());
        if (!chunks.isEmpty()) {
            List<String> vectorIds = chunks.stream()
                    .map(chunk -> UUID.nameUUIDFromBytes((document.getId().toString() + "_" + chunk.getChunkIndex()).getBytes()).toString())
                    .toList();
            try {
                log.info("Deleting {} existing vectors from Qdrant for document ID: {}...", vectorIds.size(), document.getId());
                vectorStore.delete(vectorIds);
                log.info("Successfully deleted existing vectors from Qdrant");
            } catch (Exception e) {
                log.error("Failed to delete vectors from Qdrant for document ID: {}", document.getId(), e);
                throw new VectorStoreException("Failed to delete vectors from Qdrant", e);
            }

            ragChunkRepository.deleteByDocumentId(document.getId());
            log.info("Deleted existing chunks from database");
        }
    }
}