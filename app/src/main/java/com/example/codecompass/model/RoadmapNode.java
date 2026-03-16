package com.example.codecompass.model;

import com.google.gson.annotations.SerializedName;

public class RoadmapNode {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("nodeType")
    private String nodeType;

    @SerializedName("estimatedHours")
    private int estimatedHours;

    @SerializedName("xpReward")
    private int xpReward;

    @SerializedName("status")
    private String status;

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getNodeType() { return nodeType; }
    public int getEstimatedHours() { return estimatedHours; }
    public int getXpReward() { return xpReward; }
    public String getStatus() { return status; }

    public boolean isAvailableOrInProgress() {
        return "available".equals(status) || "in_progress".equals(status);
    }
}
