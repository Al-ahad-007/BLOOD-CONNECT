package com.bloodconnect.model;

import java.sql.Timestamp;

/**
 * Specialized User class representing an Administrator.
 * Demonstrates Inheritance and OOP Role Specialization.
 */
public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin() {
        super();
        setRole("ADMIN");
    }

    public Admin(int userId, String fullName, String email, String phone, String passwordHash,
                 String city, String state, String pincode, String address,
                 String accountStatus, Timestamp createdAt, Timestamp updatedAt) {
        super(userId, fullName, email, phone, passwordHash, "ADMIN", city, state, pincode, address,
                accountStatus, createdAt, updatedAt);
    }

    public Admin(User user) {
        super(user.getUserId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getPasswordHash(), "ADMIN", user.getCity(), user.getState(),
                user.getPincode(), user.getAddress(), user.getAccountStatus(),
                user.getCreatedAt(), user.getUpdatedAt());
    }

    @Override
    public String toString() {
        return "Admin{" + super.toString() + "}";
    }
}
