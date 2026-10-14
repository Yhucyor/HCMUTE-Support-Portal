package com.school.counseling.module.ai.dto;

import java.util.List;

public class AiChatResponse {
    private String answer;
    private List<Object> sources;
    private String session_id;
    private String provider;

    public AiChatResponse() {}

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<Object> getSources() {
        return sources;
    }

    public void setSources(List<Object> sources) {
        this.sources = sources;
    }

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
