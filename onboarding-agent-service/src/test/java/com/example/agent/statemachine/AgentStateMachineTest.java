package com.example.agent.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentStateMachineTest {

    private AgentStateMachine sm;

    @BeforeEach
    void setUp() {
        sm = new AgentStateMachine();
    }

    @Test
    void initialState_isIdle() {
        assertEquals(AgentProcessingState.IDLE, sm.current());
    }

    @Test
    void validTransition_idleToProcessing() {
        sm.transition(AgentProcessingState.PROCESSING);
        assertEquals(AgentProcessingState.PROCESSING, sm.current());
    }

    @Test
    void invalidTransition_doesNotChangeState() {
        sm.transition(AgentProcessingState.DONE); // IDLE->DONE is invalid
        assertEquals(AgentProcessingState.IDLE, sm.current());
    }

    @Test
    void errorTransition_isAlwaysAllowed() {
        sm.transition(AgentProcessingState.ERROR); // from IDLE — forced regardless of table
        assertEquals(AgentProcessingState.ERROR, sm.current());
    }

    @Test
    void reset_alwaysReturnsToIdle() {
        sm.transition(AgentProcessingState.PROCESSING);
        sm.transition(AgentProcessingState.CALLING_TOOL);
        sm.reset();
        assertEquals(AgentProcessingState.IDLE, sm.current());
    }

    @Test
    void reset_fromIdle_remainsIdle() {
        sm.reset();
        assertEquals(AgentProcessingState.IDLE, sm.current());
    }

    @Test
    void fullProcessingCycle_succeedsWithValidTransitions() {
        sm.transition(AgentProcessingState.PROCESSING);
        sm.transition(AgentProcessingState.CALLING_TOOL);
        sm.transition(AgentProcessingState.PROCESSING);
        sm.transition(AgentProcessingState.RESPONDING);
        sm.transition(AgentProcessingState.DONE);
        assertEquals(AgentProcessingState.DONE, sm.current());
    }

    @Test
    void multipleToolCalls_cycleBackToProcessing() {
        sm.transition(AgentProcessingState.PROCESSING);
        sm.transition(AgentProcessingState.CALLING_TOOL);
        sm.transition(AgentProcessingState.PROCESSING);
        sm.transition(AgentProcessingState.CALLING_TOOL);
        sm.transition(AgentProcessingState.PROCESSING);
        assertEquals(AgentProcessingState.PROCESSING, sm.current());
    }
}
