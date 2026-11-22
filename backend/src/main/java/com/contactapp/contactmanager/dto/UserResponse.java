package com.contactapp.contactmanager.dto;

import com.contactapp.contactmanager.entity.User;
import java.time.LocalDateTime;

/** What we show about a user. Notice: NO password / hash field, so it can never leak. */
public record UserResponse(Long id, String name, String email, String phone, LocalDateTime createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getCreatedAt());
    }
}
