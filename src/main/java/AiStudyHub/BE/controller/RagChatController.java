package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.ChatRequest;
import AiStudyHub.BE.dto.Request.CreateSessionRequest;
import AiStudyHub.BE.dto.Request.SuggestPromptsRequest;
import AiStudyHub.BE.dto.Request.UpdateSessionTitleRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.ChatResponse;
import AiStudyHub.BE.dto.Response.ChatSessionResponse;
import AiStudyHub.BE.dto.Response.ChatMessageResponse;
import AiStudyHub.BE.service.IRagSystem;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/rag/chat")
@CrossOrigin("*")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RagChatController {

    IRagSystem ragChatService;

    @PostMapping("/sessions")
    public ResponseEntity<APIResponse<ChatSessionResponse>> createSession(@RequestBody @Valid CreateSessionRequest request) {
        log.info("API Request: Create chat session");
        ChatSessionResponse response = ragChatService.createSession(request);
        return ResponseEntity.ok(
                APIResponse.response(200, "Session created successfully", response)
        );
    }

    @GetMapping("/sessions")
    public ResponseEntity<APIResponse<List<ChatSessionResponse>>> getSessions() {
        log.info("API Request: Get chat sessions");
        List<ChatSessionResponse> response = ragChatService.getSessions();
        return ResponseEntity.ok(
                APIResponse.response(200, "Sessions retrieved successfully", response)
        );
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<APIResponse<Page<ChatMessageResponse>>> getSessionMessages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("API Request: Get messages for session {}", sessionId);
        Page<ChatMessageResponse> response = ragChatService.getSessionMessages(sessionId, page, size);
        return ResponseEntity.ok(
                APIResponse.response(200, "Messages retrieved successfully", response)
        );
    }

    @PostMapping("/sessions/{sessionId}/ask")
    public ResponseEntity<APIResponse<ChatResponse>> askQuestionInSession(
            @PathVariable Long sessionId,
            @RequestBody @Valid ChatRequest request) {
        log.info("API Request: Ask question in session {}", sessionId);
        ChatResponse response = ragChatService.askQuestionInSession(sessionId, request);
        return ResponseEntity.ok(
                APIResponse.response(200, "Answer generated successfully", response)
        );
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<APIResponse<ChatResponse>> deleteSession(@PathVariable Long sessionId) {
        log.info("API Request: Delete session {}", sessionId);
        ChatResponse response = ragChatService.deleteSession(sessionId);
        return ResponseEntity.ok(
                APIResponse.response(200, "Session deleted successfully", response)
        );
    }

    @PutMapping("/sessions/{sessionId}/documents")
    public ResponseEntity<APIResponse<ChatSessionResponse>> updateSessionDocuments(
            @PathVariable Long sessionId,
            @RequestBody @Valid CreateSessionRequest request) {
        log.info("API Request: Update documents for session {}", sessionId);
        ChatSessionResponse response = ragChatService.updateSessionDocuments(sessionId, request);
        return ResponseEntity.ok(
                APIResponse.response(200, "Session documents updated successfully", response)
        );
    }

    @PutMapping("/sessions/{sessionId}/title")
    public ResponseEntity<APIResponse<ChatSessionResponse>> updateSessionTitle(
            @PathVariable Long sessionId,
            @RequestBody @Valid UpdateSessionTitleRequest request) {
        log.info("API Request: Update title for session {}", sessionId);
        ChatSessionResponse response = ragChatService.updateSessionTitle(sessionId, request.getSessionTitle());
        return ResponseEntity.ok(
                APIResponse.response(200, "Session title updated successfully", response)
        );
    }

    @PostMapping("/suggest-prompts")
    public ResponseEntity<APIResponse<List<String>>> suggestPrompts(@RequestBody @Valid SuggestPromptsRequest request) {
        log.info("API Request: Suggest prompts for document IDs: {}", request.getDocumentIds());
        List<String> response = ragChatService.suggestPrompts(request);
        return ResponseEntity.ok(
                APIResponse.response(200, "Suggested prompts generated successfully", response)
        );
    }
}

