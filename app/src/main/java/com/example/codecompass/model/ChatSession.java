package com.example.codecompass.model;

import com.google.gson.annotations.SerializedName;

public class ChatSession {

    @SerializedName("session_id")
    private String sessionId;

    @SerializedName("context_type")
    private String contextType;

    @SerializedName("title")
    private String title;

    @SerializedName("updated_at")
    private String updatedAt;

    public String getSessionId()   { return sessionId; }
    public String getContextType() { return contextType; }
    public String getTitle()       { return title; }
    public String getUpdatedAt()   { return updatedAt; }
}
