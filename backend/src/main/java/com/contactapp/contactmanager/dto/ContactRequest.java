package com.contactapp.contactmanager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * What the frontend SENDS when creating or updating a contact.
 * Note there is no id, user or createdAt here: the client must not control those.
 */
public record ContactRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,

        // emails are optional; @Valid makes Spring validate every item in the list too
        @Valid List<EmailDto> emails,

        @NotEmpty(message = "At least one phone number is required")
        @Valid List<PhoneDto> phones) {
}
