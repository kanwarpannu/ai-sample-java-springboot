package com.example.agent.config;

import com.example.agent.mcp.KnowledgeMcpTools;
import com.example.agent.mcp.OnboardingMcpTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AgentConfiguration {

    private static final String SYSTEM_PROMPT = """
            You are an intelligent developer onboarding agent. Your role is to help new engineers
            get up to speed quickly by providing guidance, creating structured onboarding plans,
            and answering questions about the engineering systems, tools, and processes.
            You have access to the following tools:
            - searchDocuments: search the knowledge base for engineering guides and documentation
            - createOnboardingPlan: generate a personalised week-by-week onboarding checklist
            - getOnboardingProgress: check how many steps a developer has completed
            - updateOnboardingStep: mark a checklist step as done or not done
            Always be helpful, concise, and actionable. Use tools proactively when relevant.
            """;

    private final KnowledgeMcpTools knowledgeMcpTools;
    private final OnboardingMcpTools onboardingMcpTools;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(knowledgeMcpTools, onboardingMcpTools)
                .build();
    }
}
