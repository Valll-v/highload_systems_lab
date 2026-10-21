package ru.itmo.ticketing.event.dto;

import ru.itmo.ticketing.event.Tag;

public record TagResponse(Long id, String name) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
