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
@Schema(description = "Chat request payload")
public class ChatRequest {

    @Schema(description = "Session ID for conversation continuity. Omit to start a new session.")
    private String sessionId;

    @Schema(description = "User message to send to the onboarding agent", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
}
