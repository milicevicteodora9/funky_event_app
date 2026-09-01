package com.example.funkyeventapp.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Cashbox {
    private String id;
    private String userId;
    private Currency displayCurrency;
    private BigDecimal receivedAmount = BigDecimal.ZERO;
    private LocalDateTime createdAt;
    public Cashbox() { }
    public Cashbox(String id, String userId, Currency displayCurrency) { this.id=id; this.userId=userId; this.displayCurrency=displayCurrency; }
    public String getId() { return id; } public void setId(String id) { this.id=id; }
    public String getUserId() { return userId; } public void setUserId(String userId) { this.userId=userId; }
    public Currency getDisplayCurrency() { return displayCurrency; } public void setDisplayCurrency(Currency value) { this.displayCurrency=value; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal value) { receivedAmount = value == null ? BigDecimal.ZERO : value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
}
