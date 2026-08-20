package com.example.agent.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OnboardingStateMachineTest {

    private OnboardingStateMachine sm;

    @BeforeEach
    void setUp() {
        sm = new OnboardingStateMachine();
    }

    @Test
    void initialState_isNotStarted() {
        assertEquals(OnboardingWorkflowState.NOT_STARTED, sm.current());
    }

    @Test
    void createOnboardingPlan_transitionsToPlanCreated() {
        sm.transitionForTool("createOnboardingPlan");
        assertEquals(OnboardingWorkflowState.PLAN_CREATED, sm.current());
    }

    @Test
    void updateOnboardingStep_transitionsToInProgress() {
        sm.transitionForTool("createOnboardingPlan");
        sm.transitionForTool("updateOnboardingStep");
        assertEquals(OnboardingWorkflowState.IN_PROGRESS, sm.current());
    }

    @Test
    void reportBlocker_transitionsToBlocked() {
        sm.transitionForTool("createOnboardingPlan");
        sm.transitionForTool("updateOnboardingStep");
        sm.transitionForTool("reportBlocker");
        assertEquals(OnboardingWorkflowState.BLOCKED, sm.current());
    }

    @Test
    void resolveBlocker_transitionsBackToInProgress() {
        sm.transitionForTool("createOnboardingPlan");
        sm.transitionForTool("updateOnboardingStep");
        sm.transitionForTool("reportBlocker");
        sm.transitionForTool("resolveBlocker");
        assertEquals(OnboardingWorkflowState.IN_PROGRESS, sm.current());
    }

    @Test
    void createOnboardingPlan_alwaysTransitionsToPlanCreated_regardlessOfCurrentState() {
        // Even mid-session, creating a new plan resets to PLAN_CREATED
        sm.transitionForTool("createOnboardingPlan");
        sm.transitionForTool("updateOnboardingStep");
        sm.transitionForTool("createOnboardingPlan"); // second plan creation
        assertEquals(OnboardingWorkflowState.PLAN_CREATED, sm.current());
    }

    @Test
    void unknownTool_noStateChange() {
        sm.transitionForTool("someUnknownTool");
        assertEquals(OnboardingWorkflowState.NOT_STARTED, sm.current());
    }

    @Test
    void selfTransition_isSkipped() {
        sm.transitionForTool("createOnboardingPlan");
        assertEquals(OnboardingWorkflowState.PLAN_CREATED, sm.current());
        sm.transitionForTool("createOnboardingPlan"); // would be PLAN_CREATED -> PLAN_CREATED
        assertEquals(OnboardingWorkflowState.PLAN_CREATED, sm.current());
    }

    @Test
    void invalidDirectTransition_doesNotChangeState() {
        // NOT_STARTED -> IN_PROGRESS directly is not in the allowed table
        sm.transition(OnboardingWorkflowState.IN_PROGRESS);
        assertEquals(OnboardingWorkflowState.NOT_STARTED, sm.current());
    }

    @Test
    void getOnboardingProgress_doesNotAffectState() {
        sm.transitionForTool("createOnboardingPlan");
        sm.transitionForTool("getOnboardingProgress"); // not in the tool map
        assertEquals(OnboardingWorkflowState.PLAN_CREATED, sm.current());
    }
}
