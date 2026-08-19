package com.example.onboarding.mcp.tool;

import com.example.onboarding.mcp.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OnboardingMcpTools {

    private final OnboardingService onboardingService;

    @Tool(description = "Creates a new onboarding plan for a developer. Returns a Plan ID that must be used in all subsequent tool calls for this developer.")
    public String createOnboardingPlan(String developerName, String role) {
        log.info("[TOOL] createOnboardingPlan called | developerName='{}', role='{}'", developerName, role);
        String result = onboardingService.createOnboardingPlan(developerName, role);
        log.info("[TOOL] createOnboardingPlan completed");
        return result;
    }

    @Tool(description = "Returns the current onboarding progress for a developer: completed vs. total steps, remaining step titles, and any open blockers.")
    public String getOnboardingProgress(String planId) {
        log.info("[TOOL] getOnboardingProgress called | planId='{}'", planId);
        String result = onboardingService.getOnboardingProgress(planId);
        log.info("[TOOL] getOnboardingProgress completed");
        return result;
    }

    @Tool(description = "Marks an onboarding step as completed or reopens it. stepNumber is 1-based (1 to 10). completed=true marks it done, false reopens it.")
    public String updateOnboardingStep(String planId, int stepNumber, boolean completed) {
        log.info("[TOOL] updateOnboardingStep called | planId='{}', stepNumber={}, completed={}", planId, stepNumber, completed);
        String result = onboardingService.updateOnboardingStep(planId, stepNumber, completed);
        log.info("[TOOL] updateOnboardingStep completed");
        return result;
    }

    @Tool(description = "Reports a blocker preventing a developer from completing a specific step. Returns a Blocker ID to be used when resolving the blocker.")
    public String reportBlocker(String planId, int stepNumber, String description) {
        log.info("[TOOL] reportBlocker called | planId='{}', stepNumber={}", planId, stepNumber);
        String result = onboardingService.reportBlocker(planId, stepNumber, description);
        log.info("[TOOL] reportBlocker completed");
        return result;
    }

    @Tool(description = "Marks a blocker as resolved. Use the Blocker ID returned by reportBlocker.")
    public String resolveBlocker(long blockerId) {
        log.info("[TOOL] resolveBlocker called | blockerId={}", blockerId);
        String result = onboardingService.resolveBlocker(blockerId);
        log.info("[TOOL] resolveBlocker completed");
        return result;
    }
}
