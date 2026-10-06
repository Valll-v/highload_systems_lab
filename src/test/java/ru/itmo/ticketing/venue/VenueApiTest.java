package ru.itmo.ticketing.venue;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRepository;
import ru.itmo.ticketing.user.UserRole;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class VenueApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private VenueRepository venues;

    private Long admin;
    private Long customer;

    @BeforeEach
    void setUp() {
        admin = users.findByEmailIgnoreCase("admin@venue.ru")
                .orElseGet(() -> users.save(new User("admin@venue.ru", "Admin", UserRole.ADMIN))).getId();
        customer = users.findByEmailIgnoreCase("customer@venue.ru")
                .orElseGet(() -> users.save(new User("customer@venue.ru", "Customer", UserRole.CUSTOMER))).getId();
    }

    @Test
    void adminCreatesVenueWithHallAndSeats() throws Exception {
        String location = mvc.perform(post("/api/venues")
                        .header("X-User-Id", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ice Palace\",\"city\":\"Saint Petersburg\",\"address\":\"Pr. Pyatiletok 1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.halls", hasSize(0)))
                .andReturn().getResponse().getContentAsString();
        long venueId = Long.parseLong(location.replaceAll(".*\"id\":(\\d+).*", "$1"));

        String hall = mvc.perform(post("/api/venues/" + venueId + "/halls")
                        .header("X-User-Id", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Main\",\"rows\":5,\"seatsPerRow\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.capacity").value(50))
                .andReturn().getResponse().getContentAsString();
        long hallId = Long.parseLong(hall.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mvc.perform(get("/api/halls/" + hallId + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(50)))
                .andExpect(jsonPath("$[0].row").value(1))
                .andExpect(jsonPath("$[0].number").value(1))
                .andExpect(jsonPath("$[49].row").value(5))
                .andExpect(jsonPath("$[49].number").value(10));

        mvc.perform(get("/api/venues/" + venueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.halls", hasSize(1)));
    }

    @Test
    void customerCannotCreateVenue() throws Exception {
        mvc.perform(post("/api/venues")
                        .header("X-User-Id", customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"city\":\"Y\",\"address\":\"Z\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownVenueIsNotFound() throws Exception {
        mvc.perform(get("/api/venues/424242")).andExpect(status().isNotFound());
        mvc.perform(get("/api/halls/424242/seats")).andExpect(status().isNotFound());
    }

    @Test
    void hallValidation() throws Exception {
        Venue venue = venues.save(new Venue("V", "C", "A"));
        mvc.perform(post("/api/venues/" + venue.getId() + "/halls")
                        .header("X-User-Id", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"rows\":0,\"seatsPerRow\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)));
    }
}
