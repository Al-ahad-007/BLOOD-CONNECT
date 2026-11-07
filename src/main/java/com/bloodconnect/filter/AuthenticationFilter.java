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
 * Filter to protect private pages and endpoints from unauthenticated access.
 */
@WebFilter(filterName = "AuthenticationFilter", urlPatterns = {
        "/donor-dashboard.html",
        "/my-requests.html",
        "/request-blood.html",
        "/admin-dashboard.html",
        "/api/donor/*",
        "/api/requests/create",
        "/api/requests/my",
        "/api/requests/cancel",
        "/api/user/*",
        "/api/admin/*"
})
public class AuthenticationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("user") != null);

        if (isLoggedIn) {
            chain.doFilter(request, response);
            return;
        }

        String requestUri = req.getRequestURI();
        if (requestUri.contains("/api/")) {
            ApiResponse.sendError(res, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: Please log in to access this feature.");
        } else {
            String contextPath = req.getContextPath();
            res.sendRedirect(contextPath + "/login.html?error=unauthorized");
        }
    }

    @Override
    public void destroy() {
    }
}
