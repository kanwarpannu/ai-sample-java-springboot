package com.example.agent.config;

import com.example.agent.mcp.KnowledgeMcpTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
@RequiredArgsConstructor
public class AgentConfiguration {

    private static final String SYSTEM_PROMPT = """
            You are an intelligent developer onboarding agent. Your role is to help new engineers
            get up to speed quickly by providing guidance, creating structured onboarding plans,
            and tracking their progress through a persistent onboarding workflow.
            You have access to the following tools:
            - searchDocuments(keyword): search the engineering knowledge base for guides and docs
            - createOnboardingPlan(developerName, role): creates a personalised onboarding plan
              backed by PostgreSQL and returns a Plan ID (UUID) — store it for follow-up calls;
              supported roles: BACKEND_ENGINEER, FRONTEND_ENGINEER, PRODUCT_MANAGER
            - getOnboardingProgress(planId): returns completed/total step counts, remaining step
              titles, and any open blockers for the given Plan ID
            - updateOnboardingStep(planId, stepNumber, completed): marks a step done or reopens
              it; stepNumber is a 1-based integer (1–10); completed=true marks done, false reopens
            - reportBlocker(planId, stepNumber, description): reports a blocker on a specific
              step; returns a Blocker ID — store it to resolve later
            - resolveBlocker(blockerId): marks a blocker as resolved using the Blocker ID
            Always be helpful, concise, and actionable. Use tools proactively when relevant.
            When creating a plan, immediately share the Plan ID with the user.
            """;

    private final KnowledgeMcpTools knowledgeMcpTools;
    private final SyncMcpToolCallbackProvider mcpToolCallbackProvider;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        ToolCallback[] localCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(knowledgeMcpTools)
                .build()
                .getToolCallbacks();

        ToolCallback[] mcpCallbacks = mcpToolCallbackProvider.getToolCallbacks();

        ToolCallback[] allCallbacks = Stream
                .concat(Arrays.stream(localCallbacks), Arrays.stream(mcpCallbacks))
                .toArray(ToolCallback[]::new);

        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools((Object[]) allCallbacks)
                .build();
    }
}
