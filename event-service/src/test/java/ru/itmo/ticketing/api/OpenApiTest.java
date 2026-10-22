package ru.itmo.ticketing.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.ticketing.support.IntegrationTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;

    @Test
    void apiDocsDescribeEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Ticketing API"))
                .andExpect(jsonPath("$.paths['/api/events'].post").exists())
                .andExpect(jsonPath("$.paths['/api/events'].get.parameters[?(@.name == 'size')]").exists())
                .andExpect(jsonPath("$.paths['/api/tags'].get").exists())
                .andExpect(jsonPath("$.paths['/api/events/{id}/submit'].post.parameters.length()").value(1))
                .andExpect(jsonPath("$.components.securitySchemes['X-User-Id'].in").value("header"));
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
