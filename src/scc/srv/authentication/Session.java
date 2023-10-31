package scc.srv.authentication;

import scc.srv.utils.HasId;

public class Session implements HasId {

    public static final String SESSION_PREFIX = "s:";

    private String sessionId;
    private String userId;

    public Session() {
    }

    public Session(String sessionId, String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getId() {
        return userId;
    }

    public void setId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "Session{" +
                "sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}


