package com.contactapp.contactmanager.dto;

import com.contactapp.contactmanager.entity.Phone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** JSON shape for one phone number. Only digits are allowed (with an optional leading +). */
public record PhoneDto(
        Long id,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$",
                message = "Phone must contain only digits (7 to 15), optionally starting with +")
        String phone,

        @NotBlank(message = "Phone label is required (work, home, personal, ...)")
        @Size(max = 30, message = "Label must be at most 30 characters")
        String label) {

    public static PhoneDto from(Phone p) {
        return new PhoneDto(p.getId(), p.getPhone(), p.getLabel());
    }
}
