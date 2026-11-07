package com.bloodconnect.service.impl;

import com.bloodconnect.dao.BloodRequestDAO;
import com.bloodconnect.dao.DonorDAO;
import com.bloodconnect.dao.DonorResponseDAO;
import com.bloodconnect.dao.UserDAO;
import com.bloodconnect.exception.ApplicationException;
import com.bloodconnect.exception.ValidationException;
import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.Donor;
import com.bloodconnect.model.DonorResponse;
import com.bloodconnect.service.BloodRequestService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Implementation of BloodRequestService.
 */
public class BloodRequestServiceImpl implements BloodRequestService {

    private final BloodRequestDAO requestDAO = new BloodRequestDAO();
    private final DonorDAO donorDAO = new DonorDAO();
    private final DonorResponseDAO responseDAO = new DonorResponseDAO();
    private final UserDAO userDAO = new UserDAO();

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$");

    @Override
    public BloodRequest createRequest(BloodRequest req) throws ApplicationException {
        validateRequest(req);

        req.setRequestStatus("PENDING");
        int requestId = requestDAO.create(req);
        req.setRequestId(requestId);
        return req;
    }

    @Override
    public BloodRequest getRequestById(int requestId) throws ApplicationException {
        BloodRequest req = requestDAO.findById(requestId);
        if (req == null) {
            throw new ApplicationException("Blood request not found with ID: " + requestId, 404);
        }
        return req;
    }

    @Override
    public List<BloodRequest> getRequestsByRequester(int requesterId) throws ApplicationException {
        return requestDAO.findByRequesterId(requesterId);
    }

    @Override
    public List<BloodRequest> getOpenRequestsForDonor(String bloodGroup, String city) throws ApplicationException {
        return requestDAO.findOpenRequestsForDonor(bloodGroup, city);
    }

    @Override
    public List<BloodRequest> getAllRequests(String status, String bloodGroup, String city) throws ApplicationException {
        return requestDAO.findAll(status, bloodGroup, city);
    }

    @Override
    public boolean updateRequestStatus(int requestId, String newStatus) throws ApplicationException {
        if (!"PENDING".equalsIgnoreCase(newStatus) && !"ACCEPTED".equalsIgnoreCase(newStatus)
                && !"FULFILLED".equalsIgnoreCase(newStatus) && !"CANCELLED".equalsIgnoreCase(newStatus)) {
            throw new ValidationException("Invalid status. Allowed: PENDING, ACCEPTED, FULFILLED, CANCELLED.");
        }
        return requestDAO.updateStatus(requestId, newStatus.toUpperCase());
    }

    @Override
    public boolean cancelRequest(int requestId, int requesterId) throws ApplicationException {
        BloodRequest req = requestDAO.findById(requestId);
        if (req == null) {
            throw new ApplicationException("Blood request not found.", 404);
        }
        if (req.getRequesterId() != requesterId) {
            throw new ApplicationException("You are not authorized to cancel this request.", 403);
        }
        if ("FULFILLED".equalsIgnoreCase(req.getRequestStatus())) {
            throw new ValidationException("Cannot cancel a request that has already been fulfilled.");
        }

        return requestDAO.cancelRequest(requestId, requesterId);
    }

    @Override
    public DonorResponse respondToRequest(int requestId, int donorId, String status, String message)
            throws ApplicationException {
        if (!"ACCEPTED".equalsIgnoreCase(status) && !"DECLINED".equalsIgnoreCase(status)) {
            throw new ValidationException("Response status must be ACCEPTED or DECLINED.");
        }

        BloodRequest req = requestDAO.findById(requestId);
        if (req == null) {
            throw new ApplicationException("Blood request not found.", 404);
        }
        if ("FULFILLED".equalsIgnoreCase(req.getRequestStatus()) || "CANCELLED".equalsIgnoreCase(req.getRequestStatus())) {
            throw new ValidationException("This request is already " + req.getRequestStatus().toLowerCase() + ".");
        }

        Donor donor = donorDAO.findById(donorId);
        if (donor == null) {
            throw new ApplicationException("Donor profile not found.", 404);
        }

        if (responseDAO.exists(requestId, donorId)) {
            throw new ValidationException("You have already submitted a response for this blood request.");
        }

        DonorResponse res = new DonorResponse();
        res.setRequestId(requestId);
        res.setDonorId(donorId);
        res.setResponseStatus(status.toUpperCase());
        res.setResponseMessage(message != null ? message.trim() : "");

        int resId = responseDAO.create(res);
        res.setResponseId(resId);

        // If donor accepted, optionally update request status from PENDING to ACCEPTED
        if ("ACCEPTED".equalsIgnoreCase(status) && "PENDING".equalsIgnoreCase(req.getRequestStatus())) {
            requestDAO.updateStatus(requestId, "ACCEPTED");
        }

        return res;
    }

