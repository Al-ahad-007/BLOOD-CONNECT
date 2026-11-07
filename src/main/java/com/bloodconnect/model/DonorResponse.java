package com.bloodconnect.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model representing a Donor's response to an Emergency Blood Request.
 */
public class DonorResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private int responseId;
    private int requestId;
    private int donorId;
    private String responseStatus; // ACCEPTED, DECLINED
    private String responseMessage;
    private Timestamp respondedAt;

    // Joined fields for display
    private String donorName;
    private String donorPhone;
    private String donorCity;
    private String bloodGroup;
    private String patientName;
    private String hospitalName;
    private String requestStatus;

    public DonorResponse() {
        this.responseStatus = "ACCEPTED";
    }

    public DonorResponse(int responseId, int requestId, int donorId, String responseStatus,
                         String responseMessage, Timestamp respondedAt) {
        this.responseId = responseId;
        this.requestId = requestId;
        this.donorId = donorId;
        this.responseStatus = responseStatus != null ? responseStatus.toUpperCase() : "ACCEPTED";
        this.responseMessage = responseMessage;
        this.respondedAt = respondedAt;
    }

    // Getters and Setters
    public int getResponseId() {
        return responseId;
    }

    public void setResponseId(int responseId) {
        this.responseId = responseId;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus != null ? responseStatus.toUpperCase() : "ACCEPTED";
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public Timestamp getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Timestamp respondedAt) {
        this.respondedAt = respondedAt;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonorPhone() {
        return donorPhone;
    }

    public void setDonorPhone(String donorPhone) {
        this.donorPhone = donorPhone;
    }

    public String getDonorCity() {
        return donorCity;
    }

    public void setDonorCity(String donorCity) {
        this.donorCity = donorCity;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    @Override
    public String toString() {
        return "DonorResponse{" +
                "responseId=" + responseId +
                ", requestId=" + requestId +
                ", donorId=" + donorId +
                ", responseStatus='" + responseStatus + '\'' +
                ", respondedAt=" + respondedAt +
                '}';
    }
}
