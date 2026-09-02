package com.example.funkyeventapp.models;

public class TeamMember {
    private String id, firstName, lastName, phone, email, city, bankAccount, notes;
    private boolean active;
    public TeamMember() { }
    public TeamMember(String id, String fullName, String phone, String email, String city, String bankAccount, String notes, boolean active) {
        this.id=id; setFullName(fullName); this.phone=phone; this.email=email; this.city=city; this.bankAccount=bankAccount; this.notes=notes; this.active=active;
    }
    public TeamMember(String id, String firstName, String lastName, String phone, String email,
                      String city, String bankAccount, String notes, boolean active) {
        this.id=id; this.firstName=firstName; this.lastName=lastName; this.phone=phone; this.email=email;
        this.city=city; this.bankAccount=bankAccount; this.notes=notes; this.active=active;
    }
    public String getId(){return id;} public void setId(String v){id=v;}
    public String getFirstName(){return firstName;} public void setFirstName(String v){firstName=v;}
    public String getLastName(){return lastName;} public void setLastName(String v){lastName=v;}
    public String getFullName(){return ((firstName==null?"":firstName.trim())+" "+(lastName==null?"":lastName.trim())).trim();}
    public void setFullName(String v){String name=v==null?"":v.trim();int split=name.lastIndexOf(' ');if(split<0){firstName=name;lastName="";}else{firstName=name.substring(0,split).trim();lastName=name.substring(split+1).trim();}}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getCity(){return city;} public void setCity(String v){city=v;} public String getBankAccount(){return bankAccount;} public void setBankAccount(String v){bankAccount=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
