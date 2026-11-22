package com.contactapp.contactmanager.service;

import com.contactapp.contactmanager.dto.AuthResponse;
import com.contactapp.contactmanager.dto.ChangePasswordRequest;
import com.contactapp.contactmanager.dto.LoginRequest;
import com.contactapp.contactmanager.dto.RegisterRequest;
import com.contactapp.contactmanager.dto.UserResponse;
import com.contactapp.contactmanager.entity.User;
import com.contactapp.contactmanager.exception.BadRequestException;
import com.contactapp.contactmanager.exception.DuplicateResourceException;
import com.contactapp.contactmanager.exception.InvalidCredentialsException;
import com.contactapp.contactmanager.exception.ResourceNotFoundException;
import com.contactapp.contactmanager.repository.UserRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration, login, profile and change-password. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = clean(request.email());
        String phone = clean(request.phone());
        if (email != null) {
            email = email.toLowerCase(); // emails are not case-sensitive in practice
        }

        // Rule: email OR phone, at least one
        if (email == null && phone == null) {
            throw new BadRequestException("Please provide an email or a phone number");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("This email is already registered");
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("This phone number is already registered");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPhone(phone);
        // Only the BCrypt HASH is stored, never the real password
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);

        log.info("New user registered: id {}", saved.getId());
        return toAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        Optional<User> found = identifier.contains("@")
                ? userRepository.findByEmail(identifier.toLowerCase())
                : userRepository.findByPhone(identifier);

        // SAME vague error for "no such user" and "wrong password": otherwise an attacker could use the
        // difference to find out which emails/phones are registered.
        User user = found
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        log.info("User {} logged in", user.getId());
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        // a wrong CURRENT password is a 400 (not 401), so the frontend does not log the user out
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        log.info("User {} changed their password", userId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer",
                jwtService.getExpirationMinutes(), UserResponse.from(user));
    }

    // "" or "   " -> null, otherwise trimmed
    private static String clean(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
