package com.example.agent.statemachine;

public enum AgentProcessingState {
    IDLE,
    PROCESSING,
    CALLING_TOOL,
    RESPONDING,
    DONE,
    ERROR
}
