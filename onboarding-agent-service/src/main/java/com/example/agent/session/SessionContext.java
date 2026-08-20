package com.example.agent.session;

import com.example.agent.statemachine.AgentStateMachine;
import com.example.agent.statemachine.OnboardingStateMachine;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;

public class SessionContext {

    private final String sessionId;
    private final List<Message> history;
    private final AgentStateMachine agentSM;
    private final OnboardingStateMachine onboardingSM;

    public SessionContext(String sessionId) {
        this.sessionId = sessionId;
        this.history = new ArrayList<>();
        this.agentSM = new AgentStateMachine();
        this.onboardingSM = new OnboardingStateMachine();
    }

    public String getSessionId() {
        return sessionId;
    }

    public List<Message> getHistory() {
        return history;
    }

    public AgentStateMachine getAgentSM() {
        return agentSM;
    }

    public OnboardingStateMachine getOnboardingSM() {
        return onboardingSM;
    }
}
