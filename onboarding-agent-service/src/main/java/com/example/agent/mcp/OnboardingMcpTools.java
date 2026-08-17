package com.example.agent.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class OnboardingMcpTools {

    @Tool(description = "Create a structured onboarding plan for a new developer joining the team")
    public String createOnboardingPlan(String developerName, String role) {
        return """
                [Onboarding Plan Created]
                Developer: %s | Role: %s
                Week 1 — Orientation:
                  [ ] step-1: Complete HR paperwork and system access requests
                  [ ] step-2: Meet your team lead and shadow a daily standup
                  [ ] step-3: Set up local development environment (see docs/local-setup.md)
                  [ ] step-4: Read architecture overview (Confluence → Engineering)
                Week 2 — Integration:
                  [ ] step-5: Complete first good-first-issue ticket
                  [ ] step-6: Pair program with a senior engineer for 2 days
                  [ ] step-7: Attend platform team knowledge-sharing session
                Week 3-4 — Independence:
                  [ ] step-8: Own a feature from design to deployment
                  [ ] step-9: Write a runbook for your first production component
                  [ ] step-10: Present your learnings to the team (15 min demo)
                """.formatted(developerName, role);
    }

    @Tool(description = "Get the current onboarding progress and completed steps for a session")
    public String getOnboardingProgress(String sessionId) {
        return """
                [Onboarding Progress for session: %s]
                Overall: 3 / 10 steps completed (30%%)
                Completed:
                  [x] step-1: HR paperwork — done on Day 1
                  [x] step-2: Met team lead — done on Day 2
                  [x] step-3: Dev environment set up — done on Day 3
                Pending:
                  [ ] step-4: Architecture overview reading
                  [ ] step-5 through step-10: in progress
                Next recommended action: Read the architecture overview on Confluence
                """.formatted(sessionId);
    }

    @Tool(description = "Mark an onboarding checklist step as completed or incomplete for a session")
    public String updateOnboardingStep(String sessionId, String stepId, boolean completed) {
        String status = completed ? "COMPLETED" : "REOPENED";
        return """
                [Step Updated]
                Session: %s | Step: %s | Status: %s
                Progress saved successfully. Use getOnboardingProgress to see updated summary.
                """.formatted(sessionId, stepId, status);
    }
}
