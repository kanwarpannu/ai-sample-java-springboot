package com.example.agent.controller;

import com.example.agent.dto.ChatRequest;
import com.example.agent.dto.ChatResponse;
import com.example.agent.dto.StreamResult;
import com.example.agent.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

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

    @Operation(
            summary = "Stream a chat reply via SSE",
            description = "Streams tokens as Server-Sent Events. First event (name=session) carries the sessionId; "
                    + "subsequent events (name=token) carry individual tokens. Include the sessionId in follow-up "
                    + "requests to maintain conversation context."
    )
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);
        StreamResult result = agentService.streamChat(request);

        try {
            emitter.send(SseEmitter.event().name("session").data(result.sessionId()));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        result.tokenFlux().subscribe(
                token -> {
                    try {
                        emitter.send(SseEmitter.event().name("token").data(token));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::completeWithError,
                emitter::complete
        );

        return emitter;
    }
}
