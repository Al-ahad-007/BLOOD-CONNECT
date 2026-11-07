package com.bloodconnect.service;

import com.bloodconnect.exception.ApplicationException;
import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.DonorResponse;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Blood Requests, Donor Responses, and System Metrics.
 */
public interface BloodRequestService {

    /**
     * Submits a new emergency blood request after performing server-side validation.
     */
    BloodRequest createRequest(BloodRequest request) throws ApplicationException;

    /**
     * Finds request by ID.
     */
    BloodRequest getRequestById(int requestId) throws ApplicationException;

    /**
     * Retrieves all requests submitted by a specific user.
     */
    List<BloodRequest> getRequestsByRequester(int requesterId) throws ApplicationException;

    /**
     * Finds open emergency requests relevant to a donor.
     */
    List<BloodRequest> getOpenRequestsForDonor(String bloodGroup, String city) throws ApplicationException;

    /**
     * Lists all blood requests with optional filters (for Admin).
     */
    List<BloodRequest> getAllRequests(String status, String bloodGroup, String city) throws ApplicationException;

    /**
     * Updates request status with validation.
     */
    boolean updateRequestStatus(int requestId, String newStatus) throws ApplicationException;

    /**
     * Cancels an existing blood request.
     */
    boolean cancelRequest(int requestId, int requesterId) throws ApplicationException;

    /**
     * Records a donor's response (ACCEPT/DECLINE) to a blood request.
     */
    DonorResponse respondToRequest(int requestId, int donorId, String status, String message)
            throws ApplicationException;

    /**
     * Retrieves all donor responses for a blood request.
     */
    List<DonorResponse> getResponsesForRequest(int requestId) throws ApplicationException;

    /**
     * Retrieves all responses submitted by a donor.
     */
    List<DonorResponse> getResponsesByDonor(int donorId) throws ApplicationException;

    /**
     * Retrieves recent emergency requests for public homepage live feed.
     */
    List<BloodRequest> getRecentEmergencyRequests(int limit) throws ApplicationException;

    /**
     * Gathers comprehensive admin metrics.
     */
    Map<String, Object> getAdminStatistics() throws ApplicationException;
}
