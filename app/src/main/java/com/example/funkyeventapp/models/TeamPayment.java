package com.example.funkyeventapp.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TeamPayment {
    private String id, teamMemberId, paymentMethod, notes;
    private BigDecimal amount;
    private Currency currency;
    private LocalDate paymentDate;
    public TeamPayment() { }
    public TeamPayment(String id,String teamMemberId,BigDecimal amount,Currency currency,LocalDate paymentDate,String paymentMethod,String notes){this.id=id;this.teamMemberId=teamMemberId;this.amount=amount;this.currency=currency;this.paymentDate=paymentDate;this.paymentMethod=paymentMethod;this.notes=notes;}
    public String getId(){return id;} public void setId(String v){id=v;} public String getTeamMemberId(){return teamMemberId;} public void setTeamMemberId(String v){teamMemberId=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public Currency getCurrency(){return currency;} public void setCurrency(Currency v){currency=v;}
    public LocalDate getPaymentDate(){return paymentDate;} public void setPaymentDate(LocalDate v){paymentDate=v;} public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
