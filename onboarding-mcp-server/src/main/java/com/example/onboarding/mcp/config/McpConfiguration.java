package com.example.onboarding.mcp.config;

import com.example.onboarding.mcp.tool.OnboardingMcpTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class McpConfiguration {

    private final OnboardingMcpTools onboardingMcpTools;

    @Bean
    public ToolCallbackProvider onboardingToolCallbackProvider() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(onboardingMcpTools)
                .build();
    }
}
