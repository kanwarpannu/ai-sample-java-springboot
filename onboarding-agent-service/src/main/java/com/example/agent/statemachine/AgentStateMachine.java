package com.example.agent.statemachine;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

@Slf4j
public class AgentStateMachine {

    private static final Map<AgentProcessingState, Set<AgentProcessingState>> ALLOWED =
            Map.of(
                    AgentProcessingState.IDLE,         Set.of(AgentProcessingState.PROCESSING),
                    AgentProcessingState.PROCESSING,   Set.of(AgentProcessingState.CALLING_TOOL, AgentProcessingState.RESPONDING),
                    AgentProcessingState.CALLING_TOOL, Set.of(AgentProcessingState.PROCESSING),
                    AgentProcessingState.RESPONDING,   Set.of(AgentProcessingState.DONE),
                    AgentProcessingState.DONE,         Set.of(),
                    AgentProcessingState.ERROR,        Set.of()
            );

    private AgentProcessingState current = AgentProcessingState.IDLE;

    public AgentProcessingState current() {
        return current;
    }

    public void transition(AgentProcessingState next) {
        if (next == AgentProcessingState.ERROR
                || ALLOWED.getOrDefault(current, Set.of()).contains(next)) {
            AgentProcessingState prev = current;
            current = next;
            log.info("[agentState: {}->{}]", prev, next);
        } else {
            log.warn("[agentState: INVALID {}->{}]", current, next);
        }
    }

    public void reset() {
        AgentProcessingState prev = current;
        current = AgentProcessingState.IDLE;
        if (prev != AgentProcessingState.IDLE) {
            log.info("[agentState: {}->IDLE (reset)]", prev);
        }
    }
}
