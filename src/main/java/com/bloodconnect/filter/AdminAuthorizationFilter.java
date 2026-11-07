package com.bloodconnect.filter;

import com.bloodconnect.model.User;
import com.bloodconnect.util.ApiResponse;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Filter to ensure only users with the ADMIN role can access administrator pages and APIs.
 */
@WebFilter(filterName = "AdminAuthorizationFilter", urlPatterns = {
        "/admin-dashboard.html",
        "/api/admin/*"
})
public class AdminAuthorizationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user != null && user.isAdmin()) {
            chain.doFilter(request, response);
            return;
        }

        String requestUri = req.getRequestURI();
        if (requestUri.contains("/api/")) {
            ApiResponse.sendError(res, HttpServletResponse.SC_FORBIDDEN,
                    "Access Denied: Administrator privileges required.");
        } else {
            String contextPath = req.getContextPath();
            res.sendRedirect(contextPath + "/login.html?error=forbidden");
        }
    }

    @Override
    public void destroy() {
    }
}
