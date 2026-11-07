package com.bloodconnect.servlet;

import com.bloodconnect.util.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Servlet handling secure session termination and logout.
 * URL Patterns: /api/auth/logout, /logout
 */
@WebServlet(name = "LogoutServlet", urlPatterns = {"/api/auth/logout", "/logout"})
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        performLogout(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        performLogout(req, resp);
    }

    private void performLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.removeAttribute("user");
            session.removeAttribute("userId");
            session.removeAttribute("role");
            session.removeAttribute("donorId");
            session.invalidate();
        }

        // Clean JSESSIONID cookie
        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
        cookie.setMaxAge(0);
        resp.addCookie(cookie);

        String requestUri = req.getRequestURI();
        if (requestUri.contains("/api/")) {
            ApiResponse.sendSuccess(resp, "Successfully logged out.", null);
        } else {
            resp.sendRedirect(req.getContextPath() + "/login.html?status=logged_out");
        }
    }
}
