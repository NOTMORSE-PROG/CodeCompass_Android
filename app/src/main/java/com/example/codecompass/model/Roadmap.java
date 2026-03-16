package com.example.codecompass.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Roadmap {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("completionPercentage")
    private int completionPercentage;

    @SerializedName("nodes")
    private List<RoadmapNode> nodes;

    public int getId() { return id; }
    public String getTitle() { return title; }
    public int getCompletionPercentage() { return completionPercentage; }
    public List<RoadmapNode> getNodes() { return nodes; }
}
