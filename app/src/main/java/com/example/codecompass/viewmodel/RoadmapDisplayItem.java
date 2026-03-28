package com.example.codecompass.viewmodel;

import com.example.codecompass.model.RoadmapNode;

/**
 * Flat list item for the RoadmapAdapter RecyclerView.
 * The roadmap tree (milestones → child nodes) is flattened into this list
 * with three view types: milestone header, subsection divider, and node card.
 */
public class RoadmapDisplayItem {

    public static final int TYPE_MILESTONE  = 0;
    public static final int TYPE_DIVIDER    = 1;
    public static final int TYPE_NODE_CARD  = 2;

    private final int type;

    // TYPE_MILESTONE fields
    private RoadmapNode milestone;
    private int phaseTotal;
    private int phaseDone;
    private int phaseXpRemaining;

    // TYPE_DIVIDER fields
    private String dividerLabel;

    // TYPE_NODE_CARD fields
    private RoadmapNode node;

    // ── Factory constructors ──────────────────────────────────────────────────

    public static RoadmapDisplayItem milestone(RoadmapNode milestone,
                                                int total, int done, int xpRemaining) {
        RoadmapDisplayItem item = new RoadmapDisplayItem(TYPE_MILESTONE);
        item.milestone = milestone;
        item.phaseTotal = total;
        item.phaseDone = done;
        item.phaseXpRemaining = xpRemaining;
        return item;
    }

    public static RoadmapDisplayItem divider(String label) {
        RoadmapDisplayItem item = new RoadmapDisplayItem(TYPE_DIVIDER);
        item.dividerLabel = label;
        return item;
    }

    public static RoadmapDisplayItem nodeCard(RoadmapNode node) {
        RoadmapDisplayItem item = new RoadmapDisplayItem(TYPE_NODE_CARD);
        item.node = node;
        return item;
    }

    private RoadmapDisplayItem(int type) {
        this.type = type;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int getType() { return type; }

    public RoadmapNode getMilestone() { return milestone; }
    public int getPhaseTotal() { return phaseTotal; }
    public int getPhaseDone() { return phaseDone; }
    public int getPhaseXpRemaining() { return phaseXpRemaining; }

    public String getDividerLabel() { return dividerLabel; }

    public RoadmapNode getNode() { return node; }
}
