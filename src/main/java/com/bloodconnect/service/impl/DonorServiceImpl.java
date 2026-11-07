package com.bloodconnect.service.impl;

import com.bloodconnect.dao.BloodRequestDAO;
import com.bloodconnect.dao.DonorDAO;
import com.bloodconnect.dao.DonorResponseDAO;
import com.bloodconnect.dao.UserDAO;
import com.bloodconnect.exception.ApplicationException;
import com.bloodconnect.exception.ValidationException;
import com.bloodconnect.model.Donor;
import com.bloodconnect.service.DonorService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of DonorService.
 */
public class DonorServiceImpl implements DonorService {

    private final DonorDAO donorDAO = new DonorDAO();
    private final UserDAO userDAO = new UserDAO();
    private final BloodRequestDAO requestDAO = new BloodRequestDAO();
    private final DonorResponseDAO responseDAO = new DonorResponseDAO();

    @Override
    public List<Donor> searchDonors(String bloodGroup, String city, String state, String pincode, String availability)
            throws ApplicationException {
        return donorDAO.searchDonors(bloodGroup, city, state, pincode, availability);
    }

    @Override
    public Donor getDonorByUserId(int userId) throws ApplicationException {
        Donor donor = donorDAO.findByUserId(userId);
        if (donor == null) {
            throw new ApplicationException("Donor profile not found for user ID: " + userId, 404);
        }
        return donor;
    }

    @Override
    public Donor getDonorById(int donorId) throws ApplicationException {
        Donor donor = donorDAO.findById(donorId);
        if (donor == null) {
            throw new ApplicationException("Donor not found with ID: " + donorId, 404);
        }
        return donor;
    }

    @Override
    public boolean updateAvailability(int donorId, String status) throws ApplicationException {
        if (!"AVAILABLE".equalsIgnoreCase(status) && !"UNAVAILABLE".equalsIgnoreCase(status)) {
            throw new ValidationException("Status must be either AVAILABLE or UNAVAILABLE.");
        }
        return donorDAO.updateAvailability(donorId, status.toUpperCase());
    }

    @Override
    public Donor updateDonorProfile(Donor donor) throws ApplicationException {
        if (donor.getBloodGroup() == null || donor.getBloodGroup().trim().isEmpty()) {
            throw new ValidationException("Blood group cannot be empty.");
        }
        userDAO.update(donor);
        donorDAO.updateDonor(donor);
        return donorDAO.findById(donor.getDonorId());
    }

    @Override
    public Map<String, Object> getDonorStatistics(int donorId) throws ApplicationException {
        Donor donor = donorDAO.findById(donorId);
        if (donor == null) {
            throw new ApplicationException("Donor not found", 404);
        }

        int acceptedResponses = responseDAO.countByDonorAndStatus(donorId, "ACCEPTED");
        int declinedResponses = responseDAO.countByDonorAndStatus(donorId, "DECLINED");
        int completedDonations = responseDAO.countCompletedDonations(donorId);
        int totalResponses = acceptedResponses + declinedResponses;

        // Pending/relevant blood requests for donor's blood group
        int incomingRequests = requestDAO.findOpenRequestsForDonor(donor.getBloodGroup(), donor.getCity()).size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("donorId", donorId);
        stats.put("fullName", donor.getFullName());
        stats.put("bloodGroup", donor.getBloodGroup());
        stats.put("availabilityStatus", donor.getAvailabilityStatus());
        stats.put("isAvailable", donor.isAvailable());
        stats.put("matchingRequests", incomingRequests);
        stats.put("incomingRequests", incomingRequests);
        stats.put("acceptedRequests", acceptedResponses);
        stats.put("completedDonations", completedDonations);
        stats.put("totalResponses", totalResponses);
        stats.put("lastUpdated", donor.getLastUpdated());
        return stats;
    }

    @Override
    public Map<String, Object> getPublicPlatformStatistics() throws ApplicationException {
        int totalDonors = donorDAO.countTotalDonors();
        int availableDonors = donorDAO.countAvailableDonors();
        int pendingRequests = requestDAO.countByStatus("PENDING");
        int acceptedRequests = requestDAO.countByStatus("ACCEPTED");
        int activeRequests = pendingRequests + acceptedRequests;
        int fulfilledRequests = requestDAO.countByStatus("FULFILLED");

        Map<String, Object> stats = new HashMap<>();
        stats.put("registeredDonors", totalDonors);
        stats.put("availableDonors", availableDonors);
        stats.put("activeRequests", activeRequests);
        stats.put("requestsFulfilled", fulfilledRequests);
        return stats;
    }

    @Override
    public List<Donor> getAllDonors() throws ApplicationException {
        return donorDAO.findAllDonors();
    }
}
