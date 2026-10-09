package ru.itmo.ticketing.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.ticketing.booking.dto.BookingResponse;
import ru.itmo.ticketing.booking.dto.CreateBookingRequest;
import ru.itmo.ticketing.common.CurrentUser;
import ru.itmo.ticketing.common.RequireRole;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRole;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService service;

    @PostMapping
    @RequireRole(UserRole.CUSTOMER)
    public ResponseEntity<BookingResponse> create(@CurrentUser User customer, @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse booking = service.create(customer, request.eventId(), request.seatIds());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(booking.id()).toUri();
        return ResponseEntity.created(location).body(booking);
    }

    @GetMapping
    @RequireRole(UserRole.CUSTOMER)
    public ResponseEntity<List<BookingResponse>> getMyBookings(@CurrentUser User customer) {
        return ResponseEntity.ok(service.listForCustomer(customer));
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.CUSTOMER, UserRole.ADMIN})
    public ResponseEntity<BookingResponse> get(@CurrentUser User actor, @PathVariable Long id) {
        return ResponseEntity.ok(service.get(actor, id));
    }

    @PostMapping("/{id}/cancel")
    @RequireRole({UserRole.CUSTOMER, UserRole.ADMIN})
    public ResponseEntity<BookingResponse> cancel(@CurrentUser User actor, @PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(actor, id));
    }
}
