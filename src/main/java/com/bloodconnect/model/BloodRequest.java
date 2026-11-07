package com.bloodconnect.model;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Model representing an Emergency Blood Request.
 * Uses Encapsulation and provides complete state mapping to database.
 */
public class BloodRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int requestId;
    private int requesterId;
    private String patientName;
    private String bloodGroup;
    private int unitsRequired;
    private String hospitalName;
    private String hospitalAddress;
    private String city;
    private String state;
    private String pincode;
    private String contactPerson;
    private String contactPhone;
    private String urgency; // NORMAL, URGENT, CRITICAL
    private Date requiredDate;
    private String description;
    private String requestStatus; // PENDING, ACCEPTED, FULFILLED, CANCELLED
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Additional display fields populated from JOINs
    private String requesterName;
    private String requesterEmail;
    private int responseCount;

    public BloodRequest() {
        this.unitsRequired = 1;
        this.urgency = "NORMAL";
        this.requestStatus = "PENDING";
    }

    public BloodRequest(int requestId, int requesterId, String patientName, String bloodGroup,
                        int unitsRequired, String hospitalName, String hospitalAddress,
                        String city, String state, String pincode, String contactPerson,
                        String contactPhone, String urgency, Date requiredDate,
                        String description, String requestStatus, Timestamp createdAt,
                        Timestamp updatedAt) {
        this.requestId = requestId;
        this.requesterId = requesterId;
        this.patientName = patientName;
        this.bloodGroup = bloodGroup != null ? bloodGroup.toUpperCase() : "";
        this.unitsRequired = unitsRequired;
        this.hospitalName = hospitalName;
        this.hospitalAddress = hospitalAddress;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.contactPerson = contactPerson;
        this.contactPhone = contactPhone;
        this.urgency = urgency != null ? urgency.toUpperCase() : "NORMAL";
        this.requiredDate = requiredDate;
        this.description = description;
        this.requestStatus = requestStatus != null ? requestStatus.toUpperCase() : "PENDING";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Helper checks
    public boolean isCritical() {
        return "CRITICAL".equalsIgnoreCase(urgency);
    }

    public boolean isUrgent() {
        return "URGENT".equalsIgnoreCase(urgency);
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(requestStatus);
    }

    public boolean isFulfilled() {
        return "FULFILLED".equalsIgnoreCase(requestStatus);
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(requestStatus);
    }

    // Getters and Setters
    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(int requesterId) {
        this.requesterId = requesterId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup != null ? bloodGroup.toUpperCase() : null;
    }

    public int getUnitsRequired() {
        return unitsRequired;
    }

    public void setUnitsRequired(int unitsRequired) {
        this.unitsRequired = unitsRequired;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getHospitalAddress() {
        return hospitalAddress;
    }

    public void setHospitalAddress(String hospitalAddress) {
        this.hospitalAddress = hospitalAddress;
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

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency != null ? urgency.toUpperCase() : "NORMAL";
    }

    public Date getRequiredDate() {
        return requiredDate;
    }

    public void setRequiredDate(Date requiredDate) {
        this.requiredDate = requiredDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus != null ? requestStatus.toUpperCase() : "PENDING";
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

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public int getResponseCount() {
        return responseCount;
    }

    public void setResponseCount(int responseCount) {
        this.responseCount = responseCount;
    }

    @Override
    public String toString() {
        return "BloodRequest{" +
                "requestId=" + requestId +
                ", patientName='" + patientName + '\'' +
                ", bloodGroup='" + bloodGroup + '\'' +
                ", unitsRequired=" + unitsRequired +
                ", hospitalName='" + hospitalName + '\'' +
                ", urgency='" + urgency + '\'' +
                ", requestStatus='" + requestStatus + '\'' +
                '}';
    }
}
