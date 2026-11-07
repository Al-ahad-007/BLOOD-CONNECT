package com.bloodconnect.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Utility to format and stream standard JSON responses to HTTP clients.
 */
public class ApiResponse {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .create();

    private boolean success;
    private String message;
    private Object data;
    private int statusCode;

    public ApiResponse(boolean success, String message, Object data, int statusCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.statusCode = statusCode;
    }

    public static void sendSuccess(HttpServletResponse resp, String message, Object data) throws IOException {
        send(resp, HttpServletResponse.SC_OK, true, message, data);
    }

    public static void sendCreated(HttpServletResponse resp, String message, Object data) throws IOException {
        send(resp, HttpServletResponse.SC_CREATED, true, message, data);
    }

    public static void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        send(resp, statusCode, false, message, null);
    }

    private static void send(HttpServletResponse resp, int statusCode, boolean success, String message, Object data)
            throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        ApiResponse responseObj = new ApiResponse(success, message, data, statusCode);
        try (PrintWriter out = resp.getWriter()) {
            out.print(GSON.toJson(responseObj));
            out.flush();
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
