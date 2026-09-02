package com.example.funkyeventapp.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TeamPayment {
    private String id, teamMemberId, description, paymentMethod, notes;
    private BigDecimal amount;
    private Currency currency;
    private LocalDate paymentDate;
    private LocalDateTime createdAt;
    public TeamPayment() { }
    public TeamPayment(String id,String teamMemberId,BigDecimal amount,Currency currency,LocalDate paymentDate,String paymentMethod,String notes){this.id=id;this.teamMemberId=teamMemberId;this.amount=amount;this.currency=currency;this.paymentDate=paymentDate;this.paymentMethod=paymentMethod;this.notes=notes;}
    public TeamPayment(String id,String teamMemberId,String description,BigDecimal amount,Currency currency,LocalDate paymentDate,String paymentMethod,String notes){this(id,teamMemberId,amount,currency,paymentDate,paymentMethod,notes);this.description=description;}
    public String getId(){return id;} public void setId(String v){id=v;} public String getTeamMemberId(){return teamMemberId;} public void setTeamMemberId(String v){teamMemberId=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public Currency getCurrency(){return currency;} public void setCurrency(Currency v){currency=v;}
    public LocalDate getPaymentDate(){return paymentDate;} public void setPaymentDate(LocalDate v){paymentDate=v;} public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}
