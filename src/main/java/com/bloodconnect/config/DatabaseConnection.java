package com.bloodconnect.config;

import com.bloodconnect.exception.DatabaseException;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database connection manager using JDBC, Singleton pattern,
 * and external configuration management.
 */
public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static DatabaseConnection instance;

    private String driver;
    private String url;
    private String user;
    private String password;
    private boolean fallbackH2 = true;
    private boolean isH2Active = false;

    private DatabaseConnection() {
        loadConfiguration();
        initializeDriver();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    private void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                LOGGER.warning("application.properties not found on classpath, using defaults.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading application.properties", e);
        }

        // Support environment variables with fallback to properties file, then hardcoded sane defaults
        this.driver = getEnvOrProp("DB_DRIVER", props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
        this.url = getEnvOrProp("DB_URL", props.getProperty("db.url",
                "jdbc:mysql://localhost:3306/bloodconnect_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"));
        this.user = getEnvOrProp("DB_USER", props.getProperty("db.user", "root"));
        this.password = getEnvOrProp("DB_PASSWORD", props.getProperty("db.password", "root"));
        this.fallbackH2 = Boolean.parseBoolean(props.getProperty("db.fallback.h2", "true"));
    }

    private String getEnvOrProp(String envKey, String defaultValue) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal.trim();
        }
        return defaultValue;
    }

    private void initializeDriver() {
        try {
            Class.forName(driver);
            LOGGER.info("Primary JDBC Driver loaded: " + driver);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Could not load primary JDBC Driver: " + driver, e);
        }
    }

    /**
     * Obtains a JDBC Connection.
     * Tries configured primary database (MySQL) first.
     * If MySQL fails and fallbackH2 is enabled, connects to embedded H2 database.
     */
    public Connection getConnection() throws DatabaseException {
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            isH2Active = false;
            return conn;
        } catch (SQLException primaryEx) {
            if (fallbackH2) {
                LOGGER.warning("Could not connect to primary database at " + url + " (" + primaryEx.getMessage()
                        + "). Activating seamless H2 fallback for demo compatibility...");
                return getH2FallbackConnection();
            }
            throw new DatabaseException("Failed to connect to database: " + primaryEx.getMessage(), primaryEx);
        }
    }

    private synchronized Connection getH2FallbackConnection() throws DatabaseException {
        try {
            Class.forName("org.h2.Driver");
            String h2Url = "jdbc:h2:mem:bloodconnect_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
            Connection conn = DriverManager.getConnection(h2Url, "sa", "");
            if (!isH2Active) {
                initializeH2Schema(conn);
                isH2Active = true;
            }
            return conn;
        } catch (Exception e) {
            throw new DatabaseException("Failed to establish H2 fallback database connection", e);
        }
    }

    private void initializeH2Schema(Connection conn) {
        LOGGER.info("Initializing in-memory schema and seed data in H2 fallback...");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "user_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "full_name VARCHAR(100) NOT NULL, "
                    + "email VARCHAR(150) NOT NULL UNIQUE, "
                    + "phone VARCHAR(20) NOT NULL, "
                    + "password_hash VARCHAR(255) NOT NULL, "
                    + "role VARCHAR(20) NOT NULL DEFAULT 'REQUESTER', "
                    + "city VARCHAR(100) NOT NULL, "
                    + "state VARCHAR(100) NOT NULL, "
                    + "pincode VARCHAR(20) NOT NULL, "
                    + "address TEXT, "
                    + "account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS donors ("
                    + "donor_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "user_id INT NOT NULL UNIQUE, "
                    + "blood_group VARCHAR(10) NOT NULL, "
                    + "availability_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', "
                    + "is_discoverable BOOLEAN NOT NULL DEFAULT TRUE, "
                    + "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "CONSTRAINT fk_donor_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS blood_requests ("
                    + "request_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "requester_id INT NOT NULL, "
                    + "patient_name VARCHAR(100) NOT NULL, "
                    + "blood_group VARCHAR(10) NOT NULL, "
                    + "units_required INT NOT NULL DEFAULT 1, "
                    + "hospital_name VARCHAR(150) NOT NULL, "
                    + "hospital_address TEXT NOT NULL, "
                    + "city VARCHAR(100) NOT NULL, "
                    + "state VARCHAR(100) NOT NULL, "
                    + "pincode VARCHAR(20) NOT NULL, "
                    + "contact_person VARCHAR(100) NOT NULL, "
                    + "contact_phone VARCHAR(20) NOT NULL, "
                    + "urgency VARCHAR(20) NOT NULL DEFAULT 'NORMAL', "
                    + "required_date DATE NOT NULL, "
                    + "description TEXT, "
                    + "request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "CONSTRAINT fk_request_user FOREIGN KEY (requester_id) REFERENCES users(user_id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS donor_responses ("
                    + "response_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "request_id INT NOT NULL, "
                    + "donor_id INT NOT NULL, "
                    + "response_status VARCHAR(20) NOT NULL, "
                    + "response_message TEXT, "
                    + "responded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "CONSTRAINT fk_response_request FOREIGN KEY (request_id) REFERENCES blood_requests(request_id) ON DELETE CASCADE, "
                    + "CONSTRAINT fk_response_donor FOREIGN KEY (donor_id) REFERENCES donors(donor_id) ON DELETE CASCADE, "
                    + "CONSTRAINT uq_request_donor UNIQUE (request_id, donor_id))");

            // Seed Admin
            stmt.execute("MERGE INTO users (user_id, full_name, email, phone, password_hash, role, city, state, pincode, address, account_status) "
                    + "KEY(email) VALUES (1, 'System Administrator', 'admin@bloodconnect.org', '9876543210', "
                    + "'$2a$10$CdaRoius1I3wi862fgi/XOqkHXx28Gm2.4EbE1Le5UXTMtluPg8/q', 'ADMIN', 'Mumbai', 'Maharashtra', '400001', 'HQ Nariman Point', 'ACTIVE')");

            // Seed Donors
            stmt.execute("MERGE INTO users (user_id, full_name, email, phone, password_hash, role, city, state, pincode, address, account_status) "
                    + "KEY(email) VALUES (2, 'Rohit Sharma', 'rohit.sharma@example.com', '9820011223', '$2a$10$hIb1pOM1heRUNZvcGyujB.YcQjBJ9aQ66CL4ISWJcQt1DUk7CZ/pi', 'DONOR', 'Mumbai', 'Maharashtra', '400050', 'Bandra West', 'ACTIVE')");
            stmt.execute("MERGE INTO users (user_id, full_name, email, phone, password_hash, role, city, state, pincode, address, account_status) "
                    + "KEY(email) VALUES (3, 'Priya Patel', 'priya.patel@example.com', '9820022334', '$2a$10$hIb1pOM1heRUNZvcGyujB.YcQjBJ9aQ66CL4ISWJcQt1DUk7CZ/pi', 'DONOR', 'Ahmedabad', 'Gujarat', '380015', 'Satellite Road', 'ACTIVE')");
            stmt.execute("MERGE INTO users (user_id, full_name, email, phone, password_hash, role, city, state, pincode, address, account_status) "
                    + "KEY(email) VALUES (4, 'Arjun Verma', 'arjun.verma@example.com', '9820033445', '$2a$10$hIb1pOM1heRUNZvcGyujB.YcQjBJ9aQ66CL4ISWJcQt1DUk7CZ/pi', 'DONOR', 'Bengaluru', 'Karnataka', '560038', 'Indiranagar', 'ACTIVE')");
            stmt.execute("MERGE INTO users (user_id, full_name, email, phone, password_hash, role, city, state, pincode, address, account_status) "
                    + "KEY(email) VALUES (5, 'Sneha Reddy', 'sneha.reddy@example.com', '9820044556', '$2a$10$hIb1pOM1heRUNZvcGyujB.YcQjBJ9aQ66CL4ISWJcQt1DUk7CZ/pi', 'DONOR', 'Hyderabad', 'Telangana', '500081', 'HITEC City', 'ACTIVE')");
            stmt.execute("MERGE INTO users (user_id, full_name, email, phone, password_hash, role, city, state, pincode, address, account_status) "
                    + "KEY(email) VALUES (6, 'Vikram Singh', 'vikram.singh@example.com', '9820055667', '$2a$10$hIb1pOM1heRUNZvcGyujB.YcQjBJ9aQ66CL4ISWJcQt1DUk7CZ/pi', 'DONOR', 'Delhi', 'Delhi', '110001', 'CP', 'ACTIVE')");

            // Seed Requester
            stmt.execute("MERGE INTO users (user_id, full_name, email, phone, password_hash, role, city, state, pincode, address, account_status) "
                    + "KEY(email) VALUES (8, 'Rahul Mehta', 'rahul.mehta@example.com', '9811122233', '$2a$10$9rejbsoiMDS9DBmRuGLMVeGixYkreblz1ncLy.duAauodWZNH.nZm', 'REQUESTER', 'Pune', 'Maharashtra', '411004', 'Deccan', 'ACTIVE')");

            // Seed Donors mapping
            stmt.execute("MERGE INTO donors (donor_id, user_id, blood_group, availability_status, is_discoverable) KEY(user_id) VALUES (1, 2, 'O+', 'AVAILABLE', TRUE)");
            stmt.execute("MERGE INTO donors (donor_id, user_id, blood_group, availability_status, is_discoverable) KEY(user_id) VALUES (2, 3, 'A+', 'AVAILABLE', TRUE)");
            stmt.execute("MERGE INTO donors (donor_id, user_id, blood_group, availability_status, is_discoverable) KEY(user_id) VALUES (3, 4, 'B+', 'AVAILABLE', TRUE)");
            stmt.execute("MERGE INTO donors (donor_id, user_id, blood_group, availability_status, is_discoverable) KEY(user_id) VALUES (4, 5, 'AB+', 'AVAILABLE', TRUE)");
            stmt.execute("MERGE INTO donors (donor_id, user_id, blood_group, availability_status, is_discoverable) KEY(user_id) VALUES (5, 6, 'O-', 'AVAILABLE', TRUE)");

            // Seed Blood Requests
            stmt.execute("MERGE INTO blood_requests (request_id, requester_id, patient_name, blood_group, units_required, hospital_name, hospital_address, city, state, pincode, contact_person, contact_phone, urgency, required_date, description, request_status) "
                    + "KEY(request_id) VALUES (1, 8, 'Aarav Mehta', 'O+', 2, 'Lilavati Hospital', 'Bandra West', 'Mumbai', 'Maharashtra', '400050', 'Rahul Mehta', '9811122233', 'CRITICAL', CURRENT_DATE + 1, 'Urgent cardiac surgery', 'PENDING')");
            stmt.execute("MERGE INTO blood_requests (request_id, requester_id, patient_name, blood_group, units_required, hospital_name, hospital_address, city, state, pincode, contact_person, contact_phone, urgency, required_date, description, request_status) "
                    + "KEY(request_id) VALUES (2, 8, 'Devi Nair', 'B+', 1, 'Amrita Hospital', 'Edappally', 'Kochi', 'Kerala', '682041', 'Kavita Nair', '9811133344', 'URGENT', CURRENT_DATE + 2, 'Post accident trauma', 'ACCEPTED')");

            LOGGER.info("H2 fallback database successfully initialized.");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error initializing H2 fallback schema: " + e.getMessage(), e);
        }
    }

    /**
     * Utility method to safely close JDBC resources.
     */
    public static void closeQuietly(AutoCloseable... closeables) {
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public boolean isH2Active() {
        return isH2Active;
    }
}
