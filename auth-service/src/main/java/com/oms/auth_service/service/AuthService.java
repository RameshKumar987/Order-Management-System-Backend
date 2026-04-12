package com.oms.auth_service.service;


import com.oms.auth_service.dto.*;
import com.oms.auth_service.entity.Role;
import com.oms.auth_service.entity.RoleName;
import com.oms.auth_service.entity.User;
import com.oms.auth_service.repository.RoleRepository;
import com.oms.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2TokenService oauth2TokenService;
    private final RefreshTokenService refreshTokenService;
    private final OtpService otpService;

    public AuthResponse register(RegisterRequest request) {

        // duplicate email check
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        Role role = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow();

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setIsActive(true);
        user.setEmailVerified(true); // Keep backward compatibility

        userRepository.save(user);

        String accessToken = oauth2TokenService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        return new AuthResponse(accessToken, role.getName().name(), refreshToken);
    }

    /**
     * Send OTP to email for registration
     */
    public void sendOtpForRegistration(SendOtpRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // Generate and send OTP
        otpService.generateAndSendOtp(request.email());
    }

    /**
     * Verify OTP and complete registration
     */
    public AuthResponse verifyOtpAndRegister(VerifyOtpRequest request) {
        // Verify the OTP
        try {
            otpService.verifyOtp(request.email(), request.otpCode());
        } catch (RuntimeException e) {
            otpService.incrementAttempt(request.email(), request.otpCode());
            throw e;
        }

        // Check if email already exists
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // Create new user
        Role role = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setIsActive(true);
        user.setEmailVerified(true);

        userRepository.save(user);

        // Delete OTP after successful registration
        otpService.deleteOtp(request.email());

        // Generate tokens
        String accessToken = oauth2TokenService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        return new AuthResponse(accessToken, role.getName().name(), refreshToken);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getIsActive()) {
            throw new RuntimeException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String accessToken = oauth2TokenService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        return new AuthResponse(accessToken, user.getRole().getName().name(), refreshToken);
    }

    //Inventory User
    public AuthResponse createInventoryUser(CreateInventoryUserRequest request) {

        // duplicate email check
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        Role role = roleRepository.findByName(RoleName.INVENTORY)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setIsActive(true);

        userRepository.save(user);

        String accessToken = oauth2TokenService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        return new AuthResponse(accessToken, role.getName().name(), refreshToken);
    }

    // Get user by ID (for admin purposes)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get user by ID (for admin purposes)
    public User getUserById(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Delete user by ID (for admin purposes)
    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if(user.getRole().getName() == RoleName.ADMIN){
            throw new RuntimeException("Admin cannot delete admin account");
        }

        userRepository.delete(user);
    }

    // Get current authenticated user's details
    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Endpoint to update the current authenticated user's profile
    public void updateProfile(String email, UpdateProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(request.name());

        userRepository.save(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateAddress(String email, UpdateAddressRequest request) {
        System.out.println("Updating address for user: " + email);
        System.out.println("Request: " + request);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setAddressLine(request.addressLine());
        user.setCity(request.city());
        user.setState(request.state());
        user.setPincode(request.pincode());
        user.setLatitude(request.latitude());
        user.setLongitude(request.longitude());

        User savedUser = userRepository.save(user);
        System.out.println("Saved address for user: " + savedUser.getEmail() + " | Address: " + savedUser.getAddressLine());
    }

    // Endpoint to change the current authenticated user's password
    public void changePassword(String email, ChangePasswordRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new RuntimeException("Old password incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        userRepository.save(user);
    }

    // Method to toggle user active status (Block/Unblock)
    public User toggleUserActiveStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole().getName() == RoleName.ADMIN) {
            throw new RuntimeException("Admin account status cannot be toggled");
        }

        user.setIsActive(!user.getIsActive());
        return userRepository.save(user);
    }

    // Method to delete own account
    public void deleteOwnAccount(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole().getName() == RoleName.ADMIN) {
            throw new RuntimeException("Admin account cannot be deleted");
        }

        userRepository.delete(user);
    }
}
