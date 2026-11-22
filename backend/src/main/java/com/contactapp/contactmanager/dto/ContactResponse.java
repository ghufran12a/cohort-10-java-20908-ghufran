package com.contactapp.contactmanager.dto;

import com.contactapp.contactmanager.entity.Contact;
import java.time.LocalDateTime;
import java.util.List;

/**
 * What the backend SENDS BACK. We never return the Contact entity directly because:
 *  1) it holds a User (with password hash) and would leak it,
 *  2) Contact -> Email -> Contact ... would loop forever when turned into JSON.
 */
public record ContactResponse(
        Long id,
        String firstName,
        String lastName,
        String title,
        LocalDateTime createdAt,
        LocalDateTime deletedAt,
        List<EmailDto> emails,
        List<PhoneDto> phones) {

    public static ContactResponse from(Contact c) {
        return new ContactResponse(
                c.getId(),
                c.getFirstName(),
                c.getLastName(),
                c.getTitle(),
                c.getCreatedAt(),
                c.getDeletedAt(),
                c.getEmails().stream().map(EmailDto::from).toList(),
                c.getPhones().stream().map(PhoneDto::from).toList());
    }
}
