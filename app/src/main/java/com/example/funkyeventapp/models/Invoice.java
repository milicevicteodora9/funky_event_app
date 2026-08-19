package com.example.funkyeventapp.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Invoice {
    private String id;
    private String eventId;
    private String clientId;
    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal amount;
    private String currency;
    private InvoiceStatus status;
    private String pdfUri;
    private String notes;

    public Invoice() { }

    public Invoice(String id, String eventId, String clientId, String invoiceNumber,
                   LocalDate issueDate, LocalDate dueDate, BigDecimal amount, String currency,
                   InvoiceStatus status, String pdfUri, String notes) {
        this.id = id;
        this.eventId = eventId;
        this.clientId = clientId;
        this.invoiceNumber = invoiceNumber;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.pdfUri = pdfUri;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
    public String getPdfUri() { return pdfUri; }
    public void setPdfUri(String pdfUri) { this.pdfUri = pdfUri; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
