package ru.itmo.ticketing.user.dto;

import jakarta.validation.constraints.NotNull;
import ru.itmo.ticketing.user.UserRole;

public record UpdateRoleRequest(@NotNull UserRole role) {
}
