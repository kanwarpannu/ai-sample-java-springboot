package com.example.agent.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionContextStoreTest {

    private SessionContextStore store;

    @BeforeEach
    void setUp() {
        store = new SessionContextStore();
    }

    @Test
    void getOrCreate_returnsSameInstanceForSameSessionId() {
        SessionContext ctx1 = store.getOrCreate("session-1");
        SessionContext ctx2 = store.getOrCreate("session-1");
        assertSame(ctx1, ctx2);
    }

    @Test
    void getOrCreate_returnsDifferentInstancesForDifferentIds() {
        SessionContext ctx1 = store.getOrCreate("session-1");
        SessionContext ctx2 = store.getOrCreate("session-2");
        assertNotSame(ctx1, ctx2);
    }

    @Test
    void getOrCreate_newSessionHasEmptyHistory() {
        SessionContext ctx = store.getOrCreate("new-session");
        assertTrue(ctx.getHistory().isEmpty());
    }

    @Test
    void getOrCreate_newSessionIdMatchesKey() {
        SessionContext ctx = store.getOrCreate("my-session");
        assertEquals("my-session", ctx.getSessionId());
    }

    @Test
    void clear_removesSession_nextGetCreatesNewInstance() {
        SessionContext original = store.getOrCreate("session-1");
        store.clear("session-1");
        SessionContext fresh = store.getOrCreate("session-1");
        assertNotSame(original, fresh);
    }

    @Test
    void sessionCount_reflectsActiveSessions() {
        store.getOrCreate("s1");
        store.getOrCreate("s2");
        assertEquals(2, store.sessionCount());
        store.clear("s1");
        assertEquals(1, store.sessionCount());
    }
}
