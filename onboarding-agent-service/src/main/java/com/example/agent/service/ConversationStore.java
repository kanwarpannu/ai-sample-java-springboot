package com.example.agent.service;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConversationStore {

    private final ConcurrentHashMap<String, List<Message>> sessions = new ConcurrentHashMap<>();

    public List<Message> getOrCreate(String sessionId) {
        return sessions.computeIfAbsent(sessionId, id -> new ArrayList<>());
    }

    public void clear(String sessionId) {
        sessions.remove(sessionId);
    }

    public int sessionCount() {
        return sessions.size();
    }
}
