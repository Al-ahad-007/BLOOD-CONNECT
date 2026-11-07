package com.bloodconnect.model;

import java.sql.Timestamp;

/**
 * Specialized User class representing a registered Blood Donor.
 * Demonstrates Inheritance (extends User) and Polymorphism.
 */
public class Donor extends User {
    private static final long serialVersionUID = 1L;

    private int donorId;
    private String bloodGroup; // A+, A-, B+, B-, AB+, AB-, O+, O-
    private String availabilityStatus; // AVAILABLE, UNAVAILABLE
    private boolean isDiscoverable;
    private Timestamp lastUpdated;

    // Default Constructor
    public Donor() {
        super();
        setRole("DONOR");
        this.availabilityStatus = "AVAILABLE";
        this.isDiscoverable = true;
    }

    // Parameterized Constructor
    public Donor(int userId, String fullName, String email, String phone, String passwordHash,
                 String city, String state, String pincode, String address,
                 String accountStatus, Timestamp createdAt, Timestamp updatedAt,
                 int donorId, String bloodGroup, String availabilityStatus,
                 boolean isDiscoverable, Timestamp lastUpdated) {
        super(userId, fullName, email, phone, passwordHash, "DONOR", city, state, pincode, address,
                accountStatus, createdAt, updatedAt);
        this.donorId = donorId;
        this.bloodGroup = bloodGroup != null ? bloodGroup.toUpperCase() : "";
        this.availabilityStatus = availabilityStatus != null ? availabilityStatus.toUpperCase() : "AVAILABLE";
        this.isDiscoverable = isDiscoverable;
        this.lastUpdated = lastUpdated;
    }

    // Convenient Constructor from existing User
    public Donor(User user, int donorId, String bloodGroup, String availabilityStatus, boolean isDiscoverable) {
        super(user.getUserId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getPasswordHash(), "DONOR", user.getCity(), user.getState(),
                user.getPincode(), user.getAddress(), user.getAccountStatus(),
                user.getCreatedAt(), user.getUpdatedAt());
        this.donorId = donorId;
        this.bloodGroup = bloodGroup != null ? bloodGroup.toUpperCase() : "";
        this.availabilityStatus = availabilityStatus != null ? availabilityStatus.toUpperCase() : "AVAILABLE";
        this.isDiscoverable = isDiscoverable;
    }

    // Convenience method to check availability
    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(this.availabilityStatus);
    }

    // Getters and Setters
    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup != null ? bloodGroup.toUpperCase() : null;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus != null ? availabilityStatus.toUpperCase() : "AVAILABLE";
    }

    public boolean isDiscoverable() {
        return isDiscoverable;
    }

    public void setDiscoverable(boolean discoverable) {
        isDiscoverable = discoverable;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "Donor{" +
                "donorId=" + donorId +
                ", bloodGroup='" + bloodGroup + '\'' +
                ", availabilityStatus='" + availabilityStatus + '\'' +
                ", isDiscoverable=" + isDiscoverable +
                ", user=" + super.toString() +
                '}';
    }
}
