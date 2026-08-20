package com.example.agent.session;

public final class SessionContextHolder {

    private static final ThreadLocal<SessionContext> HOLDER = new ThreadLocal<>();

    private SessionContextHolder() {}

    public static void set(SessionContext ctx) {
        HOLDER.set(ctx);
    }

    public static SessionContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
