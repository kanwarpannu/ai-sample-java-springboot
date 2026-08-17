package com.example.agent.service;

import com.example.agent.dto.ChatRequest;
import com.example.agent.dto.ChatResponse;
import com.example.agent.dto.StreamResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final ChatClient chatClient;
    private final ConversationStore store;

    public ChatResponse chat(ChatRequest request) {
        String sessionId = resolveSessionId(request.getSessionId());
        List<Message> history = store.getOrCreate(sessionId);

        history.add(new UserMessage(request.getMessage()));

        String reply = chatClient.prompt()
                .messages(history)
                .call()
                .content();

        history.add(new AssistantMessage(reply));

        return ChatResponse.builder()
                .sessionId(sessionId)
                .reply(reply)
                .build();
    }

    public StreamResult streamChat(ChatRequest request) {
        String sessionId = resolveSessionId(request.getSessionId());
        List<Message> history = store.getOrCreate(sessionId);
        history.add(new UserMessage(request.getMessage()));

        StringBuilder accumulator = new StringBuilder();
        Flux<String> tokens = chatClient.prompt()
                .messages(history)
                .stream()
                .content()
                .doOnNext(accumulator::append)
                .doOnComplete(() -> history.add(new AssistantMessage(accumulator.toString())));

        return new StreamResult(sessionId, tokens);
    }

    private String resolveSessionId(String sessionId) {
        return (sessionId != null && !sessionId.isBlank())
                ? sessionId
                : UUID.randomUUID().toString();
    }
}
