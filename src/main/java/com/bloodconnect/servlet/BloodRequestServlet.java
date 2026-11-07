package com.bloodconnect.servlet;

import com.bloodconnect.exception.ApplicationException;
import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.DonorResponse;
import com.bloodconnect.model.User;
import com.bloodconnect.service.BloodRequestService;
import com.bloodconnect.service.impl.BloodRequestServiceImpl;
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
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet handling Blood Request submission, tracking, cancellation, and response inspection.
 * URL Patterns: /api/requests/*
 */
@WebServlet(name = "BloodRequestServlet", urlPatterns = {
        "/api/requests/create",
        "/api/requests/my",
        "/api/requests/detail",
        "/api/requests/cancel",
        "/api/requests/recent"
})
public class BloodRequestServlet extends HttpServlet {

    private final BloodRequestService requestService = new BloodRequestServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        try {
            switch (path) {
                case "/api/requests/my": {
                    HttpSession session = req.getSession(false);
                    if (session == null || session.getAttribute("user") == null) {
                        ApiResponse.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Login required.");
                        return;
                    }
                    User user = (User) session.getAttribute("user");
                    List<BloodRequest> myRequests = requestService.getRequestsByRequester(user.getUserId());
                    ApiResponse.sendSuccess(resp, "My requests retrieved.", myRequests);
                    break;
                }

                case "/api/requests/detail": {
                    String idStr = req.getParameter("id");
                    if (idStr == null || idStr.trim().isEmpty()) {
                        ApiResponse.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Request ID required.");
                        return;
                    }
                    int requestId = Integer.parseInt(idStr);
                    BloodRequest bloodRequest = requestService.getRequestById(requestId);
                    List<DonorResponse> responses = requestService.getResponsesForRequest(requestId);

                    Map<String, Object> data = new HashMap<>();
                    data.put("request", bloodRequest);
                    data.put("responses", responses);
                    ApiResponse.sendSuccess(resp, "Request details retrieved.", data);
                    break;
                }

                case "/api/requests/recent": {
                    // Open emergency requests for public view
                    List<BloodRequest> recent = requestService.getRecentEmergencyRequests(5);
                    ApiResponse.sendSuccess(resp, "Recent requests retrieved.", recent);
                    break;
                }

                default:
                    ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
            }
        } catch (ApplicationException e) {
            ApiResponse.sendError(resp, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error handling blood request query: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to submit or manage requests.");
            return;
        }

        User user = (User) session.getAttribute("user");

        try {
            JsonObject json = parseJsonBody(req);

            switch (path) {
                case "/api/requests/create": {
                    BloodRequest bloodReq = new BloodRequest();
                    bloodReq.setRequesterId(user.getUserId());
                    bloodReq.setPatientName(getJsonString(json, "patientName", ""));
                    bloodReq.setBloodGroup(getJsonString(json, "bloodGroup", ""));
                    bloodReq.setUnitsRequired(json.has("unitsRequired") ? json.get("unitsRequired").getAsInt() : 1);
                    bloodReq.setHospitalName(getJsonString(json, "hospitalName", ""));
                    bloodReq.setHospitalAddress(getJsonString(json, "hospitalAddress", ""));
                    bloodReq.setCity(getJsonString(json, "city", ""));
                    bloodReq.setState(getJsonString(json, "state", ""));
                    bloodReq.setPincode(getJsonString(json, "pincode", ""));
                    bloodReq.setContactPerson(getJsonString(json, "contactPerson", user.getFullName()));
                    bloodReq.setContactPhone(getJsonString(json, "contactPhone", user.getPhone()));
                    bloodReq.setUrgency(getJsonString(json, "urgency", "NORMAL"));
                    bloodReq.setDescription(getJsonString(json, "description", ""));

                    String dateStr = getJsonString(json, "requiredDate", "");
                    if (!dateStr.isEmpty()) {
                        bloodReq.setRequiredDate(Date.valueOf(dateStr));
                    }

                    BloodRequest created = requestService.createRequest(bloodReq);
                    ApiResponse.sendCreated(resp, "Emergency Blood Request submitted successfully!", created);
                    break;
                }

                case "/api/requests/cancel": {
                    int requestId = json.get("requestId").getAsInt();
                    requestService.cancelRequest(requestId, user.getUserId());
                    ApiResponse.sendSuccess(resp, "Request cancelled successfully.", null);
                    break;
                }

                default:
                    ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
            }
        } catch (ApplicationException e) {
            ApiResponse.sendError(resp, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error executing request operation: " + e.getMessage());
        }
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
