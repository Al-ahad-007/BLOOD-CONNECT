-- ========================================================
-- BloodConnect Database Schema & Seed Data
-- Database: bloodconnect_db
-- Compatibility: MySQL 8.0+ / MariaDB 10.5+
-- ========================================================

CREATE DATABASE IF NOT EXISTS bloodconnect_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE bloodconnect_db;

-- --------------------------------------------------------
-- Table: users
-- --------------------------------------------------------
DROP TABLE IF EXISTS donor_responses;
DROP TABLE IF EXISTS blood_requests;
DROP TABLE IF EXISTS donors;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'REQUESTER',
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    address TEXT,
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_city (city),
    INDEX idx_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: donors
-- --------------------------------------------------------
CREATE TABLE donors (
    donor_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    blood_group VARCHAR(10) NOT NULL,
    availability_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    is_discoverable BOOLEAN NOT NULL DEFAULT TRUE,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_donor_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_donor_blood_group (blood_group),
    INDEX idx_donor_availability (availability_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: blood_requests
-- --------------------------------------------------------
CREATE TABLE blood_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    requester_id INT NOT NULL,
    patient_name VARCHAR(100) NOT NULL,
    blood_group VARCHAR(10) NOT NULL,
    units_required INT NOT NULL DEFAULT 1,
    hospital_name VARCHAR(150) NOT NULL,
    hospital_address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    contact_person VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    urgency VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    required_date DATE NOT NULL,
    description TEXT,
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_user FOREIGN KEY (requester_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_request_blood_group (blood_group),
    INDEX idx_request_city (city),
    INDEX idx_request_pincode (pincode),
    INDEX idx_request_status (request_status),
    INDEX idx_request_urgency (urgency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: donor_responses
-- --------------------------------------------------------
CREATE TABLE donor_responses (
    response_id INT AUTO_INCREMENT PRIMARY KEY,
    request_id INT NOT NULL,
    donor_id INT NOT NULL,
    response_status VARCHAR(20) NOT NULL,
    response_message TEXT,
    responded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_response_request FOREIGN KEY (request_id) REFERENCES blood_requests(request_id) ON DELETE CASCADE,
    CONSTRAINT fk_response_donor FOREIGN KEY (donor_id) REFERENCES donors(donor_id) ON DELETE CASCADE,
    CONSTRAINT uq_request_donor UNIQUE (request_id, donor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- Seed Demonstration Data
-- Verified BCrypt Hashes:
-- Admin:     admin@bloodconnect.org  -> Admin@123
-- Donors:    donor email             -> Donor@123
-- Requester: requester email         -> Requester@123
-- ========================================================

-- Admin User (Password: Admin@123)
INSERT INTO users (user_id, full_name, email, phone, password_hash, role, city, state, pincode, address, account_status)
VALUES (1, 'System Administrator', 'admin@bloodconnect.org', '9876543210', '$2a$10$CdaRoius1I3wi862fgi/XOqkHXx28Gm2.4EbE1Le5UXTMtluPg8/q', 'ADMIN', 'Mumbai', 'Maharashtra', '400001', 'BloodConnect Central HQ, Nariman Point', 'ACTIVE');

-- Donor Users (Password: Donor@123)
INSERT INTO users (user_id, full_name, email, phone, password_hash, role, city, state, pincode, address, account_status) VALUES
(2, 'Rohit Sharma', 'rohit.sharma@example.com', '9820011223', '$2a$10$hIb1pOM1heRUNZvcGyujB.YcQjBJ9aQ66CL4ISWJcQt1DUk7CZ/pi', 'DONOR', 'Mumbai', 'Maharashtra', '400050', 'Bandra West', 'ACTIVE'),
(3, 'Priya Patel', 'priya.patel@example.com', '9820022334', '$2a$10$hIb1pOM1heRUNZvcGyujB.YcQjBJ9aQ66CL4ISWJcQt1DUk7CZ/pi', 'DONOR', 'Ahmedabad', 'Gujarat', '380015', 'Satellite Road', 'ACTIVE'),
(4, 'Arjun Verma', 'arjun.verma@example.com', '9820033445', '$2a$10$hIb1pOM1heRUNZvcGyujB.YcQjBJ9aQ66CL4ISWJcQt1DUk7CZ/pi', 'DONOR', 'Bengaluru', 'Karnataka', '560038', 'Indiranagar 100ft Road', 'ACTIVE'),
(5, 'Sneha Reddy', 'sneha.reddy@example.com', '9820044556', '$2a$10$hIb1pOM1heRUNZvcGyujB.YcQjBJ9aQ66CL4ISWJcQt1DUk7CZ/pi', 'DONOR', 'Hyderabad', 'Telangana', '500081', 'HITEC City Madhapur', 'ACTIVE'),
(6, 'Vikram Singh', 'vikram.singh@example.com', '9820055667', '$2a$10$hIb1pOM1heRUNZvcGyujB.YcQjBJ9aQ66CL4ISWJcQt1DUk7CZ/pi', 'DONOR', 'Delhi', 'Delhi', '110001', 'Connaught Place Central', 'ACTIVE'),
(7, 'Ananya Das', 'ananya.das@example.com', '9820066778', '$2a$10$hIb1pOM1heRUNZvcGyujB.YcQjBJ9aQ66CL4ISWJcQt1DUk7CZ/pi', 'DONOR', 'Kolkata', 'West Bengal', '700029', 'Southern Avenue', 'ACTIVE');

-- Requester Users (Password: Requester@123)
INSERT INTO users (user_id, full_name, email, phone, password_hash, role, city, state, pincode, address, account_status) VALUES
(8, 'Rahul Mehta', 'rahul.mehta@example.com', '9811122233', '$2a$10$9rejbsoiMDS9DBmRuGLMVeGixYkreblz1ncLy.duAauodWZNH.nZm', 'REQUESTER', 'Pune', 'Maharashtra', '411004', 'Deccan Gymkhana', 'ACTIVE'),
(9, 'Kavita Nair', 'kavita.nair@example.com', '9811133344', '$2a$10$9rejbsoiMDS9DBmRuGLMVeGixYkreblz1ncLy.duAauodWZNH.nZm', 'REQUESTER', 'Kochi', 'Kerala', '682016', 'MG Road Ernakulam', 'ACTIVE');

-- Donors Table mapping
INSERT INTO donors (donor_id, user_id, blood_group, availability_status, is_discoverable) VALUES
(1, 2, 'O+', 'AVAILABLE', TRUE),
(2, 3, 'A+', 'AVAILABLE', TRUE),
(3, 4, 'B+', 'AVAILABLE', TRUE),
(4, 5, 'AB+', 'AVAILABLE', TRUE),
(5, 6, 'O-', 'AVAILABLE', TRUE),
(6, 7, 'A-', 'UNAVAILABLE', TRUE);

-- Blood Requests
INSERT INTO blood_requests (request_id, requester_id, patient_name, blood_group, units_required, hospital_name, hospital_address, city, state, pincode, contact_person, contact_phone, urgency, required_date, description, request_status) VALUES
(1, 8, 'Aarav Mehta', 'O+', 2, 'Lilavati Hospital & Research Centre', 'A-791, Bandra Reclamation, Bandra West', 'Mumbai', 'Maharashtra', '400050', 'Rahul Mehta', '9811122233', 'CRITICAL', DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), 'Urgent requirement for cardiac surgery scheduled tomorrow morning.', 'PENDING'),
(2, 9, 'Devi Nair', 'B+', 1, 'Amrita Institute of Medical Sciences', 'Ponekkara, Edappally', 'Kochi', 'Kerala', '682041', 'Kavita Nair', '9811133344', 'URGENT', DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), 'Post-accident trauma patient in urgent need of whole blood.', 'ACCEPTED'),
(3, 8, 'Ramesh Gupta', 'A+', 3, 'AIIMS New Delhi', 'Sri Aurobindo Marg, Ansari Nagar', 'Delhi', 'Delhi', '110029', 'Dr. Alok Verma', '9877766554', 'NORMAL', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 'Planned orthopedic surgery requirement.', 'FULFILLED');

-- Donor Responses
INSERT INTO donor_responses (response_id, request_id, donor_id, response_status, response_message) VALUES
(1, 2, 3, 'ACCEPTED', 'I am available and will reach Amrita Hospital by 10 AM tomorrow.'),
(2, 1, 1, 'ACCEPTED', 'I can donate 1 unit of O+ at Lilavati Hospital today after 5 PM.');
