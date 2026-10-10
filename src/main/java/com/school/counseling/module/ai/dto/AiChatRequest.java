package com.school.counseling.module.ai.dto;

public class AiChatRequest {
    private String query;
    private String session_id;

    public AiChatRequest() {}

    public AiChatRequest(String query, String session_id) {
        this.query = query;
        this.session_id = session_id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }
}
