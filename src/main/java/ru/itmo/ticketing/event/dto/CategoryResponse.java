package ru.itmo.ticketing.event.dto;

import ru.itmo.ticketing.event.Category;

public record CategoryResponse(Long id, String name) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
