package com.example.funkyeventapp.models;

public class EventAssignment {
    private String id;
    private String userId;
    private String eventId;
    private String roleOnEvent;
    private boolean owner;

    public EventAssignment() { }
    public EventAssignment(String id, String userId, String eventId, String roleOnEvent, boolean owner) {
        this.id = id; this.userId = userId; this.eventId = eventId;
        this.roleOnEvent = roleOnEvent; this.owner = owner;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getRoleOnEvent() { return roleOnEvent; }
    public void setRoleOnEvent(String roleOnEvent) { this.roleOnEvent = roleOnEvent; }
    public boolean isOwner() { return owner; }
    public void setOwner(boolean owner) { this.owner = owner; }
}
