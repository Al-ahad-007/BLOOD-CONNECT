package com.bloodconnect.service;

import com.bloodconnect.exception.ApplicationException;
import com.bloodconnect.exception.AuthenticationException;
import com.bloodconnect.exception.ValidationException;
import com.bloodconnect.model.Donor;
import com.bloodconnect.model.User;

import java.util.List;

/**
 * Service interface for User Authentication, Registration, and Profile Management.
 * Demonstrates Abstraction in Java OOP.
 */
public interface AuthService {

    /**
     * Registers a new Requester or Admin account.
     */
    User registerUser(User user, String rawPassword, String confirmPassword) throws ApplicationException;

    /**
     * Registers a new Donor account along with donor profile.
     */
    Donor registerDonor(Donor donor, String rawPassword, String confirmPassword) throws ApplicationException;

    /**
     * Authenticates a user by email and password using BCrypt verification.
     */
    User login(String email, String rawPassword) throws ApplicationException;

    /**
     * Updates an existing user's profile details.
     */
    User updateProfile(User user) throws ApplicationException;

    /**
     * Securely changes password after verifying old password.
     */
    boolean changePassword(int userId, String oldPassword, String newPassword, String confirmPassword)
            throws ApplicationException;

    /**
     * Finds user by primary key ID.
     */
    User getUserById(int userId) throws ApplicationException;

    /**
     * Admin method to list all users.
     */
    List<User> getAllUsers() throws ApplicationException;

    /**
     * Admin method to activate or suspend account.
     */
    boolean updateAccountStatus(int userId, String status) throws ApplicationException;
}
