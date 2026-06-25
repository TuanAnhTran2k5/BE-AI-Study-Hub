package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.ChatRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.ChatResponse;
import AiStudyHub.BE.service.IRagSystem;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/rag/chat")
@CrossOrigin("*")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RagChatController {

    IRagSystem ragChatService;

    @PostMapping("/ask")
    public ResponseEntity<APIResponse<ChatResponse>> askQuestion(@RequestBody @Valid ChatRequest request) {
        log.info("API Request: Ask question '{}'", request.getQuestion());
        ChatResponse response = ragChatService.askQuestion(request);
        return ResponseEntity.ok(
                APIResponse.response(200, "Answer generated successfully", response)
        );
    }
}
