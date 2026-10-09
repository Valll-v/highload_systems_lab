package ru.itmo.ticketing.support;

import org.testcontainers.containers.PostgreSQLContainer;

public final class PostgresContainer {

    public static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        INSTANCE.start();
    }

    private PostgresContainer() {
    }
}
