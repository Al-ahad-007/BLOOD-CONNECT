package com.bloodconnect.dao;

import com.bloodconnect.config.DatabaseConnection;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for User CRUD operations using JDBC.
 */
public class UserDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    /**
     * Inserts a new user into the database.
     * Uses PreparedStatement with RETURN_GENERATED_KEYS.
     */
    public int create(User user) throws DatabaseException {
        String sql = "INSERT INTO users (full_name, email, phone, password_hash, role, city, state, pincode, address, account_status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail().toLowerCase().trim());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole());
            ps.setString(6, user.getCity());
            ps.setString(7, user.getState());
            ps.setString(8, user.getPincode());
            ps.setString(9, user.getAddress());
            ps.setString(10, user.getAccountStatus() != null ? user.getAccountStatus() : "ACTIVE");

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Creating user failed, no rows affected.");
            }

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int generatedId = rs.getInt(1);
                user.setUserId(generatedId);
                return generatedId;
            } else {
                throw new DatabaseException("Creating user failed, no generated ID obtained.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database error creating user: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Finds a user by their unique primary key ID.
     */
    public User findById(int userId) throws DatabaseException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
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
            throw new DatabaseException("Error finding user by ID: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Finds a user by unique email address.
     */
    public User findByEmail(String email) throws DatabaseException {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by email: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, ps, conn);
        }
    }

    /**
     * Checks if an email is already registered.
     */
    public boolean emailExists(String email) throws DatabaseException {
        return findByEmail(email) != null;
    }

    /**
     * Updates an existing user's profile details.
     */
    public boolean update(User user) throws DatabaseException {
        String sql = "UPDATE users SET full_name = ?, phone = ?, city = ?, state = ?, pincode = ?, address = ? WHERE user_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getCity());
            ps.setString(4, user.getState());
            ps.setString(5, user.getPincode());
            ps.setString(6, user.getAddress());
            ps.setInt(7, user.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating user: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(ps, conn);
        }
    }

    /**
     * Updates password hash.
     */
    public boolean updatePassword(int userId, String newPasswordHash) throws DatabaseException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating password: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(ps, conn);
        }
    }

    /**
     * Updates account status (ACTIVE, INACTIVE, SUSPENDED).
     */
    public boolean updateAccountStatus(int userId, String status) throws DatabaseException {
        String sql = "UPDATE users SET account_status = ? WHERE user_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = db.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status.toUpperCase());
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating account status: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(ps, conn);
        }
    }

    /**
     * Retrieves all registered users (used by Admin).
     */
    public List<User> findAll() throws DatabaseException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<User> list = new ArrayList<>();
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
            throw new DatabaseException("Error retrieving all users: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, stmt, conn);
        }
    }

    /**
     * Counts total registered users.
     */
    public int countTotalUsers() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM users";
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
            throw new DatabaseException("Error counting users: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(rs, stmt, conn);
        }
    }

    /**
     * Helper to map ResultSet row to User entity.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));
        user.setCity(rs.getString("city"));
        user.setState(rs.getString("state"));
        user.setPincode(rs.getString("pincode"));
        user.setAddress(rs.getString("address"));
        user.setAccountStatus(rs.getString("account_status"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setUpdatedAt(rs.getTimestamp("updated_at"));
        return user;
    }
}
