package com.example.agent.service;

import com.example.agent.dto.ChatRequest;
import com.example.agent.dto.ChatResponse;
import com.example.agent.dto.StreamResult;
import com.example.agent.session.SessionContext;
import com.example.agent.session.SessionContextHolder;
import com.example.agent.session.SessionContextStore;
import com.example.agent.statemachine.AgentProcessingState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final ChatClient chatClient;
    private final SessionContextStore store;

    public ChatResponse chat(ChatRequest request) {
        String sessionId = resolveSessionId(request.getSessionId());
        MDC.put("sessionId", sessionId);
        SessionContext ctx = store.getOrCreate(sessionId);
        SessionContextHolder.set(ctx);

        ctx.getAgentSM().transition(AgentProcessingState.PROCESSING);

        try {
            ctx.getHistory().add(new UserMessage(request.getMessage()));
            log.info("Received message for session");

            String reply = chatClient.prompt()
                    .messages(ctx.getHistory())
                    .call()
                    .content();

            ctx.getAgentSM().transition(AgentProcessingState.RESPONDING);
            ctx.getHistory().add(new AssistantMessage(reply));
            ctx.getAgentSM().transition(AgentProcessingState.DONE);
            log.info("Response generated | onboardingState={}", ctx.getOnboardingSM().current());

            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .reply(reply)
                    .build();
        } catch (RuntimeException e) {
            ctx.getAgentSM().transition(AgentProcessingState.ERROR);
            log.error("Error during chat: {}", e.getMessage(), e);
            throw e;
        } finally {
            ctx.getAgentSM().reset();
            SessionContextHolder.clear();
            MDC.clear();
        }
    }

    public StreamResult streamChat(ChatRequest request) {
        String sessionId = resolveSessionId(request.getSessionId());
        MDC.put("sessionId", sessionId);
        log.info("Streaming chat request received");

        SessionContext ctx = store.getOrCreate(sessionId);
        ctx.getHistory().add(new UserMessage(request.getMessage()));

        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        StringBuilder accumulator = new StringBuilder();
        Flux<String> tokens = chatClient.prompt()
                .messages(ctx.getHistory())
                .stream()
                .content()
                .doOnSubscribe(s -> {
                    // Restore MDC and session context on the subscription thread
                    // (where tool calls will execute inside the reactive pipeline)
                    if (mdcContext != null) MDC.setContextMap(mdcContext);
                    SessionContextHolder.set(ctx);
                    ctx.getAgentSM().transition(AgentProcessingState.PROCESSING);
                })
                .doOnNext(accumulator::append)
                .doOnComplete(() -> {
                    if (mdcContext != null) MDC.setContextMap(mdcContext);
                    ctx.getAgentSM().transition(AgentProcessingState.RESPONDING);
                    ctx.getHistory().add(new AssistantMessage(accumulator.toString()));
                    ctx.getAgentSM().transition(AgentProcessingState.DONE);
                    log.info("Streaming complete | onboardingState={}", ctx.getOnboardingSM().current());
                    ctx.getAgentSM().reset();
                    SessionContextHolder.clear();
                    MDC.clear();
                })
                .doOnError(e -> {
                    if (mdcContext != null) MDC.setContextMap(mdcContext);
                    ctx.getAgentSM().transition(AgentProcessingState.ERROR);
                    log.error("Streaming error: {}", e.getMessage(), e);
                    ctx.getAgentSM().reset();
                    SessionContextHolder.clear();
                    MDC.clear();
                });

        // Streaming executes on reactor threads; clear request-thread locals before returning
        MDC.clear();
        return new StreamResult(sessionId, tokens);
    }

    private String resolveSessionId(String sessionId) {
        return (sessionId != null && !sessionId.isBlank())
                ? sessionId
                : UUID.randomUUID().toString();
    }
}
