package ru.itmo.ticketing.common;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String entity, Object id) {
        super(entity + " with id " + id + " not found");
    }

    public NotFoundException(String message) {
        super(message);
    }
}
