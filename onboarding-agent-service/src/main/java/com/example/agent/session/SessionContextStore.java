package com.example.agent.session;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionContextStore {

    private final ConcurrentHashMap<String, SessionContext> sessions = new ConcurrentHashMap<>();

    public SessionContext getOrCreate(String sessionId) {
        return sessions.computeIfAbsent(sessionId, SessionContext::new);
    }

    public void clear(String sessionId) {
        sessions.remove(sessionId);
    }

    public int sessionCount() {
        return sessions.size();
    }
}
