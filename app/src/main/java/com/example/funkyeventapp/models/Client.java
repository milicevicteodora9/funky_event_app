package com.example.funkyeventapp.models;

public class Client {
    private String id;
    private String name;
    private String logoUri;
    private String taxId;
    private String address;
    private String email;
    private String phone;
    private String contactPerson;

    public Client() { }
    public Client(String id, String name, String logoUri, String taxId, String address,
                  String email, String phone, String contactPerson) {
        this.id = id; this.name = name; this.logoUri = logoUri; this.taxId = taxId;
        this.address = address; this.email = email; this.phone = phone; this.contactPerson = contactPerson;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogoUri() { return logoUri; }
    public void setLogoUri(String logoUri) { this.logoUri = logoUri; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
}
