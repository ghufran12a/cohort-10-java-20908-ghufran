package com.contactapp.contactmanager.dto;

import com.contactapp.contactmanager.entity.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO = Data Transfer Object: the shape of the JSON we send/receive for one email.
 * A "record" is a short Java class whose fields are fixed (immutable), perfect for DTOs.
 * The annotations are the validation rules; Spring checks them before our code runs.
 */
public record EmailDto(
        Long id,

        @NotBlank(message = "Email is required")
        @Size(max = 255, message = "Email must be at most 255 characters")
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Email format is invalid (example: name@example.com)")
        String email,

        @NotBlank(message = "Email label is required (work, personal, ...)")
        @Size(max = 30, message = "Label must be at most 30 characters")
        String label) {

    public static EmailDto from(Email e) {
        return new EmailDto(e.getId(), e.getEmail(), e.getLabel());
    }
}
