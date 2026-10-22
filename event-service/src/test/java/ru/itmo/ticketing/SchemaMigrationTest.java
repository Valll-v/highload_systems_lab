package ru.itmo.ticketing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.itmo.ticketing.support.IntegrationTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaMigrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void migrationsLeaveOnlyEventTables() {
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public' order by table_name",
                String.class);

        assertThat(tables)
                .contains("categories", "venues", "halls", "seats", "events", "tags", "event_tags")
                .doesNotContain("users", "bookings", "seat_reservations", "tickets");
    }

    @Test
    void organizerIsPlainIdWithoutForeignKey() {
        Long count = jdbc.queryForObject(
                "select count(*) from pg_constraint where conname = 'fk_events_organizer'", Long.class);
        assertThat(count).isZero();
    }
}
