package com.bloodconnect.service;

import com.bloodconnect.exception.ApplicationException;
import com.bloodconnect.model.Donor;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Donor Search, Profile, Availability, and Metrics.
 */
public interface DonorService {

    /**
     * Search donors based on blood group, location, and availability filters.
     * Enforces privacy and active status.
     */
    List<Donor> searchDonors(String bloodGroup, String city, String state, String pincode, String availability)
            throws ApplicationException;

    /**
     * Retrieves donor details by associated user ID.
     */
    Donor getDonorByUserId(int userId) throws ApplicationException;

    /**
     * Retrieves donor details by donor ID.
     */
    Donor getDonorById(int donorId) throws ApplicationException;

    /**
     * Updates donor availability status (AVAILABLE / UNAVAILABLE).
     */
    boolean updateAvailability(int donorId, String status) throws ApplicationException;

    /**
     * Updates donor profile, location, and blood group.
     */
    Donor updateDonorProfile(Donor donor) throws ApplicationException;

    /**
     * Computes statistics for a donor's dashboard (availability, received requests, accepted requests).
     */
    Map<String, Object> getDonorStatistics(int donorId) throws ApplicationException;

    /**
     * Public platform metrics for landing page (Registered Donors, Active Requests, Requests Fulfilled).
     */
    Map<String, Object> getPublicPlatformStatistics() throws ApplicationException;

    /**
     * Admin view: lists all donors with complete details.
     */
    List<Donor> getAllDonors() throws ApplicationException;
}
