package com.example.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat response payload")
public class ChatResponse {

    @Schema(description = "Session ID — use this in subsequent requests to continue the conversation")
    private String sessionId;

    @Schema(description = "Agent's reply to the user message")
    private String reply;
}
