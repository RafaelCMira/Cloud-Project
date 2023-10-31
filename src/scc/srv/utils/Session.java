package scc.srv.utils;

public class Session implements HasId {

    public static final String SESSION_PREFIX = "s:";

    private String sessionId;
    private String id;

    public Session(String sessionId, String userId) {
        this.sessionId = sessionId;
        this.id = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getId() {
        return id;
    }

    public void setId(String userId) {
        this.id = userId;
    }
}