    @Override
    public List<DonorResponse> getResponsesForRequest(int requestId) throws ApplicationException {
        return responseDAO.findByRequestId(requestId);
    }

    @Override
    public List<DonorResponse> getResponsesByDonor(int donorId) throws ApplicationException {
        return responseDAO.findByDonorId(donorId);
    }

    @Override
    public List<BloodRequest> getRecentEmergencyRequests(int limit) throws ApplicationException {
        return requestDAO.findRecentEmergencyRequests(limit);
    }

    @Override
    public Map<String, Object> getAdminStatistics() throws ApplicationException {
        int totalUsers = userDAO.countTotalUsers();
        int totalDonors = donorDAO.countTotalDonors();
        int availableDonors = donorDAO.countAvailableDonors();
        int totalRequests = requestDAO.countTotal();
        int pendingRequests = requestDAO.countByStatus("PENDING");
        int fulfilledRequests = requestDAO.countByStatus("FULFILLED");
        int acceptedRequests = requestDAO.countByStatus("ACCEPTED");
        int cancelledRequests = requestDAO.countByStatus("CANCELLED");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalDonors", totalDonors);
        stats.put("availableDonors", availableDonors);
        stats.put("totalRequests", totalRequests);
        stats.put("pendingRequests", pendingRequests);
        stats.put("acceptedRequests", acceptedRequests);
        stats.put("fulfilledRequests", fulfilledRequests);
        stats.put("cancelledRequests", cancelledRequests);
        return stats;
    }

    private void validateRequest(BloodRequest req) throws ValidationException {
        if (req.getPatientName() == null || req.getPatientName().trim().isEmpty()) {
            throw new ValidationException("Patient name cannot be empty.");
        }
        if (req.getBloodGroup() == null || req.getBloodGroup().trim().isEmpty()) {
            throw new ValidationException("Required blood group must be selected.");
        }
        if (req.getUnitsRequired() < 1 || req.getUnitsRequired() > 20) {
            throw new ValidationException("Units required must be between 1 and 20.");
        }
        if (req.getHospitalName() == null || req.getHospitalName().trim().isEmpty()) {
            throw new ValidationException("Hospital name cannot be empty.");
        }
        if (req.getHospitalAddress() == null || req.getHospitalAddress().trim().isEmpty()) {
            throw new ValidationException("Hospital address cannot be empty.");
        }
        if (req.getCity() == null || req.getCity().trim().isEmpty()) {
            throw new ValidationException("City cannot be empty.");
        }
        if (req.getState() == null || req.getState().trim().isEmpty()) {
            throw new ValidationException("State cannot be empty.");
        }
        if (req.getPincode() == null || req.getPincode().trim().isEmpty()) {
            throw new ValidationException("PIN code cannot be empty.");
        }
        if (req.getContactPerson() == null || req.getContactPerson().trim().isEmpty()) {
            throw new ValidationException("Contact person name cannot be empty.");
        }
        if (req.getContactPhone() == null || !PHONE_PATTERN.matcher(req.getContactPhone().trim()).matches()) {
            throw new ValidationException("Please provide a valid 10-digit emergency contact phone number.");
        }
        if (req.getRequiredDate() == null) {
            throw new ValidationException("Required date must be provided.");
        }
    }
}
