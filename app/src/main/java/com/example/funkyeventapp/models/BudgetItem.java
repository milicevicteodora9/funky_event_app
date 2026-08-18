package com.example.funkyeventapp.models;

import java.math.BigDecimal;

public class BudgetItem {
    private String id;
    private String eventId;
    private BudgetType budgetType;
    private String categoryId;
    private String description;
    private BigDecimal quantity;
    private BigDecimal days;
    private BigDecimal dailyRate;
    private String notes;
    private BudgetItemSource sourceType;
    private String sourceTransactionId;
    private String sourceBudgetItemId;

    public BudgetItem() { }
    public BudgetItem(String id, String eventId, BudgetType budgetType, String categoryId,
                      String description, BigDecimal quantity, BigDecimal days, BigDecimal dailyRate,
                      String notes, BudgetItemSource sourceType, String sourceTransactionId,
                      String sourceBudgetItemId) {
        this.id = id; this.eventId = eventId; this.budgetType = budgetType; this.categoryId = categoryId;
        this.description = description; this.quantity = quantity; this.days = days; this.dailyRate = dailyRate;
        this.notes = notes; this.sourceType = sourceType; this.sourceTransactionId = sourceTransactionId;
        this.sourceBudgetItemId = sourceBudgetItemId;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public BudgetType getBudgetType() { return budgetType; }
    public void setBudgetType(BudgetType budgetType) { this.budgetType = budgetType; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getDays() { return days; }
    public void setDays(BigDecimal days) { this.days = days; }
    public BigDecimal getDailyRate() { return dailyRate; }
    public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public BudgetItemSource getSourceType() { return sourceType; }
    public void setSourceType(BudgetItemSource sourceType) { this.sourceType = sourceType; }
    public String getSourceTransactionId() { return sourceTransactionId; }
    public void setSourceTransactionId(String sourceTransactionId) { this.sourceTransactionId = sourceTransactionId; }
    public String getSourceBudgetItemId() { return sourceBudgetItemId; }
    public void setSourceBudgetItemId(String sourceBudgetItemId) { this.sourceBudgetItemId = sourceBudgetItemId; }
    public BigDecimal getTotal() {
        BigDecimal q = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal d = days == null ? BigDecimal.ZERO : days;
        BigDecimal rate = dailyRate == null ? BigDecimal.ZERO : dailyRate;
        return q.multiply(d).multiply(rate);
    }
}
