package com.example.tryoutranking.model;

public enum PlayerStatus {
    UNREVIEWED("Unreviewed"),
    SELECTED("Selected"),
    MAYBE("Maybe"),
    RESERVE("Reserve"),
    REJECTED("Rejected"),
    WITHDRAWN("Withdrawn");

    private final String displayName;

    PlayerStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static PlayerStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNREVIEWED;
        }
        for (PlayerStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(value.trim()) || status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        return UNREVIEWED;
    }
}
