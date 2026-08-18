package com.example.funkyeventapp.models;

import androidx.annotation.DrawableRes;

public class Event {
    private final String id;
    private final String name;
    private final EventType type;
    private final String startDate;
    private final String endDate;
    private final String location;
    private final String clientName;
    private final EventStatus status;
    @DrawableRes private final int logoResource;

    public Event(String id, String name, EventType type, String startDate, String endDate,
                 String location, String clientName, EventStatus status, @DrawableRes int logoResource) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.clientName = clientName;
        this.status = status;
        this.logoResource = logoResource;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public EventType getType() { return type; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getLocation() { return location; }
    public String getClientName() { return clientName; }
    public EventStatus getStatus() { return status; }
    public int getLogoResource() { return logoResource; }
    public String getDateDisplay() {
        return endDate == null || endDate.isEmpty() ? startDate : startDate + " – " + endDate;
    }
}
