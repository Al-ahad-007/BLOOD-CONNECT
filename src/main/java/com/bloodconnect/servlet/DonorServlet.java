package com.bloodconnect.servlet;

import com.bloodconnect.exception.ApplicationException;
import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.Donor;
import com.bloodconnect.model.DonorResponse;
import com.bloodconnect.model.User;
import com.bloodconnect.service.BloodRequestService;
import com.bloodconnect.service.DonorService;
import com.bloodconnect.service.impl.BloodRequestServiceImpl;
import com.bloodconnect.service.impl.DonorServiceImpl;
import com.bloodconnect.util.ApiResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Servlet handling Donor Dashboard actions:
 * Availability toggle, incoming requests, response submission, and stats.
 * URL Patterns: /api/donor/*
 */
@WebServlet(name = "DonorServlet", urlPatterns = {
        "/api/donor/availability",
        "/api/donor/stats",
        "/api/donor/requests",
        "/api/donor/respond",
        "/api/donor/responses",
        "/api/donor/profile"
})
public class DonorServlet extends HttpServlet {

    private final DonorService donorService = new DonorServiceImpl();
    private final BloodRequestService requestService = new BloodRequestServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Donor donor = getLoggedDonor(session);
        if (donor == null) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Donor session required.");
            return;
        }

        String path = req.getServletPath();
        try {
            switch (path) {
                case "/api/donor/stats":
                    Map<String, Object> stats = donorService.getDonorStatistics(donor.getDonorId());
                    ApiResponse.sendSuccess(resp, "Donor statistics loaded.", stats);
                    break;

                case "/api/donor/requests":
                    // Relevant open emergency blood requests
                    List<BloodRequest> requests = requestService.getOpenRequestsForDonor(donor.getBloodGroup(), donor.getCity());
                    ApiResponse.sendSuccess(resp, "Incoming requests loaded.", requests);
                    break;

                case "/api/donor/responses":
                    // History of responses by this donor
                    List<DonorResponse> responses = requestService.getResponsesByDonor(donor.getDonorId());
                    ApiResponse.sendSuccess(resp, "Response history loaded.", responses);
                    break;

                case "/api/donor/profile":
                    Donor fresh = donorService.getDonorById(donor.getDonorId());
                    session.setAttribute("user", fresh);
                    ApiResponse.sendSuccess(resp, "Profile loaded.", fresh);
                    break;

                default:
                    ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
            }
        } catch (ApplicationException e) {
            ApiResponse.sendError(resp, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error handling donor request: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Donor donor = getLoggedDonor(session);
        if (donor == null) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Donor session required.");
            return;
        }

        String path = req.getServletPath();
        try {
            JsonObject json = parseJsonBody(req);

            switch (path) {
                case "/api/donor/availability":
                    String newStatus = getJsonString(json, "availabilityStatus", "");
                    if (newStatus.isEmpty()) {
                        // Toggle if not supplied
                        newStatus = donor.isAvailable() ? "UNAVAILABLE" : "AVAILABLE";
                    }
                    donorService.updateAvailability(donor.getDonorId(), newStatus);
                    donor.setAvailabilityStatus(newStatus);
                    session.setAttribute("user", donor);
                    ApiResponse.sendSuccess(resp, "Availability updated to " + newStatus, donor);
                    break;

                case "/api/donor/respond":
                    int requestId = json.get("requestId").getAsInt();
                    String status = getJsonString(json, "status", "ACCEPTED");
                    String message = getJsonString(json, "message", "");

                    DonorResponse response = requestService.respondToRequest(requestId, donor.getDonorId(), status, message);
                    ApiResponse.sendCreated(resp, "Response submitted successfully!", response);
                    break;

                case "/api/donor/profile":
                    donor.setBloodGroup(getJsonString(json, "bloodGroup", donor.getBloodGroup()));
                    donor.setFullName(getJsonString(json, "fullName", donor.getFullName()));
                    donor.setPhone(getJsonString(json, "phone", donor.getPhone()));
                    donor.setCity(getJsonString(json, "city", donor.getCity()));
                    donor.setState(getJsonString(json, "state", donor.getState()));
                    donor.setPincode(getJsonString(json, "pincode", donor.getPincode()));
                    donor.setAddress(getJsonString(json, "address", donor.getAddress()));
                    if (json.has("isDiscoverable")) {
                        donor.setDiscoverable(json.get("isDiscoverable").getAsBoolean());
                    }

                    Donor updated = donorService.updateDonorProfile(donor);
                    session.setAttribute("user", updated);
                    ApiResponse.sendSuccess(resp, "Donor profile updated successfully!", updated);
                    break;

                default:
                    ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
            }
        } catch (ApplicationException e) {
            ApiResponse.sendError(resp, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error executing donor action: " + e.getMessage());
        }
    }

    private Donor getLoggedDonor(HttpSession session) {
        if (session == null) return null;
        Object u = session.getAttribute("user");
        if (u instanceof Donor) {
            return (Donor) u;
        } else if (u instanceof User) {
            User user = (User) u;
            if (user.isDonor()) {
                try {
                    Donor d = donorService.getDonorByUserId(user.getUserId());
                    session.setAttribute("user", d);
                    return d;
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private JsonObject parseJsonBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        if (sb.length() == 0) {
            JsonObject obj = new JsonObject();
            req.getParameterMap().forEach((k, v) -> {
                if (v != null && v.length > 0) {
                    obj.addProperty(k, v[0]);
                }
            });
            return obj;
        }
        return JsonParser.parseString(sb.toString()).getAsJsonObject();
    }

    private String getJsonString(JsonObject obj, String key, String defaultVal) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString().trim();
        }
        return defaultVal;
    }
}
