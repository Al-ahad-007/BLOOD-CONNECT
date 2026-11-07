package com.bloodconnect.servlet;

import com.bloodconnect.exception.ApplicationException;
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

/**
 * Servlet handling user profile updates and password changes.
 * URL Patterns: /api/user/profile, /api/user/password
 */
@WebServlet(name = "UserServlet", urlPatterns = {"/api/user/profile", "/api/user/password"})
public class UserServlet extends HttpServlet {

    private final AuthService authService = new AuthServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in.");
            return;
        }

        try {
            User sessionUser = (User) session.getAttribute("user");
            User fresh = authService.getUserById(sessionUser.getUserId());
            session.setAttribute("user", fresh);
            ApiResponse.sendSuccess(resp, "Profile retrieved", fresh);
        } catch (ApplicationException e) {
            ApiResponse.sendError(resp, e.getStatusCode(), e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in.");
            return;
        }

        User sessionUser = (User) session.getAttribute("user");
        String path = req.getServletPath();

        try {
            JsonObject json = parseJsonBody(req);

            if ("/api/user/password".equals(path)) {
                String oldPass = getJsonString(json, "oldPassword", "");
                String newPass = getJsonString(json, "newPassword", "");
                String confirmPass = getJsonString(json, "confirmPassword", "");

                authService.changePassword(sessionUser.getUserId(), oldPass, newPass, confirmPass);
                ApiResponse.sendSuccess(resp, "Password updated successfully!", null);
            } else {
                // Update profile
                sessionUser.setFullName(getJsonString(json, "fullName", sessionUser.getFullName()));
                sessionUser.setPhone(getJsonString(json, "phone", sessionUser.getPhone()));
                sessionUser.setCity(getJsonString(json, "city", sessionUser.getCity()));
                sessionUser.setState(getJsonString(json, "state", sessionUser.getState()));
                sessionUser.setPincode(getJsonString(json, "pincode", sessionUser.getPincode()));
                sessionUser.setAddress(getJsonString(json, "address", sessionUser.getAddress()));

                User updated = authService.updateProfile(sessionUser);
                session.setAttribute("user", updated);
                ApiResponse.sendSuccess(resp, "Profile updated successfully!", updated);
            }
        } catch (ApplicationException e) {
            ApiResponse.sendError(resp, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error processing profile request: " + e.getMessage());
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
