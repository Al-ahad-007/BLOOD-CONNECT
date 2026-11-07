package com.bloodconnect.servlet;

import com.bloodconnect.exception.ApplicationException;
import com.bloodconnect.model.Donor;
import com.bloodconnect.model.User;
import com.bloodconnect.service.AuthService;
import com.bloodconnect.service.impl.AuthServiceImpl;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet handling user authentication and session status checking.
 * URL Patterns: /api/auth/login, /api/auth/me
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/api/auth/login", "/api/auth/me"})
public class LoginServlet extends HttpServlet {

    private final AuthService authService = new AuthServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getUserId());
            data.put("fullName", user.getFullName());
            data.put("email", user.getEmail());
            data.put("phone", user.getPhone());
            data.put("role", user.getRole());
            data.put("city", user.getCity());
            data.put("state", user.getState());
            data.put("pincode", user.getPincode());
            data.put("address", user.getAddress());
            if (user instanceof Donor) {
                Donor d = (Donor) user;
                data.put("donorId", d.getDonorId());
                data.put("bloodGroup", d.getBloodGroup());
                data.put("availabilityStatus", d.getAvailabilityStatus());
                data.put("isDiscoverable", d.isDiscoverable());
            }
            ApiResponse.sendSuccess(resp, "Session active", data);
        } else {
            ApiResponse.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "No active session.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            JsonObject json = parseJsonBody(req);
            String email = getJsonString(json, "email", "");
            String password = getJsonString(json, "password", "");

            User user = authService.login(email, password);

            // Invalidate any existing session and start fresh session
            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("role", user.getRole());

            String redirectUrl;
            if (user.isAdmin()) {
                redirectUrl = req.getContextPath() + "/admin-dashboard.html";
            } else if (user instanceof Donor) {
                Donor donor = (Donor) user;
                session.setAttribute("donorId", donor.getDonorId());
                redirectUrl = req.getContextPath() + "/donor-dashboard.html";
            } else {
                redirectUrl = req.getContextPath() + "/find-donors.html";
            }

            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getUserId());
            data.put("fullName", user.getFullName());
            data.put("email", user.getEmail());
            data.put("role", user.getRole());
            data.put("redirectUrl", redirectUrl);

            ApiResponse.sendSuccess(resp, "Login successful!", data);
        } catch (ApplicationException e) {
            ApiResponse.sendError(resp, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred during login: " + e.getMessage());
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
