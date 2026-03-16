package com.example.codecompass.model;

public class ChatMessage {

    private String content;
    private final boolean isUser;
    private boolean isTyping;

    public ChatMessage(String content, boolean isUser) {
        this.content = content;
        this.isUser = isUser;
        this.isTyping = false;
    }

    /** Creates a typing-indicator placeholder (AI side, no content yet). */
    public ChatMessage(boolean isTyping) {
        this.content = "";
        this.isUser = false;
        this.isTyping = isTyping;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isUser() { return isUser; }
    public boolean isTyping() { return isTyping; }
    public void setTyping(boolean typing) { this.isTyping = typing; }
}
