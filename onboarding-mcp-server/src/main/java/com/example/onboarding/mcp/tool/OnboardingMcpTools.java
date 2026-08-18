package com.example.onboarding.mcp.tool;

import com.example.onboarding.mcp.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingMcpTools {

    private final OnboardingService onboardingService;

    @Tool(description = "Creates a new onboarding plan for a developer. Returns a Plan ID that must be used in all subsequent tool calls for this developer.")
    public String createOnboardingPlan(String developerName, String role) {
        return onboardingService.createOnboardingPlan(developerName, role);
    }

    @Tool(description = "Returns the current onboarding progress for a developer: completed vs. total steps, remaining step titles, and any open blockers.")
    public String getOnboardingProgress(String planId) {
        return onboardingService.getOnboardingProgress(planId);
    }

    @Tool(description = "Marks an onboarding step as completed or reopens it. stepNumber is 1-based (1 to 10). completed=true marks it done, false reopens it.")
    public String updateOnboardingStep(String planId, int stepNumber, boolean completed) {
        return onboardingService.updateOnboardingStep(planId, stepNumber, completed);
    }

    @Tool(description = "Reports a blocker preventing a developer from completing a specific step. Returns a Blocker ID to be used when resolving the blocker.")
    public String reportBlocker(String planId, int stepNumber, String description) {
        return onboardingService.reportBlocker(planId, stepNumber, description);
    }

    @Tool(description = "Marks a blocker as resolved. Use the Blocker ID returned by reportBlocker.")
    public String resolveBlocker(long blockerId) {
        return onboardingService.resolveBlocker(blockerId);
    }
}
