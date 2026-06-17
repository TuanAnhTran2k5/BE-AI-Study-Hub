package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.ChatRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.ChatResponse;
import AiStudyHub.BE.service.RagChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for executing RAG chat queries.
 * Users submit natural language questions and receive AI-generated answers
 * sourced from context extracted from uploaded documents.
 */
@RestController
@RequestMapping("/api/user/rag/chat")
@RequiredArgsConstructor
@Slf4j
public class RagChatController {

    private final RagChatService ragChatService;

    /**
     * Answers a question based on uploaded documents context.
     *
     * @param request the ChatRequest containing the question
     * @return the generated answer and lists of document sources
     */
    @PostMapping("/ask")
    public ResponseEntity<APIResponse<ChatResponse>> askQuestion(@RequestBody @Valid ChatRequest request) {
        log.info("API Request: Ask question '{}'", request.getQuestion());
        ChatResponse response = ragChatService.askQuestion(request);
        return ResponseEntity.ok(
                APIResponse.response(200, "Answer generated successfully", response)
        );
    }
}
