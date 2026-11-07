package com.bloodconnect.dao;

import com.bloodconnect.config.DatabaseConnection;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.model.Donor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Donor operations using JDBC.
 */
public class DonorDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    /**
     * Inserts donor record associated with an existing user.
     */
    public int create(Donor donor) throws DatabaseException {
        String sql = "INSERT INTO donors (user_id, blood_group, availability_status, is_discoverable) "
                   + "VALUES (?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, donor.getUserId());
            ps.setString(2, donor.getBloodGroup());
            ps.setString(3, donor.getAvailabilityStatus() != null ? donor.getAvailabilityStatus() : "AVAILABLE");
            ps.setBoolean(4, donor.isDiscoverable());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Creating donor record failed, no rows affected.");
            }

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int generatedId = rs.getInt(1);
                donor.setDonorId(generatedId);
                return generatedId;
            } else {
                throw new DatabaseException("Creating donor record failed, no generated ID.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database error creating donor record: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Finds donor by donor primary key ID.
     */
    public Donor findById(int donorId) throws DatabaseException {
        String sql = "SELECT d.*, u.full_name, u.email, u.phone, u.city, u.state, u.pincode, u.address, u.account_status, u.created_at, u.updated_at "
                   + "FROM donors d INNER JOIN users u ON d.user_id = u.user_id "
                   + "WHERE d.donor_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, donorId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding donor by ID: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Finds donor by user foreign key ID.
     */
    public Donor findByUserId(int userId) throws DatabaseException {
        String sql = "SELECT d.*, u.full_name, u.email, u.phone, u.city, u.state, u.pincode, u.address, u.account_status, u.created_at, u.updated_at "
                   + "FROM donors d INNER JOIN users u ON d.user_id = u.user_id "
                   + "WHERE d.user_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding donor by user ID: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Updates donor availability status (AVAILABLE / UNAVAILABLE).
     */
    public boolean updateAvailability(int donorId, String status) throws DatabaseException {
        String sql = "UPDATE donors SET availability_status = ?, last_updated = CURRENT_TIMESTAMP WHERE donor_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status.toUpperCase());
            ps.setInt(2, donorId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating donor availability: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(ps, conn);
        }
    }

    /**
     * Updates donor details (blood group, discoverability).
     */
    public boolean updateDonor(Donor donor) throws DatabaseException {
        String sql = "UPDATE donors SET blood_group = ?, availability_status = ?, is_discoverable = ?, last_updated = CURRENT_TIMESTAMP "
                   + "WHERE donor_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, donor.getBloodGroup());
            ps.setString(2, donor.getAvailabilityStatus());
            ps.setBoolean(3, donor.isDiscoverable());
            ps.setInt(4, donor.getDonorId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating donor record: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(ps, conn);
        }
    }

    /**
     * Search donors with dynamic filters using PreparedStatement.
     * Respects user active status and discoverability.
     */
    public List<Donor> searchDonors(String bloodGroup, String city, String state, String pincode, String availability)
            throws DatabaseException {
        StringBuilder sql = new StringBuilder(
                "SELECT d.*, u.full_name, u.email, u.phone, u.city, u.state, u.pincode, u.address, u.account_status, u.created_at, u.updated_at "
                + "FROM donors d INNER JOIN users u ON d.user_id = u.user_id "
                + "WHERE u.account_status = 'ACTIVE' AND d.is_discoverable = TRUE "
        );

        List<Object> params = new ArrayList<>();

        if (bloodGroup != null && !bloodGroup.trim().isEmpty() && !"ALL".equalsIgnoreCase(bloodGroup.trim())) {
            sql.append("AND d.blood_group = ? ");
            params.add(bloodGroup.trim().toUpperCase());
        }
        if (city != null && !city.trim().isEmpty()) {
            sql.append("AND LOWER(u.city) LIKE ? ");
            params.add("%" + city.trim().toLowerCase() + "%");
        }
        if (state != null && !state.trim().isEmpty()) {
            sql.append("AND LOWER(u.state) LIKE ? ");
            params.add("%" + state.trim().toLowerCase() + "%");
        }
        if (pincode != null && !pincode.trim().isEmpty()) {
            sql.append("AND u.pincode = ? ");
            params.add(pincode.trim());
        }
        if (availability != null && !availability.trim().isEmpty() && !"ALL".equalsIgnoreCase(availability.trim())) {
            sql.append("AND d.availability_status = ? ");
            params.add(availability.trim().toUpperCase());
        }

        sql.append("ORDER BY d.availability_status ASC, d.last_updated DESC");

        List<Donor> list = new ArrayList<>();
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
            throw new DatabaseException("Error searching donors: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Retrieves all donors (for Admin Dashboard).
     */
    public List<Donor> findAllDonors() throws DatabaseException {
        String sql = "SELECT d.*, u.full_name, u.email, u.phone, u.city, u.state, u.pincode, u.address, u.account_status, u.created_at, u.updated_at "
                   + "FROM donors d INNER JOIN users u ON d.user_id = u.user_id "
                   + "ORDER BY d.donor_id DESC";
        List<Donor> list = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all donors: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, stmt, conn);
        }
    }

    /**
     * Counts currently available donors.
     */
    public int countAvailableDonors() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM donors d INNER JOIN users u ON d.user_id = u.user_id "
                   + "WHERE d.availability_status = 'AVAILABLE' AND u.account_status = 'ACTIVE'";
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
            throw new DatabaseException("Error counting available donors: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, stmt, conn);
        }
    }

    /**
     * Counts total registered donors.
     */
    public int countTotalDonors() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM donors";
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
            throw new DatabaseException("Error counting total donors: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, stmt, conn);
        }
    }

    /**
     * Helper to map ResultSet row to a Donor instance.
     */
    private Donor mapRow(ResultSet rs) throws SQLException {
        Donor donor = new Donor();
        donor.setDonorId(rs.getInt("donor_id"));
        donor.setUserId(rs.getInt("user_id"));
        donor.setBloodGroup(rs.getString("blood_group"));
        donor.setAvailabilityStatus(rs.getString("availability_status"));
        donor.setDiscoverable(rs.getBoolean("is_discoverable"));
        donor.setLastUpdated(rs.getTimestamp("last_updated"));

        // Inherited user properties
        donor.setFullName(rs.getString("full_name"));
        donor.setEmail(rs.getString("email"));
        donor.setPhone(rs.getString("phone"));
        donor.setCity(rs.getString("city"));
        donor.setState(rs.getString("state"));
        donor.setPincode(rs.getString("pincode"));
        donor.setAddress(rs.getString("address"));
        donor.setAccountStatus(rs.getString("account_status"));
        donor.setCreatedAt(rs.getTimestamp("created_at"));
        donor.setUpdatedAt(rs.getTimestamp("updated_at"));
        return donor;
    }
}
