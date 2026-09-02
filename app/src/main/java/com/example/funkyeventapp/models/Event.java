package com.example.funkyeventapp.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Event {
    private String id;
    private String name;
    private EventType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private EventStatus status;
    private String clientId;
    private String billingEntity;
    private String poNumber;
    private String paymentTerms;
    private String notes;
    private boolean completed;
    private List<String> assignedUserIds = new ArrayList<>();

    public Event() { }

    public Event(String id, String name, EventType type, LocalDate startDate, LocalDate endDate,
                 String location, EventStatus status, String clientId, String billingEntity,
                 String poNumber, String paymentTerms, String notes, boolean completed) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.status = status;
        this.clientId = clientId;
        this.billingEntity = billingEntity;
        this.poNumber = poNumber;
        this.paymentTerms = paymentTerms;
        this.notes = notes;
        this.completed = completed;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getBillingEntity() { return billingEntity; }
    public void setBillingEntity(String billingEntity) { this.billingEntity = billingEntity; }
    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public List<String> getAssignedUserIds() {
        if (assignedUserIds == null) assignedUserIds = new ArrayList<>();
        return new ArrayList<>(assignedUserIds);
    }
    public void setAssignedUserIds(List<String> assignedUserIds) {
        this.assignedUserIds = assignedUserIds == null
                ? new ArrayList<>() : new ArrayList<>(assignedUserIds);
    }
    public boolean isAssignedToUser(String userId) {
        return userId != null && getAssignedUserIds().contains(userId);
    }
}
