package com.example.agent.service;

import com.example.agent.dto.ChatRequest;
import com.example.agent.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private ConversationStore store;

    @InjectMocks
    private AgentService agentService;

    @BeforeEach
    void setup() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Test reply from agent");
    }

    @Test
    void chat_withNoSessionId_generatesANewSessionId() {
        List<Message> history = new ArrayList<>();
        when(store.getOrCreate(anyString())).thenReturn(history);

        ChatRequest request = ChatRequest.builder()
                .message("Hello, how do I get started?")
                .build();

        ChatResponse response = agentService.chat(request);

        assertNotNull(response.getSessionId());
        assertFalse(response.getSessionId().isBlank());
        assertEquals("Test reply from agent", response.getReply());
    }

    @Test
    void chat_withProvidedSessionId_usesExistingSession() {
        List<Message> history = new ArrayList<>();
        when(store.getOrCreate("my-session")).thenReturn(history);

        ChatRequest request = ChatRequest.builder()
                .sessionId("my-session")
                .message("What's next?")
                .build();

        ChatResponse response = agentService.chat(request);

        assertEquals("my-session", response.getSessionId());
    }

    @Test
    void chat_withBlankSessionId_generatesANewSessionId() {
        List<Message> history = new ArrayList<>();
        when(store.getOrCreate(anyString())).thenReturn(history);

        ChatRequest request = ChatRequest.builder()
                .sessionId("   ")
                .message("Hello")
                .build();

        ChatResponse response = agentService.chat(request);

        assertNotEquals("   ", response.getSessionId());
        assertFalse(response.getSessionId().isBlank());
    }

    @Test
    void chat_appendsUserMessageThenAssistantMessageToHistory() {
        List<Message> history = new ArrayList<>();
        when(store.getOrCreate(anyString())).thenReturn(history);

        ChatRequest request = ChatRequest.builder()
                .sessionId("test-session")
                .message("What should I do first?")
                .build();

        agentService.chat(request);

        assertEquals(2, history.size());
        assertInstanceOf(UserMessage.class, history.get(0));
        assertInstanceOf(AssistantMessage.class, history.get(1));
    }

    @Test
    void chat_userMessageInHistoryMatchesRequestMessage() {
        List<Message> history = new ArrayList<>();
        when(store.getOrCreate(anyString())).thenReturn(history);

        ChatRequest request = ChatRequest.builder()
                .sessionId("test-session")
                .message("Set up my dev environment")
                .build();

        agentService.chat(request);

        assertEquals("Set up my dev environment", history.get(0).getText());
    }

    @Test
    void chat_assistantMessageInHistoryMatchesLlmReply() {
        List<Message> history = new ArrayList<>();
        when(store.getOrCreate(anyString())).thenReturn(history);

        ChatRequest request = ChatRequest.builder()
                .sessionId("test-session")
                .message("Who is my buddy?")
                .build();

        agentService.chat(request);

        assertEquals("Test reply from agent", history.get(1).getText());
    }

    @Test
    void chat_multiTurn_historyGrowsAcrossTurns() {
        List<Message> history = new ArrayList<>();
        when(store.getOrCreate("multi-turn")).thenReturn(history);

        agentService.chat(ChatRequest.builder().sessionId("multi-turn").message("Turn 1").build());
        agentService.chat(ChatRequest.builder().sessionId("multi-turn").message("Turn 2").build());

        assertEquals(4, history.size()); // 2 user + 2 assistant
    }
}
