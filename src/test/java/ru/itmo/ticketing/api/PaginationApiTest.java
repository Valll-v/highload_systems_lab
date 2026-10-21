package ru.itmo.ticketing.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.ticketing.support.ApiClient;
import ru.itmo.ticketing.support.IntegrationTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.itmo.ticketing.support.ApiClient.expect;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaginationApiTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private ApiClient api;
    private long admin;
    private long organizer;
    private long category;
    private long hall;

    @BeforeAll
    void setUp() throws Exception {
        api = new ApiClient(mvc, json);
        admin = api.user("admin@page.test", "ADMIN");
        organizer = api.user("org@page.test", "ORGANIZER");
        category = api.category(admin, "Paged");
        hall = api.hall(admin, api.venue(admin, "Paged Hall", "Omsk"), 3, 10);
    }

    @Test
    void sizeIsCappedAtFifty() throws Exception {
        mvc.perform(get("/api/users?size=500").header("X-User-Id", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(header().exists("X-Total-Count"));

        mvc.perform(get("/api/halls/" + hall + "/seats?size=1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void pageWithTotalCountInHeader() throws Exception {
        long before = Long.parseLong(mvc.perform(get("/api/categories?size=1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("X-Total-Count"));
        api.category(admin, "Paged extra " + before);

        mvc.perform(get("/api/categories?size=1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", String.valueOf(before + 1)))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(before + 1))
                .andExpect(jsonPath("$.totalPages").value((int) (before + 1)));
    }

    @Test
    void sliceWithoutTotalForInfiniteScroll() throws Exception {
        var first = expect(api.get(null, "/api/halls/" + hall + "/seats?page=0&size=20"), 200);
        assertThat(first.body().get("content")).hasSize(20);
        assertThat(first.body().get("hasNext").asBoolean()).isTrue();
        assertThat(first.body().has("totalElements")).isFalse();

        var second = expect(api.get(null, "/api/halls/" + hall + "/seats?page=1&size=20"), 200);
        assertThat(second.body().get("content")).hasSize(10);
        assertThat(second.body().get("hasNext").asBoolean()).isFalse();

        api.publishedEvent(organizer, admin, category, hall, "Scroll 1");
        api.publishedEvent(organizer, admin, category, hall, "Scroll 2");
        var events = expect(api.get(null, "/api/events?city=omsk&size=1"), 200);
        assertThat(events.body().get("content")).hasSize(1);
        assertThat(events.body().get("hasNext").asBoolean()).isTrue();
        assertThat(events.body().has("totalElements")).isFalse();
    }

    @Test
    void badPageParamsAreRejected() throws Exception {
        assertThat(api.get(null, "/api/events?page=-1").status()).isEqualTo(400);
        assertThat(api.get(null, "/api/events?size=0").status()).isEqualTo(400);
        assertThat(api.get(null, "/api/events?page=abc").status()).isEqualTo(400);
    }
}
