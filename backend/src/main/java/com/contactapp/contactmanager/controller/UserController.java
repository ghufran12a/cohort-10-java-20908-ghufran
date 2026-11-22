package com.contactapp.contactmanager.controller;

import com.contactapp.contactmanager.dto.ChangePasswordRequest;
import com.contactapp.contactmanager.dto.UserResponse;
import com.contactapp.contactmanager.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in user's own profile. "Who is logged in" comes from the token, never from the request. */
@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    // @AuthenticationPrincipal Jwt = the already-verified token of the current request
    @GetMapping
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.getProfile(Long.valueOf(jwt.getSubject()));
    }

    // PUT /api/users/me/password -> 204
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(Long.valueOf(jwt.getSubject()), request);
        return ResponseEntity.noContent().build();
    }
}
