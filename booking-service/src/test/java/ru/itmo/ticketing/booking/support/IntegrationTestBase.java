package ru.itmo.ticketing.booking.support;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.itmo.ticketing.booking.client.EventClient;
import ru.itmo.ticketing.booking.client.UserClient;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

/**
 * Postgres в Testcontainers, Feign-клиенты заменены моками, которые отвечают из TestUsers и TestEvents.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @MockBean(reset = MockReset.NONE)
    private UserClient userClient;

    @MockBean(reset = MockReset.NONE)
    private EventClient eventClient;

    @Autowired
    protected TestUsers users;

    @Autowired
    protected TestEvents events;

    @BeforeAll
    void clientsAnswerFromTestData() {
        doAnswer(call -> users.me(call.getArgument(0))).when(userClient).me(anyLong());
        doAnswer(call -> events.event(call.getArgument(0), null, users)).when(eventClient).event(anyLong());
        doAnswer(call -> events.event(call.getArgument(0), call.getArgument(1), users)).when(eventClient).eventAs(anyLong(), anyLong());
        doAnswer(call -> events.hall(call.getArgument(0))).when(eventClient).hall(anyLong());
        doAnswer(call -> events.seats(call.getArgument(0), call.getArgument(1), call.getArgument(2)))
                .when(eventClient).seats(anyLong(), anyInt(), anyInt());
    }
}
