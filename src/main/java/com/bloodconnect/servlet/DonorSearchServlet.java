package com.bloodconnect.servlet;

import com.bloodconnect.exception.ApplicationException;
import com.bloodconnect.model.Donor;
import com.bloodconnect.model.User;
import com.bloodconnect.service.DonorService;
import com.bloodconnect.service.impl.DonorServiceImpl;
import com.bloodconnect.util.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet handling donor searches and queries with privacy controls.
 * URL Pattern: /api/donors/search
 */
@WebServlet(name = "DonorSearchServlet", urlPatterns = {"/api/donors/search"})
public class DonorSearchServlet extends HttpServlet {

    private final DonorService donorService = new DonorServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String bloodGroup = req.getParameter("bloodGroup");
        String city = req.getParameter("city");
        String state = req.getParameter("state");
        String pincode = req.getParameter("pincode");
        String availability = req.getParameter("availability");

        try {
            List<Donor> donors = donorService.searchDonors(bloodGroup, city, state, pincode, availability);

            // Check if current requester is logged in to manage privacy-controlled fields
            HttpSession session = req.getSession(false);
            boolean isAuthenticated = (session != null && session.getAttribute("user") != null);

            List<Map<String, Object>> resultList = new ArrayList<>();
            for (Donor d : donors) {
                Map<String, Object> item = new HashMap<>();
                item.put("donorId", d.getDonorId());
                item.put("fullName", d.getFullName());
                item.put("bloodGroup", d.getBloodGroup());
                item.put("city", d.getCity());
                item.put("state", d.getState());
                item.put("pincode", d.getPincode());
                item.put("availabilityStatus", d.getAvailabilityStatus());
                item.put("isAvailable", d.isAvailable());
                item.put("lastUpdated", d.getLastUpdated());

                // Privacy protection: phone and email only revealed to authenticated users
                if (isAuthenticated) {
                    item.put("phone", d.getPhone());
                    item.put("email", d.getEmail());
                    item.put("canContact", true);
                } else {
                    item.put("phone", maskPhone(d.getPhone()));
                    item.put("email", maskEmail(d.getEmail()));
                    item.put("canContact", false);
                }

                resultList.add(item);
            }

            ApiResponse.sendSuccess(resp, "Donors retrieved successfully.", resultList);
        } catch (ApplicationException e) {
            ApiResponse.sendError(resp, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error searching donors: " + e.getMessage());
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "******";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***@***.com";
        String[] parts = email.split("@");
        String prefix = parts[0];
        String visible = prefix.length() > 2 ? prefix.substring(0, 2) : prefix.substring(0, 1);
        return visible + "***@" + parts[1];
    }
}
