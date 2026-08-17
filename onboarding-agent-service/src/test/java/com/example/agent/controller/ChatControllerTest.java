package com.example.agent.controller;

import com.example.agent.dto.ChatRequest;
import com.example.agent.dto.ChatResponse;
import com.example.agent.service.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentService agentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void chat_newSession_returns200WithSessionIdAndReply() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("How do I set up the order service?")
                .build();

        ChatResponse response = ChatResponse.builder()
                .sessionId("abc-123")
                .reply("Here is your onboarding guide...")
                .build();

        when(agentService.chat(any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sessionId").value("abc-123"))
                .andExpect(jsonPath("$.reply").value("Here is your onboarding guide..."));
    }

    @Test
    void chat_withExistingSessionId_returnsTheSameSessionId() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .sessionId("existing-session")
                .message("What's my next step?")
                .build();

        ChatResponse response = ChatResponse.builder()
                .sessionId("existing-session")
                .reply("Your next step is to read the architecture overview.")
                .build();

        when(agentService.chat(any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("existing-session"));
    }

    @Test
    void chat_agentReturnsReply_replyPresentInResponse() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("Who should I contact for infra issues?")
                .build();

        ChatResponse response = ChatResponse.builder()
                .sessionId("new-session")
                .reply("Contact platform-team@acme.com for infrastructure questions.")
                .build();

        when(agentService.chat(any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Contact platform-team@acme.com for infrastructure questions."));
    }
}
