package com.example.agent.config;

import com.example.agent.session.SessionContext;
import com.example.agent.session.SessionContextHolder;
import com.example.agent.statemachine.AgentProcessingState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

@Slf4j
@RequiredArgsConstructor
class StateAwareToolCallback implements ToolCallback {

    private final ToolCallback delegate;

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        SessionContext ctx = SessionContextHolder.get();
        String toolName = getToolDefinition().name();

        if (ctx != null) {
            ctx.getAgentSM().transition(AgentProcessingState.CALLING_TOOL);
            log.info("Calling tool: {} | input={}", toolName, toolInput);
        }

        try {
            String result = delegate.call(toolInput);
            if (ctx != null) {
                ctx.getOnboardingSM().transitionForTool(toolName);
                ctx.getAgentSM().transition(AgentProcessingState.PROCESSING);
                log.info("Tool completed: {} | onboardingState={}", toolName, ctx.getOnboardingSM().current());
            }
            return result;
        } catch (RuntimeException e) {
            if (ctx != null) {
                ctx.getAgentSM().transition(AgentProcessingState.ERROR);
            }
            log.error("Tool failed: {} | error={}", toolName, e.getMessage());
            throw e;
        }
    }
}
