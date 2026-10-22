package ru.itmo.ticketing.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.itmo.ticketing.booking.client.UserInfo;
import ru.itmo.ticketing.booking.client.UserRole;
import ru.itmo.ticketing.booking.common.Blocking;
import ru.itmo.ticketing.booking.common.CurrentUser;
import ru.itmo.ticketing.booking.common.PageResponse;
import ru.itmo.ticketing.booking.common.Paging;
import ru.itmo.ticketing.booking.dto.BookingResponse;
import ru.itmo.ticketing.booking.dto.CreateBookingRequest;
import ru.itmo.ticketing.booking.dto.TicketResponse;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BookingController {

    private final BookingService service;

    @PostMapping("/bookings")
    public Mono<ResponseEntity<BookingResponse>> create(@CurrentUser(UserRole.CUSTOMER) UserInfo customer,
                                                        @Valid @RequestBody CreateBookingRequest request,
                                                        ServerHttpRequest http) {
        return Blocking.call(() -> service.create(customer, request.eventId(), request.seatIds()))
                .map(booking -> {
                    URI location = UriComponentsBuilder.fromUri(http.getURI()).path("/{id}").build(booking.id());
                    return ResponseEntity.created(location).body(booking);
                });
    }

    @GetMapping("/bookings")
    public Mono<ResponseEntity<PageResponse<BookingResponse>>> myBookings(@CurrentUser(UserRole.CUSTOMER) UserInfo customer,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        var pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return Blocking.call(() -> service.listForCustomer(customer, pageable)).map(Paging::withTotal);
    }

    @GetMapping("/bookings/{id}")
    public Mono<BookingResponse> get(@CurrentUser({UserRole.CUSTOMER, UserRole.ADMIN}) UserInfo actor,
                                     @PathVariable Long id) {
        return Blocking.call(() -> service.get(actor, id));
    }

    @PostMapping("/bookings/{id}/cancel")
    public Mono<BookingResponse> cancel(@CurrentUser({UserRole.CUSTOMER, UserRole.ADMIN}) UserInfo actor,
                                        @PathVariable Long id) {
        return Blocking.call(() -> service.cancel(actor, id));
    }

    @GetMapping("/tickets")
    public Mono<ResponseEntity<PageResponse<TicketResponse>>> myTickets(@CurrentUser(UserRole.CUSTOMER) UserInfo customer,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        var pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return Blocking.call(() -> service.ticketsForCustomer(customer, pageable)).map(Paging::withTotal);
    }
}
