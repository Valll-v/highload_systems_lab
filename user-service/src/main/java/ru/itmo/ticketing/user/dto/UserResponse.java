package ru.itmo.ticketing.user.dto;

import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRole;

import java.time.OffsetDateTime;

public record UserResponse(Long id, String email, String fullName, UserRole role, OffsetDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getCreatedAt());
    }
}
