package com.example.funkyeventapp.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Receipt {
    private String id, receiptNumber, seller, sellerTaxId, recognizedText, scannedDocumentId;
    private LocalDate issueDate;
    private BigDecimal totalAmount;
    private Currency currency;
    private ReceiptProcessingStatus processingStatus;
    public Receipt() { }
    public Receipt(String id, String receiptNumber, String seller, String sellerTaxId, LocalDate issueDate,
            BigDecimal totalAmount, Currency currency, String recognizedText, ReceiptProcessingStatus processingStatus, String scannedDocumentId) {
        this.id=id; this.receiptNumber=receiptNumber; this.seller=seller; this.sellerTaxId=sellerTaxId; this.issueDate=issueDate;
        this.totalAmount=totalAmount; this.currency=currency; this.recognizedText=recognizedText; this.processingStatus=processingStatus; this.scannedDocumentId=scannedDocumentId;
    }
    public String getId(){return id;} public void setId(String v){id=v;} public String getReceiptNumber(){return receiptNumber;} public void setReceiptNumber(String v){receiptNumber=v;}
    public String getSeller(){return seller;} public void setSeller(String v){seller=v;} public String getSellerTaxId(){return sellerTaxId;} public void setSellerTaxId(String v){sellerTaxId=v;}
    public LocalDate getIssueDate(){return issueDate;} public void setIssueDate(LocalDate v){issueDate=v;} public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal v){totalAmount=v;}
    public Currency getCurrency(){return currency;} public void setCurrency(Currency v){currency=v;} public String getRecognizedText(){return recognizedText;} public void setRecognizedText(String v){recognizedText=v;}
    public ReceiptProcessingStatus getProcessingStatus(){return processingStatus;} public void setProcessingStatus(ReceiptProcessingStatus v){processingStatus=v;} public String getScannedDocumentId(){return scannedDocumentId;} public void setScannedDocumentId(String v){scannedDocumentId=v;}
}
