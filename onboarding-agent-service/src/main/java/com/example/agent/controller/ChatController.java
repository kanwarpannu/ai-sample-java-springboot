package com.example.agent.controller;

import com.example.agent.dto.ChatRequest;
import com.example.agent.dto.ChatResponse;
import com.example.agent.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Onboarding Agent", description = "AI-powered developer onboarding chat interface")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {

    private final AgentService agentService;

    @Operation(
            summary = "Send a message to the onboarding agent",
            description = "Submit a message and optional session ID. Omit sessionId to start a new conversation. "
                    + "Include the returned sessionId in subsequent requests to maintain context."
    )
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(agentService.chat(request));
    }
}
