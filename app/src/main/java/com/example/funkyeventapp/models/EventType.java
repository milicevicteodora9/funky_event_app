package com.example.funkyeventapp.models;

public enum EventType {
    EVENT("Event"), CAMPAIGN("Campaign");

    private final String label;
    EventType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
