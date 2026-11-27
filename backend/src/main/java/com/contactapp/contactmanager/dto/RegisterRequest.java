package com.contactapp.contactmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Registration form. The user gives an email OR a phone number (at least one; the service checks that). */
public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 255, message = "Email must be at most 255 characters")
        @Pattern(regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Email format is invalid (example: name@example.com)")
        String email,

        @Pattern(regexp = "^$|^\\+?[0-9]{7,15}$",
                message = "Phone must contain only digits (7 to 15), optionally starting with +")
        String phone,

        // BCrypt rejects passwords longer than 72 BYTES in UTF-8. @Size counts characters, and some characters
        // (like accented letters) need more than one byte, so AuthService also checks the byte length
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Password must contain at least one letter and one number")
        String password) {
}
