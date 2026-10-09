package ru.itmo.ticketing.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.ticketing.booking.dto.BookingResponse;
import ru.itmo.ticketing.booking.dto.CreateBookingRequest;
import ru.itmo.ticketing.common.CurrentUser;
import ru.itmo.ticketing.common.RequireRole;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRole;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService service;

    @PostMapping
    @RequireRole(UserRole.CUSTOMER)
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@CurrentUser User customer, @Valid @RequestBody CreateBookingRequest request) {
        return service.create(customer, request.eventId(), request.seatIds());
    }

    @GetMapping
    @RequireRole(UserRole.CUSTOMER)
    public List<BookingResponse> getMyBookings(@CurrentUser User customer) {
        return service.listForCustomer(customer);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.CUSTOMER, UserRole.ADMIN})
    public BookingResponse get(@CurrentUser User actor, @PathVariable Long id) {
        return service.get(actor, id);
    }

    @PostMapping("/{id}/cancel")
    @RequireRole({UserRole.CUSTOMER, UserRole.ADMIN})
    public BookingResponse cancel(@CurrentUser User actor, @PathVariable Long id) {
        return service.cancel(actor, id);
    }
}
