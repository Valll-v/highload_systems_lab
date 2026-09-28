package ru.itmo.ticketing;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class SchemaMigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void migrationsCreateAllTables() {
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public' order by table_name",
                String.class);

        assertThat(tables).contains(
                "users", "categories", "venues", "halls", "seats",
                "events", "bookings", "seat_reservations", "tickets");
    }

    @Test
    void seatReservationIsUniquePerEvent() {
        Long count = jdbc.queryForObject(
                "select count(*) from pg_constraint where conname = 'uk_reservations_event_seat'", Long.class);
        assertThat(count).isEqualTo(1);
    }
}
