package com.bloodconnect.dao;

import com.bloodconnect.config.DatabaseConnection;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.model.BloodRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Blood Request CRUD operations using JDBC.
 */
public class BloodRequestDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    /**
     * Creates a new blood request.
     */
    public int create(BloodRequest req) throws DatabaseException {
        String sql = "INSERT INTO blood_requests (requester_id, patient_name, blood_group, units_required, "
                   + "hospital_name, hospital_address, city, state, pincode, contact_person, contact_phone, "
                   + "urgency, required_date, description, request_status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, req.getRequesterId());
            ps.setString(2, req.getPatientName());
            ps.setString(3, req.getBloodGroup());
            ps.setInt(4, req.getUnitsRequired());
            ps.setString(5, req.getHospitalName());
            ps.setString(6, req.getHospitalAddress());
            ps.setString(7, req.getCity());
            ps.setString(8, req.getState());
            ps.setString(9, req.getPincode());
            ps.setString(10, req.getContactPerson());
            ps.setString(11, req.getContactPhone());
            ps.setString(12, req.getUrgency() != null ? req.getUrgency() : "NORMAL");
            ps.setDate(13, req.getRequiredDate());
            ps.setString(14, req.getDescription());
            ps.setString(15, req.getRequestStatus() != null ? req.getRequestStatus() : "PENDING");

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Creating blood request failed, no rows affected.");
            }

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                req.setRequestId(id);
                return id;
            } else {
                throw new DatabaseException("Creating blood request failed, no ID obtained.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database error creating blood request: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Finds blood request by ID with joined requester details and response count.
     */
    public BloodRequest findById(int requestId) throws DatabaseException {
        String sql = "SELECT r.*, u.full_name AS requester_name, u.email AS requester_email, "
                   + "(SELECT COUNT(*) FROM donor_responses dr WHERE dr.request_id = r.request_id) AS response_count "
                   + "FROM blood_requests r "
                   + "INNER JOIN users u ON r.requester_id = u.user_id "
                   + "WHERE r.request_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, requestId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding blood request by ID: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Finds all requests submitted by a specific requester.
     */
    public List<BloodRequest> findByRequesterId(int requesterId) throws DatabaseException {
        String sql = "SELECT r.*, u.full_name AS requester_name, u.email AS requester_email, "
                   + "(SELECT COUNT(*) FROM donor_responses dr WHERE dr.request_id = r.request_id) AS response_count "
                   + "FROM blood_requests r "
                   + "INNER JOIN users u ON r.requester_id = u.user_id "
                   + "WHERE r.requester_id = ? "
                   + "ORDER BY r.created_at DESC";
        List<BloodRequest> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, requesterId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding requests by requester ID: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Finds open requests matching a blood group or location (for Donor Dashboard feeds).
     */
    public List<BloodRequest> findOpenRequestsForDonor(String bloodGroup, String city) throws DatabaseException {
        StringBuilder sql = new StringBuilder(
                "SELECT r.*, u.full_name AS requester_name, u.email AS requester_email, "
                + "(SELECT COUNT(*) FROM donor_responses dr WHERE dr.request_id = r.request_id) AS response_count "
                + "FROM blood_requests r "
                + "INNER JOIN users u ON r.requester_id = u.user_id "
                + "WHERE r.request_status IN ('PENDING', 'ACCEPTED') "
        );

        List<Object> params = new ArrayList<>();
        if (bloodGroup != null && !bloodGroup.trim().isEmpty()) {
            sql.append("AND (r.blood_group = ? OR r.blood_group = 'O-') ");
            params.add(bloodGroup.trim().toUpperCase());
        }

        sql.append("ORDER BY CASE r.urgency WHEN 'CRITICAL' THEN 1 WHEN 'URGENT' THEN 2 ELSE 3 END, r.created_at DESC");

        List<BloodRequest> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding open requests for donor: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Updates request status (PENDING, ACCEPTED, FULFILLED, CANCELLED).
     */
    public boolean updateStatus(int requestId, String newStatus) throws DatabaseException {
        String sql = "UPDATE blood_requests SET request_status = ?, updated_at = CURRENT_TIMESTAMP WHERE request_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus.toUpperCase());
            ps.setInt(2, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating request status: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(ps, conn);
        }
    }

    /**
     * Cancels a request if it belongs to the specified requester and is not already fulfilled.
     */
    public boolean cancelRequest(int requestId, int requesterId) throws DatabaseException {
        String sql = "UPDATE blood_requests SET request_status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP "
                   + "WHERE request_id = ? AND requester_id = ? AND request_status != 'FULFILLED'";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, requestId);
            ps.setInt(2, requesterId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error cancelling request: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(ps, conn);
        }
    }

    /**
     * Retrieves all blood requests with optional filters (for Admin Dashboard).
     */
    public List<BloodRequest> findAll(String statusFilter, String bloodGroupFilter, String cityFilter) throws DatabaseException {
        StringBuilder sql = new StringBuilder(
                "SELECT r.*, u.full_name AS requester_name, u.email AS requester_email, "
                + "(SELECT COUNT(*) FROM donor_responses dr WHERE dr.request_id = r.request_id) AS response_count "
                + "FROM blood_requests r "
                + "INNER JOIN users u ON r.requester_id = u.user_id WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter.trim())) {
            sql.append("AND r.request_status = ? ");
            params.add(statusFilter.trim().toUpperCase());
        }
        if (bloodGroupFilter != null && !bloodGroupFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(bloodGroupFilter.trim())) {
            sql.append("AND r.blood_group = ? ");
            params.add(bloodGroupFilter.trim().toUpperCase());
        }
        if (cityFilter != null && !cityFilter.trim().isEmpty()) {
            sql.append("AND LOWER(r.city) LIKE ? ");
            params.add("%" + cityFilter.trim().toLowerCase() + "%");
        }

        sql.append("ORDER BY r.created_at DESC");

        List<BloodRequest> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving requests: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Finds recent emergency blood requests for public homepage live feed.
     */
    public List<BloodRequest> findRecentEmergencyRequests(int limit) throws DatabaseException {
        String sql = "SELECT r.*, u.full_name AS requester_name, u.email AS requester_email, "
                   + "(SELECT COUNT(*) FROM donor_responses dr WHERE dr.request_id = r.request_id) AS response_count "
                   + "FROM blood_requests r "
                   + "INNER JOIN users u ON r.requester_id = u.user_id "
                   + "WHERE r.request_status IN ('PENDING', 'ACCEPTED') "
                   + "ORDER BY CASE r.urgency WHEN 'CRITICAL' THEN 1 WHEN 'URGENT' THEN 2 ELSE 3 END, r.created_at DESC "
                   + "LIMIT ?";
        List<BloodRequest> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, limit <= 0 ? 5 : limit);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving recent emergency requests: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Counts requests by status.
     */
    public int countByStatus(String status) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM blood_requests WHERE request_status = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status.toUpperCase());
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting requests by status: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Counts total blood requests.
     */
    public int countTotal() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM blood_requests";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting total requests: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, stmt, conn);
        }
    }

    /**
     * Helper to map ResultSet row to BloodRequest entity.
     */
    private BloodRequest mapRow(ResultSet rs) throws SQLException {
        BloodRequest req = new BloodRequest();
        req.setRequestId(rs.getInt("request_id"));
        req.setRequesterId(rs.getInt("requester_id"));
        req.setPatientName(rs.getString("patient_name"));
        req.setBloodGroup(rs.getString("blood_group"));
        req.setUnitsRequired(rs.getInt("units_required"));
        req.setHospitalName(rs.getString("hospital_name"));
        req.setHospitalAddress(rs.getString("hospital_address"));
        req.setCity(rs.getString("city"));
        req.setState(rs.getString("state"));
        req.setPincode(rs.getString("pincode"));
        req.setContactPerson(rs.getString("contact_person"));
        req.setContactPhone(rs.getString("contact_phone"));
        req.setUrgency(rs.getString("urgency"));
        req.setRequiredDate(rs.getDate("required_date"));
        req.setDescription(rs.getString("description"));
        req.setRequestStatus(rs.getString("request_status"));
        req.setCreatedAt(rs.getTimestamp("created_at"));
        req.setUpdatedAt(rs.getTimestamp("updated_at"));

        try {
            req.setRequesterName(rs.getString("requester_name"));
            req.setRequesterEmail(rs.getString("requester_email"));
        } catch (SQLException ignored) {
        }

        try {
            req.setResponseCount(rs.getInt("response_count"));
        } catch (SQLException ignored) {
        }

        return req;
    }
}
