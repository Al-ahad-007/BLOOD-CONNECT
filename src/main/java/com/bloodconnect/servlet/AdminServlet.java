package com.bloodconnect.servlet;

import com.bloodconnect.exception.ApplicationException;
import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.Donor;
import com.bloodconnect.model.User;
import com.bloodconnect.service.AuthService;
import com.bloodconnect.service.BloodRequestService;
import com.bloodconnect.service.DonorService;
import com.bloodconnect.service.impl.AuthServiceImpl;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Servlet handling Administrator Dashboard operations:
 * System statistics, user moderation, donor oversight, and request status administration.
 * Protected by AdminAuthorizationFilter.
 * URL Patterns: /api/admin/*
 */
@WebServlet(name = "AdminServlet", urlPatterns = {
        "/api/admin/stats",
        "/api/admin/users",
        "/api/admin/donors",
        "/api/admin/requests",
        "/api/admin/request/status",
        "/api/admin/user/status"
})
public class AdminServlet extends HttpServlet {

    private final AuthService authService = new AuthServiceImpl();
    private final DonorService donorService = new DonorServiceImpl();
    private final BloodRequestService requestService = new BloodRequestServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        try {
            switch (path) {
                case "/api/admin/stats": {
                    Map<String, Object> stats = requestService.getAdminStatistics();
                    ApiResponse.sendSuccess(resp, "Admin statistics retrieved.", stats);
                    break;
                }

                case "/api/admin/users": {
                    List<User> users = authService.getAllUsers();
                    ApiResponse.sendSuccess(resp, "All users retrieved.", users);
                    break;
                }

                case "/api/admin/donors": {
                    List<Donor> donors = donorService.getAllDonors();
                    ApiResponse.sendSuccess(resp, "All donors retrieved.", donors);
                    break;
                }

                case "/api/admin/requests": {
                    String status = req.getParameter("status");
                    String bloodGroup = req.getParameter("bloodGroup");
                    String city = req.getParameter("city");
                    List<BloodRequest> requests = requestService.getAllRequests(status, bloodGroup, city);
                    ApiResponse.sendSuccess(resp, "Requests retrieved.", requests);
                    break;
                }

                default:
                    ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
            }
        } catch (ApplicationException e) {
            ApiResponse.sendError(resp, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error executing admin query: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        try {
            JsonObject json = parseJsonBody(req);

            switch (path) {
                case "/api/admin/request/status": {
                    int requestId = json.get("requestId").getAsInt();
                    String newStatus = getJsonString(json, "status", "");
                    requestService.updateRequestStatus(requestId, newStatus);
                    ApiResponse.sendSuccess(resp, "Request status updated successfully to " + newStatus, null);
                    break;
                }

                case "/api/admin/user/status": {
                    int userId = json.get("userId").getAsInt();
                    String newStatus = getJsonString(json, "status", "");
                    authService.updateAccountStatus(userId, newStatus);
                    ApiResponse.sendSuccess(resp, "User account status updated to " + newStatus, null);
                    break;
                }

                default:
                    ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
            }
        } catch (ApplicationException e) {
            ApiResponse.sendError(resp, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error executing admin action: " + e.getMessage());
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
