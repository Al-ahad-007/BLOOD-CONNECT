package com.bloodconnect.servlet;

import com.bloodconnect.exception.ApplicationException;
import com.bloodconnect.model.Donor;
import com.bloodconnect.model.User;
import com.bloodconnect.service.AuthService;
import com.bloodconnect.service.impl.AuthServiceImpl;
import com.bloodconnect.util.ApiResponse;
import com.google.gson.Gson;
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
 * Servlet handling user and donor registrations.
 * URL Pattern: /api/auth/register
 */
@WebServlet(name = "RegisterServlet", urlPatterns = {"/api/auth/register"})
public class RegisterServlet extends HttpServlet {

    private final AuthService authService = new AuthServiceImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            JsonObject json = parseJsonBody(req);

            String role = getJsonString(json, "role", "REQUESTER").toUpperCase();
            String fullName = getJsonString(json, "fullName", "");
            String email = getJsonString(json, "email", "");
            String phone = getJsonString(json, "phone", "");
            String password = getJsonString(json, "password", "");
            String confirmPassword = getJsonString(json, "confirmPassword", "");
            String city = getJsonString(json, "city", "");
            String state = getJsonString(json, "state", "");
            String pincode = getJsonString(json, "pincode", "");
            String address = getJsonString(json, "address", "");

            User createdUser;
            String redirectUrl;

            if ("DONOR".equalsIgnoreCase(role)) {
                String bloodGroup = getJsonString(json, "bloodGroup", "");
                String availability = getJsonString(json, "availabilityStatus", "AVAILABLE");
                boolean discoverable = json.has("isDiscoverable") ? json.get("isDiscoverable").getAsBoolean() : true;

                Donor donor = new Donor();
                donor.setFullName(fullName);
                donor.setEmail(email);
                donor.setPhone(phone);
                donor.setRole("DONOR");
                donor.setCity(city);
                donor.setState(state);
                donor.setPincode(pincode);
                donor.setAddress(address);
                donor.setBloodGroup(bloodGroup);
                donor.setAvailabilityStatus(availability);
                donor.setDiscoverable(discoverable);

                createdUser = authService.registerDonor(donor, password, confirmPassword);
                redirectUrl = req.getContextPath() + "/donor-dashboard.html";
            } else {
                User user = new User(fullName, email, phone, null, "REQUESTER", city, state, pincode, address);
                createdUser = authService.registerUser(user, password, confirmPassword);
                redirectUrl = req.getContextPath() + "/find-donors.html";
            }

            // Establish authenticated session
            HttpSession session = req.getSession(true);
            session.setAttribute("user", createdUser);
            session.setAttribute("userId", createdUser.getUserId());
            session.setAttribute("role", createdUser.getRole());
            if (createdUser instanceof Donor) {
                session.setAttribute("donorId", ((Donor) createdUser).getDonorId());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("userId", createdUser.getUserId());
            data.put("fullName", createdUser.getFullName());
            data.put("email", createdUser.getEmail());
            data.put("role", createdUser.getRole());
            data.put("redirectUrl", redirectUrl);

            ApiResponse.sendCreated(resp, "Registration successful!", data);
        } catch (ApplicationException e) {
            ApiResponse.sendError(resp, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred during registration: " + e.getMessage());
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
