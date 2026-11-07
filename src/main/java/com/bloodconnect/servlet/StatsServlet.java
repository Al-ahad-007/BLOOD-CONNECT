package com.bloodconnect.servlet;

import com.bloodconnect.exception.ApplicationException;
import com.bloodconnect.service.DonorService;
import com.bloodconnect.service.impl.DonorServiceImpl;
import com.bloodconnect.util.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Public servlet providing live platform statistics for the homepage.
 * URL Pattern: /api/stats/public
 */
@WebServlet(name = "StatsServlet", urlPatterns = {"/api/stats/public"})
public class StatsServlet extends HttpServlet {

    private final DonorService donorService = new DonorServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Map<String, Object> stats = donorService.getPublicPlatformStatistics();
            ApiResponse.sendSuccess(resp, "Public platform statistics loaded.", stats);
        } catch (ApplicationException e) {
            ApiResponse.sendError(resp, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error loading statistics: " + e.getMessage());
        }
    }
}
