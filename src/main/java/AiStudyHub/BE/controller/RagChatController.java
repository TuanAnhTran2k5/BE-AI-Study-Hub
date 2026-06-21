package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.ChatRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.ChatResponse;
import AiStudyHub.BE.service.impl.IRagChat;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/rag/chat")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RagChatController {

    IRagChat ragChatService;

    @PostMapping("/ask")
    public ResponseEntity<APIResponse<ChatResponse>> askQuestion(@RequestBody @Valid ChatRequest request) {
        log.info("API Request: Ask question '{}'", request.getQuestion());
        ChatResponse response = ragChatService.askQuestion(request);
        return ResponseEntity.ok(
                APIResponse.response(200, "Answer generated successfully", response)
        );
    }
}
