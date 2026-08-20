package com.example.agent.service;

import com.example.agent.dto.ChatRequest;
import com.example.agent.dto.ChatResponse;
import com.example.agent.session.SessionContext;
import com.example.agent.session.SessionContextStore;
import com.example.agent.statemachine.AgentProcessingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private SessionContextStore store;

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
        when(store.getOrCreate(anyString())).thenAnswer(inv -> new SessionContext(inv.getArgument(0)));

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
        when(store.getOrCreate("my-session")).thenReturn(new SessionContext("my-session"));

        ChatRequest request = ChatRequest.builder()
                .sessionId("my-session")
                .message("What's next?")
                .build();

        ChatResponse response = agentService.chat(request);

        assertEquals("my-session", response.getSessionId());
    }

    @Test
    void chat_withBlankSessionId_generatesANewSessionId() {
        when(store.getOrCreate(anyString())).thenAnswer(inv -> new SessionContext(inv.getArgument(0)));

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
        SessionContext ctx = new SessionContext("test-session");
        when(store.getOrCreate("test-session")).thenReturn(ctx);

        ChatRequest request = ChatRequest.builder()
                .sessionId("test-session")
                .message("What should I do first?")
                .build();

        agentService.chat(request);

        List<Message> history = ctx.getHistory();
        assertEquals(2, history.size());
        assertInstanceOf(UserMessage.class, history.get(0));
        assertInstanceOf(AssistantMessage.class, history.get(1));
    }

    @Test
    void chat_userMessageInHistoryMatchesRequestMessage() {
        SessionContext ctx = new SessionContext("test-session");
        when(store.getOrCreate("test-session")).thenReturn(ctx);

        ChatRequest request = ChatRequest.builder()
                .sessionId("test-session")
                .message("Set up my dev environment")
                .build();

        agentService.chat(request);

        assertEquals("Set up my dev environment", ctx.getHistory().get(0).getText());
    }

    @Test
    void chat_assistantMessageInHistoryMatchesLlmReply() {
        SessionContext ctx = new SessionContext("test-session");
        when(store.getOrCreate("test-session")).thenReturn(ctx);

        ChatRequest request = ChatRequest.builder()
                .sessionId("test-session")
                .message("Who is my buddy?")
                .build();

        agentService.chat(request);

        assertEquals("Test reply from agent", ctx.getHistory().get(1).getText());
    }

    @Test
    void chat_multiTurn_historyGrowsAcrossTurns() {
        SessionContext ctx = new SessionContext("multi-turn");
        when(store.getOrCreate("multi-turn")).thenReturn(ctx);

        agentService.chat(ChatRequest.builder().sessionId("multi-turn").message("Turn 1").build());
        agentService.chat(ChatRequest.builder().sessionId("multi-turn").message("Turn 2").build());

        assertEquals(4, ctx.getHistory().size()); // 2 user + 2 assistant
    }

    @Test
    void chat_successfulRequest_agentStateResetsToIdle() {
        SessionContext ctx = new SessionContext("state-check");
        when(store.getOrCreate("state-check")).thenReturn(ctx);

        agentService.chat(ChatRequest.builder().sessionId("state-check").message("Hello").build());

        assertEquals(AgentProcessingState.IDLE, ctx.getAgentSM().current());
    }

    @Test
    void chat_llmThrowsException_agentStateResetsToIdle() {
        SessionContext ctx = new SessionContext("error-session");
        when(store.getOrCreate("error-session")).thenReturn(ctx);
        when(chatClient.prompt()).thenThrow(new RuntimeException("LLM unavailable"));

        assertThrows(RuntimeException.class, () ->
                agentService.chat(ChatRequest.builder().sessionId("error-session").message("Hello").build())
        );

        assertEquals(AgentProcessingState.IDLE, ctx.getAgentSM().current());
    }
}
