package com.contactapp.contactmanager.dto;

/** Returned after register/login: the token the frontend must send with every later request. */
public record AuthResponse(String token, String tokenType, long expiresInMinutes, UserResponse user) {
}
