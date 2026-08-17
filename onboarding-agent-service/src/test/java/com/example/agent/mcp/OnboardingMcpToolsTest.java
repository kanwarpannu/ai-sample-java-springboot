package com.example.agent.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OnboardingMcpToolsTest {

    private final OnboardingMcpTools tools = new OnboardingMcpTools();

    @Test
    void createOnboardingPlan_returnsNonNullNonBlankPlan() {
        String result = tools.createOnboardingPlan("Alice", "Backend Engineer");
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void createOnboardingPlan_includesDeveloperName() {
        String result = tools.createOnboardingPlan("Alice", "Backend Engineer");
        assertTrue(result.contains("Alice"));
    }

    @Test
    void createOnboardingPlan_includesRole() {
        String result = tools.createOnboardingPlan("Bob", "Frontend Developer");
        assertTrue(result.contains("Frontend Developer"));
    }

    @Test
    void createOnboardingPlan_includesChecklistSteps() {
        String result = tools.createOnboardingPlan("Charlie", "DevOps Engineer");
        assertTrue(result.contains("step-1") && result.contains("step-10"));
    }

    @Test
    void getOnboardingProgress_returnsNonNullNonBlankProgress() {
        String result = tools.getOnboardingProgress("session-123");
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void getOnboardingProgress_includesSessionId() {
        String result = tools.getOnboardingProgress("session-abc");
        assertTrue(result.contains("session-abc"));
    }

    @Test
    void getOnboardingProgress_includesCompletionInfo() {
        String result = tools.getOnboardingProgress("session-xyz");
        assertTrue(result.contains("completed") || result.contains("Completed"));
    }

    @Test
    void updateOnboardingStep_completedTrue_showsCompletedStatus() {
        String result = tools.updateOnboardingStep("session-1", "step-3", true);
        assertTrue(result.contains("COMPLETED"));
    }

    @Test
    void updateOnboardingStep_completedFalse_showsReopenedStatus() {
        String result = tools.updateOnboardingStep("session-1", "step-3", false);
        assertTrue(result.contains("REOPENED"));
    }

    @Test
    void updateOnboardingStep_includesSessionAndStepId() {
        String result = tools.updateOnboardingStep("session-99", "step-7", true);
        assertTrue(result.contains("session-99"));
        assertTrue(result.contains("step-7"));
    }
}
