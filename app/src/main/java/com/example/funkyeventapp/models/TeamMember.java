package com.example.funkyeventapp.models;

public class TeamMember {
    private String id, fullName, phone, email, city, bankAccount, notes;
    private boolean active;
    public TeamMember() { }
    public TeamMember(String id, String fullName, String phone, String email, String city, String bankAccount, String notes, boolean active) {
        this.id=id; this.fullName=fullName; this.phone=phone; this.email=email; this.city=city; this.bankAccount=bankAccount; this.notes=notes; this.active=active;
    }
    public String getId(){return id;} public void setId(String v){id=v;} public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getCity(){return city;} public void setCity(String v){city=v;} public String getBankAccount(){return bankAccount;} public void setBankAccount(String v){bankAccount=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
