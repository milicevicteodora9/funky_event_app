package com.example.funkyeventapp.models;

import java.math.BigDecimal;

public class Budget {
    private String id;
    private String eventId;
    private boolean includeVat;
    private BigDecimal discountPercentage;

    public Budget() { }
    public Budget(String id, String eventId, boolean includeVat, BigDecimal discountPercentage) {
        this.id = id; this.eventId = eventId; this.includeVat = includeVat;
        this.discountPercentage = discountPercentage;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public boolean isIncludeVat() { return includeVat; }
    public void setIncludeVat(boolean includeVat) { this.includeVat = includeVat; }
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(BigDecimal discountPercentage) { this.discountPercentage = discountPercentage; }
}
