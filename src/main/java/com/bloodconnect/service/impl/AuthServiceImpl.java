package com.bloodconnect.service.impl;

import com.bloodconnect.dao.DonorDAO;
import com.bloodconnect.dao.UserDAO;
import com.bloodconnect.exception.ApplicationException;
import com.bloodconnect.exception.AuthenticationException;
import com.bloodconnect.exception.ValidationException;
import com.bloodconnect.model.Donor;
import com.bloodconnect.model.User;
import com.bloodconnect.service.AuthService;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Implementation of AuthService.
 * Handles server-side validation, BCrypt password hashing, and role-based policies.
 */
public class AuthServiceImpl implements AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final DonorDAO donorDAO = new DonorDAO();

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$");

    @Override
    public User registerUser(User user, String rawPassword, String confirmPassword) throws ApplicationException {
        validateCommonRegistration(user, rawPassword, confirmPassword);

        if (userDAO.emailExists(user.getEmail())) {
            throw new ValidationException("An account with this email address already exists.");
        }

        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));
        user.setPasswordHash(hashedPassword);
        user.setAccountStatus("ACTIVE");

        int userId = userDAO.create(user);
        user.setUserId(userId);
        return user;
    }

    @Override
    public Donor registerDonor(Donor donor, String rawPassword, String confirmPassword) throws ApplicationException {
        validateCommonRegistration(donor, rawPassword, confirmPassword);

        if (donor.getBloodGroup() == null || donor.getBloodGroup().trim().isEmpty()) {
            throw new ValidationException("Blood group is required for donor registration.");
        }
        validateBloodGroup(donor.getBloodGroup());

        if (userDAO.emailExists(donor.getEmail())) {
            throw new ValidationException("An account with this email address already exists.");
        }

        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));
        donor.setPasswordHash(hashedPassword);
        donor.setRole("DONOR");
        donor.setAccountStatus("ACTIVE");

        // 1. Create base user record
        int userId = userDAO.create(donor);
        donor.setUserId(userId);

        // 2. Create associated donor record
        int donorId = donorDAO.create(donor);
        donor.setDonorId(donorId);

        return donor;
    }

    @Override
    public User login(String email, String rawPassword) throws ApplicationException {
        if (email == null || email.trim().isEmpty() || rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new ValidationException("Email and password must not be empty.");
        }

        User user = userDAO.findByEmail(email.trim());
        if (user == null) {
            throw new AuthenticationException("Invalid email address or password.");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("Your account is currently inactive or suspended. Please contact administrator.");
        }

        boolean passwordMatches = false;
        try {
            passwordMatches = BCrypt.checkpw(rawPassword, user.getPasswordHash());
        } catch (Exception e) {
            // In case of non-BCrypt format in old legacy test data
            passwordMatches = rawPassword.equals(user.getPasswordHash());
        }

        if (!passwordMatches) {
            throw new AuthenticationException("Invalid email address or password.");
        }

        // If user is a donor, populate Donor details
        if (user.isDonor()) {
            Donor donor = donorDAO.findByUserId(user.getUserId());
            if (donor != null) {
                return donor;
            }
        }

        return user;
    }

    @Override
    public User updateProfile(User user) throws ApplicationException {
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            throw new ValidationException("Full name cannot be empty.");
        }
        if (user.getPhone() == null || !PHONE_PATTERN.matcher(user.getPhone().trim()).matches()) {
            throw new ValidationException("Please provide a valid 10-digit mobile number.");
        }
        if (user.getCity() == null || user.getCity().trim().isEmpty()) {
            throw new ValidationException("City cannot be empty.");
        }

        userDAO.update(user);
        return userDAO.findById(user.getUserId());
    }

    @Override
    public boolean changePassword(int userId, String oldPassword, String newPassword, String confirmPassword)
            throws ApplicationException {
        if (newPassword == null || newPassword.length() < 6) {
            throw new ValidationException("New password must be at least 6 characters long.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("New password and confirmation password do not match.");
        }

        User user = userDAO.findById(userId);
        if (user == null) {
            throw new AuthenticationException("User not found.");
        }

        boolean oldMatches = false;
        try {
            oldMatches = BCrypt.checkpw(oldPassword, user.getPasswordHash());
        } catch (Exception e) {
            oldMatches = oldPassword.equals(user.getPasswordHash());
        }

        if (!oldMatches) {
            throw new ValidationException("Current password is incorrect.");
        }

        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));
        return userDAO.updatePassword(userId, newHash);
    }

    @Override
    public User getUserById(int userId) throws ApplicationException {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new ApplicationException("User not found.", 404);
        }
        if (user.isDonor()) {
            Donor donor = donorDAO.findByUserId(userId);
            if (donor != null) {
                return donor;
            }
        }
        return user;
    }

    @Override
    public List<User> getAllUsers() throws ApplicationException {
        return userDAO.findAll();
    }

    @Override
    public boolean updateAccountStatus(int userId, String status) throws ApplicationException {
        if (!"ACTIVE".equalsIgnoreCase(status) && !"INACTIVE".equalsIgnoreCase(status) && !"SUSPENDED".equalsIgnoreCase(status)) {
            throw new ValidationException("Invalid account status. Must be ACTIVE, INACTIVE, or SUSPENDED.");
        }
        return userDAO.updateAccountStatus(userId, status);
    }

    private void validateCommonRegistration(User user, String rawPassword, String confirmPassword)
            throws ValidationException {
        if (user.getFullName() == null || user.getFullName().trim().length() < 2) {
            throw new ValidationException("Full name must be at least 2 characters.");
        }
        if (user.getEmail() == null || !EMAIL_PATTERN.matcher(user.getEmail().trim()).matches()) {
            throw new ValidationException("Please enter a valid email address.");
        }
        if (user.getPhone() == null || !PHONE_PATTERN.matcher(user.getPhone().trim()).matches()) {
            throw new ValidationException("Please enter a valid 10-digit mobile number.");
        }
        if (user.getCity() == null || user.getCity().trim().isEmpty()) {
            throw new ValidationException("City cannot be empty.");
        }
        if (user.getState() == null || user.getState().trim().isEmpty()) {
            throw new ValidationException("State cannot be empty.");
        }
        if (user.getPincode() == null || user.getPincode().trim().isEmpty()) {
            throw new ValidationException("PIN code cannot be empty.");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters long.");
        }
        if (!rawPassword.equals(confirmPassword)) {
            throw new ValidationException("Password and confirmation password do not match.");
        }
    }

    private void validateBloodGroup(String bg) throws ValidationException {
        String clean = bg.trim().toUpperCase();
        switch (clean) {
            case "A+":
            case "A-":
            case "B+":
            case "B-":
            case "AB+":
            case "AB-":
            case "O+":
            case "O-":
                return;
            default:
                throw new ValidationException("Invalid blood group selected: " + bg);
        }
    }
}
