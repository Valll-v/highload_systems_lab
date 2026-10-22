package ru.itmo.ticketing.booking.client;

public record UserInfo(Long id, String email, String fullName, UserRole role) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
