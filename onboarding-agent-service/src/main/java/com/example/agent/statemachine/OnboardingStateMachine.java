package com.example.agent.statemachine;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

@Slf4j
public class OnboardingStateMachine {

    private static final Map<OnboardingWorkflowState, Set<OnboardingWorkflowState>> ALLOWED =
            Map.of(
                    OnboardingWorkflowState.NOT_STARTED,  Set.of(OnboardingWorkflowState.PLAN_CREATED),
                    OnboardingWorkflowState.PLAN_CREATED, Set.of(OnboardingWorkflowState.IN_PROGRESS),
                    OnboardingWorkflowState.IN_PROGRESS,  Set.of(OnboardingWorkflowState.BLOCKED, OnboardingWorkflowState.COMPLETED),
                    OnboardingWorkflowState.BLOCKED,      Set.of(OnboardingWorkflowState.IN_PROGRESS),
                    OnboardingWorkflowState.COMPLETED,    Set.of()
            );

    // Maps tool name to the next onboarding state it drives (excludes createOnboardingPlan — handled specially)
    private static final Map<String, OnboardingWorkflowState> TOOL_STATE_MAP = Map.of(
            "updateOnboardingStep", OnboardingWorkflowState.IN_PROGRESS,
            "reportBlocker",        OnboardingWorkflowState.BLOCKED,
            "resolveBlocker",       OnboardingWorkflowState.IN_PROGRESS
    );

    private OnboardingWorkflowState current = OnboardingWorkflowState.NOT_STARTED;

    public OnboardingWorkflowState current() {
        return current;
    }

    public void transitionForTool(String toolName) {
        if ("createOnboardingPlan".equals(toolName)) {
            // Creating a new plan always resets to PLAN_CREATED regardless of current state
            OnboardingWorkflowState prev = current;
            current = OnboardingWorkflowState.PLAN_CREATED;
            if (prev != OnboardingWorkflowState.PLAN_CREATED) {
                log.info("[onboardingState: {}->PLAN_CREATED]", prev);
            }
            return;
        }
        OnboardingWorkflowState next = TOOL_STATE_MAP.get(toolName);
        if (next != null) {
            transition(next);
        }
    }

    public void transition(OnboardingWorkflowState next) {
        if (current == next) {
            return; // self-transition — silently skip
        }
        if (ALLOWED.getOrDefault(current, Set.of()).contains(next)) {
            OnboardingWorkflowState prev = current;
            current = next;
            log.info("[onboardingState: {}->{}]", prev, next);
        } else {
            log.warn("[onboardingState: INVALID {}->{}]", current, next);
        }
    }
}
