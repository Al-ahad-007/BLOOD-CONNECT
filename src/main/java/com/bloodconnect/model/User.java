package com.bloodconnect.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Base User model demonstrating Encapsulation and acting as base class
 * for specialized roles in Java OOP.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private String role; // DONOR, REQUESTER, ADMIN
    private String city;
    private String state;
    private String pincode;
    private String address;
    private String accountStatus; // ACTIVE, INACTIVE, SUSPENDED
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Default Constructor
    public User() {
        this.role = "REQUESTER";
        this.accountStatus = "ACTIVE";
    }

    // Parameterized Constructor
    public User(int userId, String fullName, String email, String phone, String passwordHash,
                String role, String city, String state, String pincode, String address,
                String accountStatus, Timestamp createdAt, Timestamp updatedAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role != null ? role.toUpperCase() : "REQUESTER";
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.address = address;
        this.accountStatus = accountStatus != null ? accountStatus.toUpperCase() : "ACTIVE";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Convenient Constructor for Registration
    public User(String fullName, String email, String phone, String passwordHash,
                String role, String city, String state, String pincode, String address) {
        this();
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role != null ? role.toUpperCase() : "REQUESTER";
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.address = address;
    }

    // Business Logic Methods
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.accountStatus);
    }

    public boolean hasRole(String targetRole) {
        return this.role != null && this.role.equalsIgnoreCase(targetRole);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isDonor() {
        return hasRole("DONOR");
    }

    public boolean isRequester() {
        return hasRole("REQUESTER");
    }

    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role != null ? role.toUpperCase() : null;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus != null ? accountStatus.toUpperCase() : null;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", city='" + city + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                '}';
    }
}
