package com.example.funkyeventapp.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CashboxTransaction {
    private String id, cashboxId, name, description, eventId, receiptId;
    private BigDecimal amount, exchangeRate, amountInEur;
    private Currency currency;
    private LocalDate date;
    private TransactionType transactionType;
    private ExpensePurpose expensePurpose;
    public CashboxTransaction() { }
    public CashboxTransaction(String id, String cashboxId, String name, String description, BigDecimal amount,
            Currency currency, BigDecimal exchangeRate, BigDecimal amountInEur, LocalDate date,
            TransactionType transactionType, ExpensePurpose expensePurpose, String eventId, String receiptId) {
        this.id=id; this.cashboxId=cashboxId; this.name=name; this.description=description; this.amount=amount;
        this.currency=currency; this.exchangeRate=exchangeRate; this.amountInEur=amountInEur; this.date=date;
        this.transactionType=transactionType; this.expensePurpose=expensePurpose; this.eventId=eventId; this.receiptId=receiptId;
    }
    public String getId(){return id;} public void setId(String v){id=v;} public String getCashboxId(){return cashboxId;} public void setCashboxId(String v){cashboxId=v;}
    public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public Currency getCurrency(){return currency;} public void setCurrency(Currency v){currency=v;}
    public BigDecimal getExchangeRate(){return exchangeRate;} public void setExchangeRate(BigDecimal v){exchangeRate=v;} public BigDecimal getAmountInEur(){return amountInEur;} public void setAmountInEur(BigDecimal v){amountInEur=v;}
    public LocalDate getDate(){return date;} public void setDate(LocalDate v){date=v;} public TransactionType getTransactionType(){return transactionType;} public void setTransactionType(TransactionType v){transactionType=v;}
    public ExpensePurpose getExpensePurpose(){return expensePurpose;} public void setExpensePurpose(ExpensePurpose v){expensePurpose=v;} public String getEventId(){return eventId;} public void setEventId(String v){eventId=v;}
    public String getReceiptId(){return receiptId;} public void setReceiptId(String v){receiptId=v;}
}
