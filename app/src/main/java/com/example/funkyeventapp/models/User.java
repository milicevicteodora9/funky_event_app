package com.example.funkyeventapp.models;

public class User {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;
    private boolean active;

    public User() { }
    public User(String id, String firstName, String lastName, String email, UserRole role, boolean active) {
        this.id = id; this.firstName = firstName; this.lastName = lastName;
        this.email = email; this.role = role; this.active = active;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getFullName() { return firstName + " " + lastName; }
}
