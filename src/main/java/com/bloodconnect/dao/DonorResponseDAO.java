package com.bloodconnect.dao;

import com.bloodconnect.config.DatabaseConnection;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.model.DonorResponse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Donor Responses to emergency requests using JDBC.
 */
public class DonorResponseDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    /**
     * Checks if a donor has already submitted a response for the given request.
     */
    public boolean exists(int requestId, int donorId) throws DatabaseException {
        String sql = "SELECT 1 FROM donor_responses WHERE request_id = ? AND donor_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, requestId);
            ps.setInt(2, donorId);
            rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new DatabaseException("Error checking existing donor response: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Inserts a new response from a donor to a blood request.
     */
    public int create(DonorResponse response) throws DatabaseException {
        String sql = "INSERT INTO donor_responses (request_id, donor_id, response_status, response_message) "
                   + "VALUES (?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, response.getRequestId());
            ps.setInt(2, response.getDonorId());
            ps.setString(3, response.getResponseStatus() != null ? response.getResponseStatus() : "ACCEPTED");
            ps.setString(4, response.getResponseMessage());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Failed to save donor response.");
            }

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                response.setResponseId(id);
                return id;
            } else {
                throw new DatabaseException("Failed to obtain donor response ID.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database error saving donor response: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Finds all responses for a particular blood request (viewable by requester or admin).
     */
    public List<DonorResponse> findByRequestId(int requestId) throws DatabaseException {
        String sql = "SELECT dr.*, u.full_name AS donor_name, u.phone AS donor_phone, u.city AS donor_city, "
                   + "d.blood_group, br.patient_name, br.hospital_name, br.request_status "
                   + "FROM donor_responses dr "
                   + "INNER JOIN donors d ON dr.donor_id = d.donor_id "
                   + "INNER JOIN users u ON d.user_id = u.user_id "
                   + "INNER JOIN blood_requests br ON dr.request_id = br.request_id "
                   + "WHERE dr.request_id = ? "
                   + "ORDER BY dr.responded_at DESC";
        List<DonorResponse> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, requestId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding responses by request ID: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Finds all responses submitted by a specific donor.
     */
    public List<DonorResponse> findByDonorId(int donorId) throws DatabaseException {
        String sql = "SELECT dr.*, u.full_name AS donor_name, u.phone AS donor_phone, u.city AS donor_city, "
                   + "d.blood_group, br.patient_name, br.hospital_name, br.request_status "
                   + "FROM donor_responses dr "
                   + "INNER JOIN donors d ON dr.donor_id = d.donor_id "
                   + "INNER JOIN users u ON d.user_id = u.user_id "
                   + "INNER JOIN blood_requests br ON dr.request_id = br.request_id "
                   + "WHERE dr.donor_id = ? "
                   + "ORDER BY dr.responded_at DESC";
        List<DonorResponse> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, donorId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding responses by donor ID: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Counts responses made by a donor matching a status (e.g., ACCEPTED).
     */
    public int countByDonorAndStatus(int donorId, String status) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM donor_responses WHERE donor_id = ? AND response_status = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, donorId);
            ps.setString(2, status.toUpperCase());
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting donor responses: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Counts completed donations for a donor (either explicitly marked COMPLETED or request marked FULFILLED).
     */
    public int countCompletedDonations(int donorId) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM donor_responses dr "
                   + "INNER JOIN blood_requests br ON dr.request_id = br.request_id "
                   + "WHERE dr.donor_id = ? AND (dr.response_status = 'COMPLETED' OR (dr.response_status = 'ACCEPTED' AND br.request_status = 'FULFILLED'))";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, donorId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting completed donations: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    private DonorResponse mapRow(ResultSet rs) throws SQLException {
        DonorResponse res = new DonorResponse();
        res.setResponseId(rs.getInt("response_id"));
        res.setRequestId(rs.getInt("request_id"));
        res.setDonorId(rs.getInt("donor_id"));
        res.setResponseStatus(rs.getString("response_status"));
        res.setResponseMessage(rs.getString("response_message"));
        res.setRespondedAt(rs.getTimestamp("responded_at"));

        try {
            res.setDonorName(rs.getString("donor_name"));
            res.setDonorPhone(rs.getString("donor_phone"));
            res.setDonorCity(rs.getString("donor_city"));
            res.setBloodGroup(rs.getString("blood_group"));
            res.setPatientName(rs.getString("patient_name"));
            res.setHospitalName(rs.getString("hospital_name"));
            res.setRequestStatus(rs.getString("request_status"));
        } catch (SQLException ignored) {
        }

        return res;
    }
}
