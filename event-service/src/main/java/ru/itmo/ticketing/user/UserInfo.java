package ru.itmo.ticketing.user;

/**
 * Ответ user-service на GET /api/users/me.
 */
public record UserInfo(Long id, String email, String fullName, UserRole role) {
}
