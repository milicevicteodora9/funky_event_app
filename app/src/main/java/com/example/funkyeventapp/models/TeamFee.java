package com.example.funkyeventapp.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TeamFee {
    private String id, teamMemberId, eventId, description, notes;
    private BigDecimal amount;
    private Currency currency;
    private LocalDate date;
    public TeamFee() { }
    public TeamFee(String id,String teamMemberId,String eventId,String description,BigDecimal amount,Currency currency,LocalDate date,String notes){this.id=id;this.teamMemberId=teamMemberId;this.eventId=eventId;this.description=description;this.amount=amount;this.currency=currency;this.date=date;this.notes=notes;}
    public String getId(){return id;} public void setId(String v){id=v;} public String getTeamMemberId(){return teamMemberId;} public void setTeamMemberId(String v){teamMemberId=v;} public String getEventId(){return eventId;} public void setEventId(String v){eventId=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public Currency getCurrency(){return currency;} public void setCurrency(Currency v){currency=v;}
    public LocalDate getDate(){return date;} public void setDate(LocalDate v){date=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
