package com.contactapp.contactmanager.dto;

import jakarta.validation.constraints.NotBlank;

/** Login form: "identifier" is the email OR the phone number used at registration. */
public record LoginRequest(
        @NotBlank(message = "Email or phone is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password) {
}
