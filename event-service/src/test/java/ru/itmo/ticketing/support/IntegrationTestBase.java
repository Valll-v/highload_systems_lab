package ru.itmo.ticketing.support;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.itmo.ticketing.booking.BookingClient;
import ru.itmo.ticketing.booking.CancelledBookings;
import ru.itmo.ticketing.user.UserClient;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = PostgresContainer.INSTANCE;

    @MockBean(reset = MockReset.NONE)
    private UserClient userClient;

    @MockBean(reset = MockReset.NONE)
    protected BookingClient bookingClient;

    @Autowired
    protected TestUsers users;

    @BeforeAll
    void userServiceAnswersFromTestUsers() {
        doAnswer(call -> users.me(call.getArgument(0))).when(userClient).me(anyLong());
        doAnswer(call -> new CancelledBookings(call.getArgument(0), 0)).when(bookingClient).cancelByEvent(anyLong(), anyLong());
    }
}
